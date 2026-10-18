package com.cushty.capture;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.DrawManager;

/**
 * Captures the next rendered game frame. This is the game canvas only: never the desktop, other
 * windows or the RuneLite sidebar.
 */
@Singleton
public class ScreenshotCapture
{
	private final DrawManager drawManager;
	private final ScheduledExecutorService executor;

	@Inject
	ScreenshotCapture(DrawManager drawManager, ScheduledExecutorService executor)
	{
		this.drawManager = drawManager;
		this.executor = executor;
	}

	/** @return a future completed with PNG bytes, off the client thread */
	public CompletableFuture<byte[]> captureNextFrame()
	{
		CompletableFuture<byte[]> result = new CompletableFuture<>();
		drawManager.requestNextFrameListener(frame ->
		{
			// The frame buffer may be reused by the renderer, so copy it before leaving this callback.
			BufferedImage copy = copy(frame);
			executor.execute(() ->
			{
				try
				{
					result.complete(encodePng(copy));
				}
				catch (IOException e)
				{
					result.completeExceptionally(e);
				}
			});
		});
		return result;
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
