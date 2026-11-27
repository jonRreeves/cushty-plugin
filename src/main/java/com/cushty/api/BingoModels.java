package com.cushty.api;

import java.util.Collections;
import java.util.List;
import lombok.Data;

/** JSON shapes for {@code GET /v1/bingo/*}. Unknown fields are ignored by Gson. */
public final class BingoModels
{
	private BingoModels()
	{
	}

	@Data
	public static class Watchlist
	{
		private boolean active;
		private Integer eventId;
		private String eventName;
		private Integer teamId;
		private String teamName;
		private List<WatchItem> items = Collections.emptyList();
	}

	@Data
	public static class WatchItem
	{
		private int id;
		private String name;
	}

	@Data
	public static class Board
	{
		private int eventId;
		private String eventName;
		private int teamId;
		private String teamName;
		private int width;
		private int height;
		private List<Tile> tiles = Collections.emptyList();
	}

	@Data
	public static class Tile
	{
		private int tileId;
		private String name;
		private int x;
		private int y;
		private boolean complete;
		private List<TileGroup> groups = Collections.emptyList();
	}

	@Data
	public static class TileGroup
	{
		/** {@code all} (AND) or {@code pool} (any N of these items). */
		private String mode = "all";
		private Integer poolQty;
		private List<TileItem> items = Collections.emptyList();
	}

	@Data
	public static class TileItem
	{
		private int itemId;
		private String itemName;
		private int requiredQty;
		private int progress;
	}

	@Data
	public static class History
	{
		private int eventId;
		private String eventName;
		private int teamId;
		private String teamName;
		private List<HistoryEntry> entries = Collections.emptyList();
	}

	@Data
	public static class HistoryEntry
	{
		private int id;
		private int tileId;
		private String tileName;
		private int x;
		private int y;
		private int itemId;
		private String itemName;
		private int quantity;
		private String status;
		private String source;
		private String discordId;
		private long createdAt;
		private Long reversedAt;
		private String reversedBy;
	}
}
