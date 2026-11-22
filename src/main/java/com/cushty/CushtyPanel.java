package com.cushty;

import com.cushty.api.BingoModels;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Sidebar: Connection, Manual screenshots, and Bingo — kept simple for players in-game.
 * RuneLite's font only has basic glyphs, so bingo text stays ASCII.
 */
class CushtyPanel extends PluginPanel
{
	private static final int MAX_CAPTION = 500;
	private static final int HISTORY_LIMIT = 8;
	private static final DateTimeFormatter HISTORY_TIME =
		DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

	private final JTextArea caption = new JTextArea(3, 20);
	private final JButton sendButton = new JButton("Full screen");
	private final JButton snipButton = new JButton("Snip");
	private final JButton testButton = new JButton("Test connection");
	private final JButton refreshBingoButton = new JButton("Refresh board");
	private final JTextArea bingoBoard = new JTextArea(10, 20);
	/** Shown under Test connection (and reused for snip/send feedback). */
	private final JLabel status = new JLabel(" ");
	private boolean busy;
	private boolean snipping;

	CushtyPanel(Consumer<String> onSend, Runnable onSnip, Runnable onTest, Runnable onRefreshBingo)
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("Cushty");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);

		// ---- Connection -----------------------------------------------------------------------
		testButton.addActionListener(e -> onTest.run());
		testButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		testButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setFont(FontManager.getRunescapeSmallFont());
		status.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel connectionBody = new JPanel();
		connectionBody.setLayout(new BoxLayout(connectionBody, BoxLayout.Y_AXIS));
		connectionBody.setOpaque(false);
		connectionBody.add(testButton);
		connectionBody.add(Box.createVerticalStrut(4));
		connectionBody.add(status);

		// ---- Manual screenshots ---------------------------------------------------------------
		JLabel captionLabel = new JLabel("Caption (optional)");
		captionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		caption.setLineWrap(true);
		caption.setWrapStyleWord(true);
		caption.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		caption.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		JScrollPane captionScroll = new JScrollPane(caption);
		captionScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		captionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		captionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

		sendButton.addActionListener(e -> onSend.accept(caption()));
		snipButton.addActionListener(e -> onSnip.run());

		JPanel shotButtons = new JPanel(new GridLayout(0, 1, 0, 6));
		shotButtons.add(sendButton);
		shotButtons.add(snipButton);
		shotButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
		shotButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

		JPanel manualBody = new JPanel();
		manualBody.setLayout(new BoxLayout(manualBody, BoxLayout.Y_AXIS));
		manualBody.setOpaque(false);
		manualBody.add(captionLabel);
		manualBody.add(Box.createVerticalStrut(4));
		manualBody.add(captionScroll);
		manualBody.add(Box.createVerticalStrut(6));
		manualBody.add(shotButtons);

		// ---- Bingo ----------------------------------------------------------------------------
		refreshBingoButton.addActionListener(e -> onRefreshBingo.run());
		bingoBoard.setEditable(false);
		bingoBoard.setLineWrap(true);
		bingoBoard.setWrapStyleWord(true);
		bingoBoard.setFont(FontManager.getRunescapeSmallFont());
		bingoBoard.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bingoBoard.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		bingoBoard.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		bingoBoard.setText(
			"Turn on Bingo in settings, then refresh.\n\n"
				+ "Drops for your team's tiles are posted automatically.");
		JScrollPane bingoScroll = new JScrollPane(bingoBoard);
		bingoScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		bingoScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

		refreshBingoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		refreshBingoButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

		JPanel bingoBody = new JPanel();
		bingoBody.setLayout(new BoxLayout(bingoBody, BoxLayout.Y_AXIS));
		bingoBody.setOpaque(false);
		bingoBody.add(refreshBingoButton);
		bingoBody.add(Box.createVerticalStrut(6));
		bingoBody.add(bingoScroll);

		JPanel stack = new JPanel();
		stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
		stack.setOpaque(false);
		stack.add(title);
		stack.add(Box.createVerticalStrut(10));
		stack.add(section("Connection", connectionBody));
		stack.add(Box.createVerticalStrut(10));
		stack.add(section("Manual screenshots", manualBody));
		stack.add(Box.createVerticalStrut(10));
		stack.add(section("Bingo", bingoBody));

		JScrollPane outer = new JScrollPane(stack);
		outer.setBorder(BorderFactory.createEmptyBorder());
		outer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		outer.getVerticalScrollBar().setUnitIncrement(16);
		add(outer, BorderLayout.CENTER);
	}

	private static JPanel section(String name, JComponent body)
	{
		JLabel label = new JLabel(name);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);

		body.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel wrap = new JPanel();
		wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
		wrap.setOpaque(false);
		wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrap.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			BorderFactory.createEmptyBorder(0, 0, 8, 0)));
		wrap.add(label);
		wrap.add(Box.createVerticalStrut(6));
		wrap.add(body);
		return wrap;
	}

	/** Label for the non-snip button. Matches the capture-area setting. Safe from any thread. */
	void setSendLabel(String label)
	{
		SwingUtilities.invokeLater(() -> sendButton.setText(label));
	}

	/** Safe to call from any thread. */
	void setStatus(String message, boolean error)
	{
		SwingUtilities.invokeLater(() ->
			showStatus(message, error ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_COMPLETE_COLOR));
	}

	/** Neutral status, for snip instructions. Safe to call from any thread. */
	void setHint(String message)
	{
		SwingUtilities.invokeLater(() -> showStatus(message, ColorScheme.LIGHT_GRAY_COLOR));
	}

	/** Safe to call from any thread. */
	void setBusy(boolean busy)
	{
		SwingUtilities.invokeLater(() ->
		{
			this.busy = busy;
			refreshButtons();
		});
	}

	/** Safe to call from any thread. */
	void setSnipping(boolean snipping)
	{
		SwingUtilities.invokeLater(() ->
		{
			this.snipping = snipping;
			refreshButtons();
		});
	}

	/** Call on the Swing thread. */
	String caption()
	{
		String text = caption.getText().trim();
		return text.length() > MAX_CAPTION ? text.substring(0, MAX_CAPTION) : text;
	}

	/** Safe to call from any thread. */
	void clearCaption()
	{
		SwingUtilities.invokeLater(() -> caption.setText(""));
	}

	/** Safe to call from any thread. */
	void showBingoBoard(BingoModels.Board board, BingoModels.History history)
	{
		SwingUtilities.invokeLater(() ->
		{
			bingoBoard.setText(formatBingo(board, history));
			bingoBoard.setCaretPosition(0);
		});
	}

	/** Safe to call from any thread. */
	void showBingoMessage(String message)
	{
		SwingUtilities.invokeLater(() -> bingoBoard.setText(message));
	}

	private void refreshButtons()
	{
		sendButton.setEnabled(!busy && !snipping);
		snipButton.setEnabled(!busy);
		snipButton.setText(snipping ? "Cancel snip" : "Snip");
		testButton.setEnabled(!busy && !snipping);
		refreshBingoButton.setEnabled(!busy && !snipping);
	}

	private void showStatus(String message, Color color)
	{
		status.setText("<html>" + escape(message) + "</html>");
		status.setForeground(color);
	}

	private static String formatBingo(BingoModels.Board board, BingoModels.History history)
	{
		List<BingoModels.Tile> tiles = board.getTiles();
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
				.append("Tile ")
				.append(n++)
				.append(" - ")
				.append(tile.getName())
				.append('\n');
		}

		sb.append("\nRecent\n");
		sb.append(formatHistory(history, tiles));
		return sb.toString().trim();
	}

	private static String formatHistory(BingoModels.History history, List<BingoModels.Tile> tiles)
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
			String when = HISTORY_TIME.format(Instant.ofEpochMilli(entry.getCreatedAt()));
			int tileNum = tileNumber(tiles, entry.getX(), entry.getY());
			String line;
			if (entry.getItemId() == 0)
			{
				line = when + " - Tile " + tileNum + " marked done";
			}
			else
			{
				String item = entry.getItemName() != null ? entry.getItemName() : ("item " + entry.getItemId());
				line = when + " - +" + entry.getQuantity() + " " + item + " - Tile " + tileNum;
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

	private static int tileNumber(List<BingoModels.Tile> tiles, int x, int y)
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

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
