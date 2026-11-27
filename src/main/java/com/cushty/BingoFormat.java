package com.cushty;

import com.cushty.api.BingoModels;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ASCII-safe bingo board text for the RuneLite sidebar (RS font has no fancy glyphs).
 */
final class BingoFormat
{
	private static final Pattern GENERIC_TILE_NAME = Pattern.compile("(?i)^tile\\s*\\d*$");
	private static final int HISTORY_LIMIT = 8;
	private static final DateTimeFormatter HISTORY_TIME =
		DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

	private BingoFormat()
	{
	}

	static String formatBoard(BingoModels.Board board, BingoModels.History history)
	{
		List<BingoModels.Tile> tiles = new ArrayList<>(board.getTiles());
		tiles.sort(Comparator.comparingInt(BingoModels.Tile::getY).thenComparingInt(BingoModels.Tile::getX));
		long done = tiles.stream().filter(BingoModels.Tile::isComplete).count();

		StringBuilder sb = new StringBuilder();
		sb.append(board.getEventName()).append('\n');
		sb.append(board.getTeamName())
			.append(" - ")
			.append(board.getWidth())
			.append('x')
			.append(board.getHeight())
			.append('\n');
		sb.append(done).append('/').append(tiles.size()).append(" tiles done\n\n");

		int n = 1;
		for (BingoModels.Tile tile : tiles)
		{
			sb.append(tile.isComplete() ? "[x] " : "[ ] ")
				.append(tileLine(tile, n++))
				.append('\n');
		}

		sb.append("\nRecent\n");
		sb.append(formatHistory(history, tiles));
		return sb.toString().trim();
	}

	/** One tile: "#1 Molch pearl" / "#2 Clue scroll" — never "Tile 1 - Tile 1". */
	static String tileLine(BingoModels.Tile tile, int n)
	{
		String label = tileLabel(tile, n);
		return "#" + n + " " + label;
	}

	static String tileLabel(BingoModels.Tile tile, int n)
	{
		String name = tile.getName() != null ? tile.getName().trim() : "";
		if (!isGenericName(name, n))
		{
			return name;
		}
		String fromReqs = summarizeRequirements(tile);
		if (fromReqs != null)
		{
			return fromReqs;
		}
		return "manual";
	}

	static boolean isGenericName(String name, int n)
	{
		if (name == null || name.isEmpty())
		{
			return true;
		}
		if (GENERIC_TILE_NAME.matcher(name.trim()).matches())
		{
			return true;
		}
		return name.trim().equalsIgnoreCase("Tile " + n);
	}

	/**
	 * Compact requirement summary, e.g. {@code Molch pearl}, {@code 0/5 Water Rune},
	 * {@code A + B}, {@code A or B}, {@code any 3 of: A, B, C}.
	 */
	static String summarizeRequirements(BingoModels.Tile tile)
	{
		List<BingoModels.TileGroup> groups = tile.getGroups();
		if (groups == null || groups.isEmpty())
		{
			return null;
		}
		List<String> groupParts = new ArrayList<>();
		for (BingoModels.TileGroup group : groups)
		{
			if (group == null || group.getItems() == null || group.getItems().isEmpty())
			{
				continue;
			}
			if ("pool".equalsIgnoreCase(group.getMode()))
			{
				int need = group.getPoolQty() != null ? group.getPoolQty() : group.getItems().size();
				List<String> names = new ArrayList<>();
				for (BingoModels.TileItem item : group.getItems())
				{
					names.add(item.getItemName() != null ? item.getItemName() : ("item " + item.getItemId()));
				}
				groupParts.add("any " + need + " of: " + String.join(", ", names));
				continue;
			}
			List<String> andParts = new ArrayList<>();
			for (BingoModels.TileItem item : group.getItems())
			{
				andParts.add(formatItem(item, tile.isComplete()));
			}
			groupParts.add(String.join(" + ", andParts));
		}
		if (groupParts.isEmpty())
		{
			return null;
		}
		return String.join(" or ", groupParts);
	}

	static String formatItem(BingoModels.TileItem item, boolean tileComplete)
	{
		String name = item.getItemName() != null && !item.getItemName().isEmpty()
			? item.getItemName()
			: ("item " + item.getItemId());
		if (item.getRequiredQty() <= 1)
		{
			return name;
		}
		if (tileComplete)
		{
			return item.getRequiredQty() + "x " + name;
		}
		int progress = Math.max(0, item.getProgress());
		return progress + "/" + item.getRequiredQty() + " " + name;
	}

	static String formatHistory(BingoModels.History history, List<BingoModels.Tile> tiles)
	{
		if (history == null || history.getEntries() == null || history.getEntries().isEmpty())
		{
			return "No activity yet.";
		}
		StringBuilder sb = new StringBuilder();
		int shown = 0;
		for (BingoModels.HistoryEntry entry : history.getEntries())
		{
			if (shown >= HISTORY_LIMIT)
			{
				break;
			}
			if ("REVERSED".equalsIgnoreCase(entry.getStatus()))
			{
				continue;
			}
			int tileNum = tileNumber(tiles, entry.getX(), entry.getY());
			String when = historyTime(entry.getCreatedAt());
			String tileRef = "#" + tileNum;
			String line;
			if (entry.getItemId() == 0)
			{
				line = when + " - " + tileRef + " marked done";
			}
			else
			{
				String item = entry.getItemName() != null ? entry.getItemName() : ("item " + entry.getItemId());
				line = when + " - +" + entry.getQuantity() + " " + item + " (" + tileRef + ")";
			}
			sb.append("- ").append(line).append('\n');
			shown++;
		}
		if (shown == 0)
		{
			return "No activity yet.";
		}
		return sb.toString().trim();
	}

	static int tileNumber(List<BingoModels.Tile> tiles, int x, int y)
	{
		int n = 1;
		for (BingoModels.Tile tile : tiles)
		{
			if (tile.getX() == x && tile.getY() == y)
			{
				return n;
			}
			n++;
		}
		return Math.max(1, y * 10 + x + 1);
	}

	private static String historyTime(long createdAtMs)
	{
		return HISTORY_TIME.format(Instant.ofEpochMilli(createdAtMs));
	}
}
