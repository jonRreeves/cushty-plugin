package com.cushty.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.Test;

public class CaptureFramesTest
{
	@Test
	public void buildsARectangleFromEitherDragDirection()
	{
		Rectangle area = CaptureFrames.fromPoints(new Point(30, 40), new Point(10, 15));
		assertEquals(new Rectangle(10, 15, 20, 25), area);
	}

	@Test
	public void rejectsWidgetsThatCoverTheWholeCanvas()
	{
		assertFalse(CaptureFrames.include(new Rectangle(0, 0, 800, 600), 800, 600));
		assertTrue(CaptureFrames.include(new Rectangle(0, 340, 519, 142), 765, 503));
		assertFalse(CaptureFrames.include(new Rectangle(0, 0, 4, 4), 765, 503));
	}

	@Test
	public void scalesCanvasCoordinatesOntoALargerImage()
	{
		Rectangle scaled = CaptureFrames.scale(new Rectangle(100, 200, 50, 40), 2000, 1600, 1000, 800);
		assertEquals(new Rectangle(200, 400, 100, 80), scaled);
	}

	@Test
	public void clampsACropThatStartsOffTheImage()
	{
		Rectangle scaled = CaptureFrames.scale(new Rectangle(-10, -10, 30, 30), 100, 100, 100, 100);
		assertEquals(new Rectangle(0, 0, 20, 20), scaled);
		BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		BufferedImage crop = CaptureFrames.crop(image, scaled);
		assertEquals(20, crop.getWidth());
		assertEquals(20, crop.getHeight());
	}

	@Test
	public void dropsAnEmptyScaledArea()
	{
		assertNull(CaptureFrames.scale(new Rectangle(200, 200, 10, 10), 100, 100, 100, 100));
	}
}
