package com.cushty.capture;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.DrawManager;

/**
 * Captures the next rendered game frame. This is the game canvas only: never the desktop, other
 * windows or the RuneLite sidebar. The image may then be cropped to the chatbox or a snipped area.
 */
@Singleton
public class ScreenshotCapture
{
	private static final int[] CHATBOX_PARTS = {
		InterfaceID.Chatbox.CHATAREA,
		InterfaceID.Chatbox.CHAT_BACKGROUND,
		InterfaceID.Chatbox.CHATDISPLAY,
		InterfaceID.Chatbox.SCROLLAREA,
		InterfaceID.Chatbox.INPUT,
		InterfaceID.Chatbox.CONTROLS
	};

	private final DrawManager drawManager;
	private final ScheduledExecutorService executor;
	private final Client client;
	private final ClientThread clientThread;
	private final PrivateChatRedactor privateChat;

	@Inject
	ScreenshotCapture(
		DrawManager drawManager,
		ScheduledExecutorService executor,
		Client client,
		ClientThread clientThread,
		PrivateChatRedactor privateChat)
	{
		this.drawManager = drawManager;
		this.executor = executor;
		this.client = client;
		this.clientThread = clientThread;
		this.privateChat = privateChat;
	}

	/**
	 * @param snip canvas rectangle for a manual snip, or null to use {@code chatbox} / the full frame
	 * @param chatbox when there is no snip, capture only the chatbox
	 * @param hidePrivateChat rebuild chat without private messages for the captured frame
	 * @return a future completed with PNG bytes, off the client thread
	 */
	public CompletableFuture<byte[]> capture(Rectangle snip, boolean chatbox, boolean hidePrivateChat)
	{
		CompletableFuture<byte[]> result = new CompletableFuture<>();
		AtomicBoolean aborted = new AtomicBoolean();
		AtomicBoolean armed = new AtomicBoolean();
		AtomicBoolean restored = new AtomicBoolean();
		// One restore per capture. The frame listener and the timeout can both ask for it.
		Runnable restore = () -> clientThread.invokeLater(() ->
		{
			if (armed.get() && restored.compareAndSet(false, true))
			{
				privateChat.end(client);
			}
		});
		// orTimeout does not cancel the original future, so a late frame must notice the timeout.
		CompletableFuture<byte[]> timed = result.orTimeout(10, TimeUnit.SECONDS);
		timed.whenComplete((png, error) ->
		{
			aborted.set(true);
			restore.run();
		});

		clientThread.invokeAtTickEnd(() ->
		{
			if (aborted.get())
			{
				return;
			}
			Rectangle crop = snip;
			if (crop == null && chatbox)
			{
				crop = chatboxBounds();
				if (crop == null)
				{
					result.completeExceptionally(new CaptureException("The chatbox is not visible"));
					return;
				}
			}
			if (aborted.get() || result.isDone())
			{
				return;
			}

			if (hidePrivateChat)
			{
				privateChat.begin(client);
				armed.set(true);
			}
			final Rectangle cropFinal = crop == null ? null : new Rectangle(crop);
			final Dimension space = coordinateSpace();
			boolean listening = false;
			try
			{
				drawManager.requestNextFrameListener(frame -> onFrame(frame, result, aborted, cropFinal, space, restore));
				listening = true;
			}
			finally
			{
				if (!listening)
				{
					restore.run();
				}
			}
		});
		return timed;
	}

	/** Put private chat back if a capture is still holding it, for example on shutdown. */
	public void releaseRedaction()
	{
		Runnable restore = () -> privateChat.forceEnd(client);
		if (client.isClientThread())
		{
			restore.run();
		}
		else
		{
			clientThread.invoke(restore);
		}
	}

	private void onFrame(
		Image frame,
		CompletableFuture<byte[]> result,
		AtomicBoolean aborted,
		Rectangle crop,
		Dimension space,
		Runnable restore)
	{
		try
		{
			if (aborted.get() || result.isDone())
			{
				return;
			}
			// The frame buffer may be reused by the renderer, so copy it before leaving this callback.
			BufferedImage copy = copy(frame);
			BufferedImage shot = copy;
			if (crop != null)
			{
				Rectangle scaled = CaptureFrames.scale(crop, copy.getWidth(), copy.getHeight(), space.width, space.height);
				shot = CaptureFrames.crop(copy, scaled);
				if (shot == null)
				{
					result.completeExceptionally(new CaptureException("Could not capture that area"));
					return;
				}
			}
			BufferedImage finalShot = shot;
			executor.execute(() ->
			{
				try
				{
					result.complete(encodePng(finalShot));
				}
				catch (IOException e)
				{
					result.completeExceptionally(e);
				}
			});
		}
		catch (Throwable t)
		{
			result.completeExceptionally(t);
		}
		finally
		{
			// Next client cycle, after this frame has been copied and before the next draw.
			restore.run();
		}
	}

	private Rectangle chatboxBounds()
	{
		Dimension space = coordinateSpace();
		Rectangle[] parts = new Rectangle[CHATBOX_PARTS.length];
		int count = 0;
		for (int id : CHATBOX_PARTS)
		{
			Widget widget = client.getWidget(id);
			if (widget == null || widget.isHidden())
			{
				continue;
			}
			Rectangle bounds = new Rectangle(widget.getBounds());
			if (!CaptureFrames.include(bounds, space.width, space.height))
			{
				continue;
			}
			parts[count++] = bounds;
		}
		if (count == 0)
		{
			return null;
		}
		Rectangle[] present = new Rectangle[count];
		System.arraycopy(parts, 0, present, 0, count);
		return CaptureFrames.union(present);
	}

	private Dimension coordinateSpace()
	{
		Dimension real = client.getRealDimensions();
		if (real != null && real.width > 0 && real.height > 0)
		{
			return real;
		}
		return new Dimension(Math.max(client.getCanvasWidth(), 1), Math.max(client.getCanvasHeight(), 1));
	}

	private static BufferedImage copy(Image frame)
	{
		BufferedImage image = new BufferedImage(frame.getWidth(null), frame.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.drawImage(frame, 0, 0, null);
		graphics.dispose();
		return image;
	}

	static byte[] encodePng(BufferedImage image) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (!ImageIO.write(image, "png", out))
		{
			throw new IOException("No PNG encoder available");
		}
		return out.toByteArray();
	}
}
