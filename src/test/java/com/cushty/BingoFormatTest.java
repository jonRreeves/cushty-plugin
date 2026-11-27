package com.cushty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cushty.api.BingoModels;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class BingoFormatTest
{
	@Test
	public void genericNameIsDetected()
	{
		assertTrue(BingoFormat.isGenericName("Tile 1", 1));
		assertTrue(BingoFormat.isGenericName("tile", 2));
		assertTrue(BingoFormat.isGenericName("", 1));
		assertFalse(BingoFormat.isGenericName("Molch pearls", 1));
		assertFalse(BingoFormat.isGenericName("Clue scroll tile", 3));
	}

	@Test
	public void prefersCustomNameOverRequirements()
	{
		BingoModels.Tile tile = tile("Aerial fishing", allGroup(item("Molch pearl", 1, 1)));
		assertEquals("#1 Aerial fishing", BingoFormat.tileLine(tile, 1));
	}

	@Test
	public void fallsBackToItemNameWhenTitleIsGeneric()
	{
		BingoModels.Tile tile = tile("Tile 1", allGroup(item("Molch pearl", 1, 1)));
		assertEquals("#1 Molch pearl", BingoFormat.tileLine(tile, 1));
	}

	@Test
	public void showsProgressForQtyRequirements()
	{
		BingoModels.Tile tile = tile("Tile 2", allGroup(item("Water Rune", 5, 2)));
		assertEquals("#2 2/5 Water Rune", BingoFormat.tileLine(tile, 2));
	}

	@Test
	public void orGroupsJoinWithOr()
	{
		BingoModels.Tile tile = new BingoModels.Tile();
		tile.setName("Tile");
		tile.setGroups(Arrays.asList(
			allGroup(item("Bandos chestplate", 1, 0)),
			allGroup(item("Bandos tassets", 1, 0))));
		assertEquals("#1 Bandos chestplate or Bandos tassets", BingoFormat.tileLine(tile, 1));
	}

	@Test
	public void poolGroupSummarizesAnyN()
	{
		BingoModels.TileGroup pool = new BingoModels.TileGroup();
		pool.setMode("pool");
		pool.setPoolQty(3);
		pool.setItems(Arrays.asList(
			item("Berserker ring", 1, 0),
			item("Seers ring", 1, 0),
			item("Warrior ring", 1, 0)));
		BingoModels.Tile tile = new BingoModels.Tile();
		tile.setName("Tile 20");
		tile.setGroups(Collections.singletonList(pool));
		assertEquals("#20 any 3 of: Berserker ring, Seers ring, Warrior ring", BingoFormat.tileLine(tile, 20));
	}

	@Test
	public void manualGenericTileSaysManual()
	{
		BingoModels.Tile tile = new BingoModels.Tile();
		tile.setName("Tile 3");
		tile.setGroups(Collections.emptyList());
		assertEquals("#3 manual", BingoFormat.tileLine(tile, 3));
	}

	@Test
	public void formatBoardUsesLabelsNotTileTile()
	{
		BingoModels.Board board = new BingoModels.Board();
		board.setEventName("Clan Bingo");
		board.setTeamName("Red");
		board.setWidth(2);
		board.setHeight(1);

		BingoModels.Tile a = tile("Tile 1", allGroup(item("Molch pearl", 1, 1)));
		a.setComplete(true);
		a.setX(0);
		a.setY(0);
		BingoModels.Tile b = tile("Clue scroll", Collections.emptyList());
		b.setX(1);
		b.setY(0);
		board.setTiles(Arrays.asList(a, b));

		String text = BingoFormat.formatBoard(board, null);
		assertTrue(text.contains("[x] #1 Molch pearl"));
		assertTrue(text.contains("[ ] #2 Clue scroll"));
		assertFalse(text.contains("Tile 1 - Tile 1"));
	}

	private static BingoModels.Tile tile(String name, BingoModels.TileGroup group)
	{
		return tile(name, Collections.singletonList(group));
	}

	private static BingoModels.Tile tile(String name, java.util.List<BingoModels.TileGroup> groups)
	{
		BingoModels.Tile tile = new BingoModels.Tile();
		tile.setName(name);
		tile.setGroups(groups);
		return tile;
	}

	private static BingoModels.TileGroup allGroup(BingoModels.TileItem item)
	{
		BingoModels.TileGroup group = new BingoModels.TileGroup();
		group.setMode("all");
		group.setItems(Collections.singletonList(item));
		return group;
	}

	private static BingoModels.TileItem item(String name, int required, int progress)
	{
		BingoModels.TileItem item = new BingoModels.TileItem();
		item.setItemName(name);
		item.setRequiredQty(required);
		item.setProgress(progress);
		item.setItemId(1);
		return item;
	}
}
