package com.cushty.pets;

/** Recognises the game messages shown when a pet is rolled. */
public final class PetMessages
{
	public enum Result
	{
		NONE,
		NEW_PET,
		DUPLICATE
	}

	private static final String FOLLOWED = "You have a funny feeling like you're being followed";
	private static final String BACKPACK = "You feel something weird sneaking into your backpack";
	private static final String DUPLICATE = "You have a funny feeling like you would have been followed";

	private PetMessages()
	{
	}

	/** @param message the chat message with formatting tags already removed */
	public static Result classify(String message)
	{
		if (message == null)
		{
			return Result.NONE;
		}
		if (message.startsWith(DUPLICATE))
		{
			return Result.DUPLICATE;
		}
		if (message.startsWith(FOLLOWED) || message.startsWith(BACKPACK))
		{
			return Result.NEW_PET;
		}
		return Result.NONE;
	}
}
