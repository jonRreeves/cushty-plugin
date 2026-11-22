package com.cushty.loot;

import com.cushty.api.EventPayload;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Value;

/** Decides which items in a drop are worth sharing. Free of RuneLite types so it can be unit tested. */
public final class LootFilter
{
	public static final int MAX_ITEMS = 25;
	/** Hard floor so the valuable-drop threshold cannot be edited down to pocket change. */
	public static final int MIN_STACK_VALUE = 5_000_000;

	private final long minStackValue;
	private final Set<Integer> watchIds;

	public LootFilter(long minStackValue)
	{
		this(minStackValue, Set.of());
	}

	public LootFilter(long minStackValue, Collection<Integer> watchIds)
	{
		this.minStackValue = Math.max(minStackValue, MIN_STACK_VALUE);
		this.watchIds = watchIds == null || watchIds.isEmpty()
			? Set.of()
			: new HashSet<>(watchIds);
	}

	@Value
	public static class Result
	{
		List<EventPayload.LootItem> items;
		long totalValue;

		public boolean isEmpty()
		{
			return items.isEmpty();
		}
	}

	/**
	 * Returns the items to share, most valuable first. Bingo watchlist items are included even
	 * when under the gp threshold. The total value covers the whole drop, not just the selected items.
	 */
	public Result select(Collection<EventPayload.LootItem> drop)
	{
		long total = 0;
		List<EventPayload.LootItem> selected = new ArrayList<>();
		for (EventPayload.LootItem item : drop)
		{
			total += item.stackValue();
			if (item.stackValue() >= minStackValue || watchIds.contains(item.getId()))
			{
				selected.add(item);
			}
		}

		selected.sort(Comparator.comparingLong(EventPayload.LootItem::stackValue).reversed());
		if (selected.size() > MAX_ITEMS)
		{
			selected = new ArrayList<>(selected.subList(0, MAX_ITEMS));
		}
		return new Result(selected, total);
	}
}
