package com.cushty.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON body of {@code POST /v1/events}. Fields are {@code type}, optional {@code rsn},
 * {@code caption} and {@code world}, plus {@code loot} or {@code pet}. Nulls are omitted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPayload
{
	public static final String TYPE_MANUAL = "manual";
	public static final String TYPE_LOOT = "loot";
	public static final String TYPE_PET = "pet";

	private String type;
	private String rsn;
	private String caption;
	private Integer world;
	private Loot loot;
	private Pet pet;

	@Data
	@AllArgsConstructor
	public static class Loot
	{
		private String source;
		private List<LootItem> items;
		private long totalValue;
	}

	@Data
	@AllArgsConstructor
	public static class LootItem
	{
		private int id;
		private String name;
		private int quantity;
		private long priceEach;

		public long stackValue()
		{
			return priceEach * quantity;
		}
	}

	@Data
	@AllArgsConstructor
	public static class Pet
	{
		private boolean duplicate;
		private String message;
	}
}
