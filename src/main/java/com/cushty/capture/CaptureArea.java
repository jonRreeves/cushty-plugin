package com.cushty.capture;

/** Which part of the game view a non-snip screenshot uses. */
public enum CaptureArea
{
	ENTIRE_SCREEN("Entire screen"),
	CHATBOX("Chatbox");

	private final String label;

	CaptureArea(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}

	/** Sidebar button for a manual shot that follows this setting. */
	public String buttonLabel()
	{
		return this == CHATBOX ? "Chatbox" : "Full screen";
	}
}
