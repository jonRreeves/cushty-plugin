package com.cushty.capture;

/** A screenshot could not be taken for a reason worth showing in the panel. */
public final class CaptureException extends RuntimeException
{
	public CaptureException(String message)
	{
		super(message);
	}
}
