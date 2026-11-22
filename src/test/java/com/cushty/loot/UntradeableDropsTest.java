package com.cushty.loot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UntradeableDropsTest
{
	@Test
	public void parsesMolchPearl()
	{
		assertEquals("Molch pearl", UntradeableDrops.parseItemName("Untradeable drop: Molch pearl"));
	}

	@Test
	public void ignoresOtherChat()
	{
		assertNull(UntradeableDrops.parseItemName("You catch a bluegill."));
		assertNull(UntradeableDrops.parseItemName(null));
	}
}
