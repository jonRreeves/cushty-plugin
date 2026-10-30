package com.cushty.capture;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Geometry for cropping a game-canvas screenshot. Coordinates are canvas pixels, not stretched pixels. */
final class CaptureFrames
{
	private static final int MIN_PART = 8;

	private CaptureFrames()
	{
	}

	static Rectangle fromPoints(Point a, Point b)
	{
		int x = Math.min(a.x, b.x);
		int y = Math.min(a.y, b.y);
		return new Rectangle(x, y, Math.abs(a.x - b.x), Math.abs(a.y - b.y));
	}

	/**
	 * A widget is part of the chatbox when it has a real size and is not the full game view.
	 * Some parent widgets report bounds that cover the whole canvas.
	 */
	static boolean include(Rectangle part, int spaceWidth, int spaceHeight)
	{
		if (part == null || spaceWidth <= 0 || spaceHeight <= 0)
		{
			return false;
		}
		if (part.width < MIN_PART || part.height < MIN_PART)
		{
			return false;
		}
		boolean coversWidth = part.width > spaceWidth * 0.95;
		boolean coversHeight = part.height > spaceHeight * 0.95;
		return !(coversWidth && coversHeight);
	}

	static Rectangle union(Rectangle... parts)
	{
		Rectangle union = null;
		for (Rectangle part : parts)
		{
			if (part == null)
			{
				continue;
			}
			union = union == null ? new Rectangle(part) : union.union(part);
		}
		return union;
	}

	/** Map a canvas rectangle onto a screenshot, which may be stretched or scaled for the display. */
	static Rectangle scale(Rectangle canvas, int imageWidth, int imageHeight, int spaceWidth, int spaceHeight)
	{
		if (canvas == null || imageWidth <= 0 || imageHeight <= 0 || spaceWidth <= 0 || spaceHeight <= 0)
		{
			return null;
		}
		double scaleX = imageWidth / (double) spaceWidth;
		double scaleY = imageHeight / (double) spaceHeight;
		int x1 = clamp((int) Math.round(canvas.x * scaleX), 0, imageWidth);
		int y1 = clamp((int) Math.round(canvas.y * scaleY), 0, imageHeight);
		int x2 = clamp((int) Math.round((canvas.x + canvas.width) * scaleX), 0, imageWidth);
		int y2 = clamp((int) Math.round((canvas.y + canvas.height) * scaleY), 0, imageHeight);
		if (x2 < x1)
		{
			int swap = x1;
			x1 = x2;
			x2 = swap;
		}
		if (y2 < y1)
		{
			int swap = y1;
			y1 = y2;
			y2 = swap;
		}
		if (x2 <= x1 || y2 <= y1)
		{
			return null;
		}
		return new Rectangle(x1, y1, x2 - x1, y2 - y1);
	}

	static BufferedImage crop(BufferedImage image, Rectangle area)
	{
		if (area == null || area.width <= 0 || area.height <= 0)
		{
			return null;
		}
		if (area.x < 0 || area.y < 0 || area.x + area.width > image.getWidth() || area.y + area.height > image.getHeight())
		{
			return null;
		}
		return image.getSubimage(area.x, area.y, area.width, area.height);
	}

	private static int clamp(int value, int min, int max)
	{
		return Math.max(min, Math.min(max, value));
	}
}
