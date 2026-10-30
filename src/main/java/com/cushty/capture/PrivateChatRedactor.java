package com.cushty.capture;

import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Leaves private messages out of one drawn frame.
 * <p>
 * The chat builder asks plugins, via {@code chatFilterCheck}, whether each line should be
 * shown. While a capture is active we reject private messages and rebuild chat, so wrapped
 * lines disappear instead of leaving the text in place. The split private-chat window is a
 * separate widget, so it is hidden for the same frame. Call {@link #end} afterwards to put
 * both back. Login and logout lines are not private messages and stay.
 */
@Singleton
public class PrivateChatRedactor
{
	private final AtomicInteger depth = new AtomicInteger();
	private volatile boolean hidSplitPrivate;

	@Inject
	PrivateChatRedactor()
	{
	}

	public void onScriptCallback(ScriptCallbackEvent event, Client client)
	{
		if (depth.get() <= 0 || !"chatFilterCheck".equals(event.getEventName()))
		{
			return;
		}
		apply(client.getIntStack(), client.getIntStackSize());
	}

	/** Rebuild chat without private messages. Safe to nest; one matching {@link #end} restores it. */
	public void begin(Client client)
	{
		if (depth.getAndIncrement() == 0)
		{
			client.refreshChat();
			hideSplitPrivate(client);
		}
	}

	/** Put private messages back. Does nothing if {@link #begin} was not called. */
	public void end(Client client)
	{
		int left = depth.decrementAndGet();
		if (left > 0)
		{
			return;
		}
		if (left < 0)
		{
			depth.set(0);
			return;
		}
		client.refreshChat();
		showSplitPrivate(client);
	}

	/** Restore even if a capture was abandoned, such as the plugin shutting down. */
	public void forceEnd(Client client)
	{
		if (depth.getAndSet(0) > 0)
		{
			client.refreshChat();
			showSplitPrivate(client);
		}
	}

	static void apply(int[] intStack, int intStackSize)
	{
		// The chat script pushes the show flag, then the message type, then the message id.
		if (intStack == null || intStackSize < 3 || intStack.length < intStackSize)
		{
			return;
		}
		ChatMessageType type = ChatMessageType.of(intStack[intStackSize - 2]);
		if (isPrivate(type))
		{
			intStack[intStackSize - 3] = 0;
		}
	}

	static boolean isPrivate(ChatMessageType type)
	{
		return type == ChatMessageType.PRIVATECHAT
			|| type == ChatMessageType.PRIVATECHATOUT
			|| type == ChatMessageType.MODPRIVATECHAT;
	}

	private void hideSplitPrivate(Client client)
	{
		Widget pm = client.getWidget(InterfaceID.PmChat.CONTAINER);
		if (pm != null && !pm.isHidden())
		{
			pm.setHidden(true);
			hidSplitPrivate = true;
		}
	}

	private void showSplitPrivate(Client client)
	{
		if (!hidSplitPrivate)
		{
			return;
		}
		hidSplitPrivate = false;
		Widget pm = client.getWidget(InterfaceID.PmChat.CONTAINER);
		if (pm != null)
		{
			pm.setHidden(false);
		}
	}
}
