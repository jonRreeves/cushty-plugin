package com.cushty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/** Sidebar panel: send a full or chatbox screenshot, snip a region, and test the connection. */
class CushtyPanel extends PluginPanel
{
	private static final int MAX_CAPTION = 500;

	private final JTextArea caption = new JTextArea(4, 20);
	private final JButton sendButton = new JButton("Full screen");
	private final JButton snipButton = new JButton("Snip");
	private final JButton testButton = new JButton("Test connection");
	private final JLabel status = new JLabel();
	private boolean busy;
	private boolean snipping;

	CushtyPanel(Consumer<String> onSend, Runnable onSnip, Runnable onTest)
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("Cushty");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);

		JLabel captionLabel = new JLabel("Caption (optional)");
		captionLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		caption.setLineWrap(true);
		caption.setWrapStyleWord(true);
		caption.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		caption.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		JScrollPane captionScroll = new JScrollPane(caption);
		captionScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));

		JPanel top = new JPanel(new BorderLayout(0, 6));
		top.add(title, BorderLayout.NORTH);
		top.add(captionLabel, BorderLayout.CENTER);
		top.add(captionScroll, BorderLayout.SOUTH);

		sendButton.addActionListener(e -> onSend.accept(caption()));
		snipButton.addActionListener(e -> onSnip.run());
		testButton.addActionListener(e -> onTest.run());

		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
		buttons.add(sendButton);
		buttons.add(snipButton);
		buttons.add(testButton);

		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setFont(FontManager.getRunescapeSmallFont());

		JPanel bottom = new JPanel(new BorderLayout(0, 8));
		bottom.add(buttons, BorderLayout.NORTH);
		bottom.add(status, BorderLayout.CENTER);

		add(top, BorderLayout.NORTH);
		add(bottom, BorderLayout.CENTER);
	}

	/** Label for the non-snip button. Matches the capture-area setting. Safe from any thread. */
	void setSendLabel(String label)
	{
		SwingUtilities.invokeLater(() -> sendButton.setText(label));
	}

	/** Safe to call from any thread. */
	void setStatus(String message, boolean error)
	{
		SwingUtilities.invokeLater(() -> showStatus(message, error ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_COMPLETE_COLOR));
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

	private void refreshButtons()
	{
		sendButton.setEnabled(!busy && !snipping);
		snipButton.setEnabled(!busy);
		snipButton.setText(snipping ? "Cancel snip" : "Snip");
	}

	private void showStatus(String message, Color color)
	{
		status.setText("<html>" + escape(message) + "</html>");
		status.setForeground(color);
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
