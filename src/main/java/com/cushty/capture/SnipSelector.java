package com.cushty.capture;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Drag a rectangle on the game view. The rectangle is drawn as an overlay and removed before
 * the screenshot is taken, so the border is not in the image.
 */
@Singleton
public class SnipSelector
{
	static final int MIN_SIZE = 8;

	private final Client client;
	private final OverlayManager overlayManager;
	private final MouseManager mouseManager;
	private final SnipOverlay overlay = new SnipOverlay();
	private final SnipMouse mouse = new SnipMouse();
	private final SnipKeys keys = new SnipKeys();

	private volatile boolean active;
	private volatile Point anchor;
	private volatile Rectangle selection;
	private Consumer<Rectangle> onReady;
	private Runnable onCancel;
	private Consumer<String> onHint;

	@Inject
	SnipSelector(Client client, OverlayManager overlayManager, MouseManager mouseManager)
	{
		this.client = client;
		this.overlayManager = overlayManager;
		this.mouseManager = mouseManager;
	}

	public KeyListener keys()
	{
		return keys;
	}

	public void registerMouse()
	{
		mouseManager.unregisterMouseListener(mouse);
		mouseManager.registerMouseListener(mouse);
	}

	public void unregisterMouse()
	{
		mouseManager.unregisterMouseListener(mouse);
	}

	public boolean isActive()
	{
		return active;
	}

	public void start(Consumer<Rectangle> onReady, Runnable onCancel, Consumer<String> onHint)
	{
		if (active)
		{
			return;
		}
		this.onReady = onReady;
		this.onCancel = onCancel;
		this.onHint = onHint;
		anchor = null;
		selection = null;
		active = true;
		// Sit in front of other mouse listeners so the drag is not also a game click.
		mouseManager.unregisterMouseListener(mouse);
		mouseManager.registerMouseListener(0, mouse);
		overlayManager.add(overlay);
	}

	/** Stop drawing and tell the caller the snip was cancelled. */
	public void cancel()
	{
		if (!active)
		{
			return;
		}
		stopVisuals();
		Runnable cancel = onCancel;
		if (cancel != null)
		{
			cancel.run();
		}
	}

	/** Stop drawing without a cancel notice. Used when another screenshot takes over. */
	public void dismiss()
	{
		if (!active)
		{
			return;
		}
		stopVisuals();
	}

	private void stopVisuals()
	{
		active = false;
		anchor = null;
		selection = null;
		overlayManager.remove(overlay);
	}

	private void finish(Rectangle area)
	{
		Consumer<Rectangle> ready = onReady;
		stopVisuals();
		if (ready != null)
		{
			ready.accept(area);
		}
	}

	private final class SnipOverlay extends Overlay
	{
		private SnipOverlay()
		{
			setPosition(OverlayPosition.DYNAMIC);
			setLayer(OverlayLayer.ALWAYS_ON_TOP);
			setPriority(PRIORITY_HIGH);
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			Rectangle area = selection;
			if (!active || area == null)
			{
				return null;
			}
			Dimension canvas = canvasSize();
			graphics.setColor(new Color(0, 0, 0, 100));
			dimAround(graphics, canvas, area);
			graphics.setColor(new Color(255, 208, 64));
			graphics.setStroke(new BasicStroke(1));
			graphics.draw(area);
			return null;
		}
	}

	private final class SnipMouse extends MouseAdapter
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			if (!active || SwingUtilities.isMiddleMouseButton(event))
			{
				return event;
			}
			if (SwingUtilities.isLeftMouseButton(event))
			{
				anchor = event.getPoint();
				selection = new Rectangle(anchor.x, anchor.y, 0, 0);
			}
			else
			{
				cancel();
			}
			event.consume();
			return event;
		}

		@Override
		public MouseEvent mouseDragged(MouseEvent event)
		{
			if (!active || anchor == null || !SwingUtilities.isLeftMouseButton(event))
			{
				return event;
			}
			selection = CaptureFrames.fromPoints(anchor, event.getPoint());
			event.consume();
			return event;
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent event)
		{
			if (!active || !SwingUtilities.isLeftMouseButton(event))
			{
				return event;
			}
			Rectangle area = selection;
			anchor = null;
			if (area == null || area.width < MIN_SIZE || area.height < MIN_SIZE)
			{
				selection = null;
				Consumer<String> hint = onHint;
				if (hint != null)
				{
					hint.accept("Drag a larger area.");
				}
			}
			else
			{
				finish(new Rectangle(area));
			}
			event.consume();
			return event;
		}

		@Override
		public MouseEvent mouseClicked(MouseEvent event)
		{
			if (!active || SwingUtilities.isMiddleMouseButton(event))
			{
				return event;
			}
			event.consume();
			return event;
		}
	}

	private final class SnipKeys implements KeyListener
	{
		@Override
		public void keyPressed(KeyEvent event)
		{
			if (active && event.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				event.consume();
				cancel();
			}
		}

		@Override
		public void keyReleased(KeyEvent event)
		{
		}

		@Override
		public void keyTyped(KeyEvent event)
		{
		}

		@Override
		public boolean isEnabledOnLoginScreen()
		{
			return true;
		}
	}

	private Dimension canvasSize()
	{
		Dimension real = client.getRealDimensions();
		if (real != null && real.width > 0 && real.height > 0)
		{
			return real;
		}
		return new Dimension(Math.max(client.getCanvasWidth(), 1), Math.max(client.getCanvasHeight(), 1));
	}

	private static void dimAround(Graphics2D graphics, Dimension canvas, Rectangle hole)
	{
		int right = hole.x + hole.width;
		int bottom = hole.y + hole.height;
		graphics.fillRect(0, 0, canvas.width, Math.max(0, hole.y));
		graphics.fillRect(0, bottom, canvas.width, Math.max(0, canvas.height - bottom));
		graphics.fillRect(0, hole.y, Math.max(0, hole.x), hole.height);
		graphics.fillRect(right, hole.y, Math.max(0, canvas.width - right), hole.height);
	}
}
