package com.cushty;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Launches RuneLite in developer mode with Cushty loaded. Run with {@code ./gradlew run}. */
public class CushtyPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CushtyPlugin.class);
		RuneLite.main(args);
	}
}
