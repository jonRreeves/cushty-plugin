package com.cushty.loot;

import static org.junit.Assert.assertEquals;
import com.cushty.api.EventPayload.LootItem;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class LootFilterTest
{
	private static List<String> names(LootFilter.Result result)
	{
		return result.getItems().stream().map(LootItem::getName).collect(Collectors.toList());
	}

	@Test
	public void selectsStacksOverThresholdMostValuableFirst()
	{
		LootFilter filter = new LootFilter(LootFilter.MIN_STACK_VALUE);
		LootFilter.Result result = filter.select(List.of(
			new LootItem(1, "Dragon bones", 2, 2_000),
			new LootItem(2, "Draconic visage", 1, 4_000_000),
			new LootItem(3, "Skeletal visage", 1, 30_000_000),
			new LootItem(4, "Twisted bow", 1, 1_000_000_000),
			new LootItem(5, "Rune dart", 600, 2_000)));

		assertEquals(List.of("Twisted bow", "Skeletal visage"), names(result));
		assertEquals(1_000_000_000L + 30_000_000 + 4_000_000 + 1_200_000 + 4_000, result.getTotalValue());
	}

	@Test
	public void raisesThresholdsBelowTheFloor()
	{
		LootFilter filter = new LootFilter(1);
		LootFilter.Result result = filter.select(List.of(
			new LootItem(1, "Bones", 1, 0),
			new LootItem(2, "Draconic visage", 1, 4_000_000),
			new LootItem(3, "Twisted bow", 1, LootFilter.MIN_STACK_VALUE)));

		assertEquals(List.of("Twisted bow"), names(result));
	}

	@Test
	public void capsItemCount()
	{
		List<LootItem> drop = new ArrayList<>();
		for (int i = 0; i < 40; i++)
		{
			drop.add(new LootItem(i, "Item " + i, 1, LootFilter.MIN_STACK_VALUE + i));
		}
		LootFilter.Result result = new LootFilter(LootFilter.MIN_STACK_VALUE).select(drop);
		assertEquals(LootFilter.MAX_ITEMS, result.getItems().size());
		assertEquals("Item 39", result.getItems().get(0).getName());
	}
}
