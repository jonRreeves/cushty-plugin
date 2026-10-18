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

/** Sidebar panel: send a screenshot with an optional caption, and test the connection. */
class CushtyPanel extends PluginPanel
{
	private static final int MAX_CAPTION = 500;

	private final JTextArea caption = new JTextArea(4, 20);
	private final JButton sendButton = new JButton("Send screenshot");
	private final JButton testButton = new JButton("Test connection");
	private final JLabel status = new JLabel();

	CushtyPanel(Consumer<String> onSend, Runnable onTest)
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

		sendButton.addActionListener(e ->
		{
			String text = caption.getText().trim();
			onSend.accept(text.length() > MAX_CAPTION ? text.substring(0, MAX_CAPTION) : text);
		});
		testButton.addActionListener(e -> onTest.run());

		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
		buttons.add(sendButton);
		buttons.add(testButton);

		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setFont(FontManager.getRunescapeSmallFont());

		JPanel bottom = new JPanel(new BorderLayout(0, 8));
		bottom.add(buttons, BorderLayout.NORTH);
		bottom.add(status, BorderLayout.CENTER);

		add(top, BorderLayout.NORTH);
		add(bottom, BorderLayout.CENTER);
	}

	/** Safe to call from any thread. */
	void setStatus(String message, boolean error)
	{
		SwingUtilities.invokeLater(() ->
		{
			status.setText("<html>" + escape(message) + "</html>");
			status.setForeground(error ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_COMPLETE_COLOR);
		});
	}

	/** Safe to call from any thread. */
	void setBusy(boolean busy)
	{
		SwingUtilities.invokeLater(() -> sendButton.setEnabled(!busy));
	}

	/** Safe to call from any thread. */
	void clearCaption()
	{
		SwingUtilities.invokeLater(() -> caption.setText(""));
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
