package com.cushty;

import com.cushty.capture.CaptureArea;
import com.cushty.loot.LootFilter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CushtyConfig.GROUP)
public interface CushtyConfig extends Config
{
	String GROUP = "cushty";
	String THIRD_PARTY_WARNING = "This plugin submits your IP address to a 3rd party website not controlled or verified by the RuneLite Developers.";

	@ConfigSection(
		name = "Connection",
		description = "Where to send events. There is no default server. Paste a server URL and a bearer token. Nothing is uploaded until you enable a feature below.",
		position = 0
	)
	String connectionSection = "connection";

	@ConfigSection(
		name = "Capture",
		description = "What is included in every screenshot: drops, pets, and manual shots",
		position = 1
	)
	String captureSection = "capture";

	@ConfigSection(
		name = "Screenshots",
		description = "Manual screenshots",
		position = 2
	)
	String screenshotSection = "screenshots";

	@ConfigSection(
		name = "Valuable drops",
		description = "Automatically share valuable drops",
		position = 3
	)
	String lootSection = "loot";

	@ConfigSection(
		name = "Pets",
		description = "Automatically share pet drops",
		position = 4
	)
	String petSection = "pets";

	@ConfigItem(
		keyName = "serverUrl",
		name = "Server URL",
		description = "HTTPS server that receives your events. There is no default. Leave blank to send nothing. Test connection also contacts this server.",
		section = connectionSection,
		position = 0
	)
	default String serverUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "token",
		name = "Token",
		description = "Bearer token for the server above. Treat it like a password.",
		secret = true,
		section = connectionSection,
		position = 1
	)
	default String token()
	{
		return "";
	}

	@ConfigItem(
		keyName = "includeRsn",
		name = "Include RSN",
		description = "Show your in-game name on posts. When off, your Discord name is used.",
		section = connectionSection,
		position = 2
	)
	default boolean includeRsn()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatNotifications",
		name = "Chat notifications",
		description = "Print a game chat message when something is sent or fails",
		section = connectionSection,
		position = 3
	)
	default boolean chatNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hidePrivateChat",
		name = "Hide private chat",
		description = "Leave private messages out of every screenshot. Chat is redrawn without them for the captured frame, then restored. The split private-chat window is hidden for that frame too.",
		section = captureSection,
		position = 0
	)
	default boolean hidePrivateChat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "captureArea",
		name = "Capture area",
		description = "Entire screen is the game view. Chatbox is only the chatbox. A manual snip uses the area you drag instead.",
		section = captureSection,
		position = 1
	)
	default CaptureArea captureArea()
	{
		return CaptureArea.ENTIRE_SCREEN;
	}

	@ConfigItem(
		keyName = "manualEnabled",
		name = "Enable manual screenshots",
		description = "Send a screenshot with the hotkey or the sidebar. Snip drags a region of the game view. The other button uses Capture area.",
		warning = THIRD_PARTY_WARNING,
		section = screenshotSection,
		position = 0
	)
	default boolean manualEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "screenshotHotkey",
		name = "Screenshot hotkey",
		description = "Hotkey that sends a screenshot of the capture area immediately",
		section = screenshotSection,
		position = 1
	)
	default Keybind screenshotHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "lootEnabled",
		name = "Enable valuable drops",
		description = "Send a screenshot when you receive a valuable drop",
		warning = THIRD_PARTY_WARNING,
		section = lootSection,
		position = 0
	)
	default boolean lootEnabled()
	{
		return false;
	}

	@Range(min = LootFilter.MIN_STACK_VALUE)
	@Units(" gp")
	@ConfigItem(
		keyName = "lootMinValue",
		name = "Minimum value",
		description = "Send when a single item stack in a drop is worth at least this much (Grand Exchange price). Cannot be set below 5M.",
		section = lootSection,
		position = 1
	)
	default int lootMinValue()
	{
		return LootFilter.MIN_STACK_VALUE;
	}

	@ConfigItem(
		keyName = "petsEnabled",
		name = "Enable pets",
		description = "Send a screenshot when you get a pet (including duplicates)",
		warning = THIRD_PARTY_WARNING,
		section = petSection,
		position = 0
	)
	default boolean petsEnabled()
	{
		return false;
	}
}
