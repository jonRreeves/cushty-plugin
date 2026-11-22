package com.cushty.loot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the game's {@code Untradeable drop: Item name} chat line.
 * Aerial fishing (Molch pearls) and similar activities announce this way and often never
 * fire a loot-tracker {@code LootReceived} that Cushty would see.
 */
public final class UntradeableDrops
{
	private static final Pattern PATTERN = Pattern.compile(
		"^Untradeable drop:\\s*(.+)$",
		Pattern.CASE_INSENSITIVE);

	private UntradeableDrops()
	{
	}

	/** Returns the item name, or {@code null} if the message is not an untradeable-drop line. */
	public static String parseItemName(String plainChat)
	{
		if (plainChat == null || plainChat.isBlank())
		{
			return null;
		}
		Matcher match = PATTERN.matcher(plainChat.trim());
		return match.matches() ? match.group(1).trim() : null;
	}
}
