package com.cushty.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.ChatMessageType;
import org.junit.Test;

public class PrivateChatRedactorTest
{
	@Test
	public void blocksIncomingOutgoingAndModeratorPrivateMessages()
	{
		assertTrue(PrivateChatRedactor.isPrivate(ChatMessageType.PRIVATECHAT));
		assertTrue(PrivateChatRedactor.isPrivate(ChatMessageType.PRIVATECHATOUT));
		assertTrue(PrivateChatRedactor.isPrivate(ChatMessageType.MODPRIVATECHAT));
		assertFalse(PrivateChatRedactor.isPrivate(ChatMessageType.PUBLICCHAT));
		assertFalse(PrivateChatRedactor.isPrivate(ChatMessageType.GAMEMESSAGE));
		assertFalse(PrivateChatRedactor.isPrivate(ChatMessageType.LOGINLOGOUTNOTIFICATION));
		assertFalse(PrivateChatRedactor.isPrivate(ChatMessageType.FRIENDSCHAT));
	}

	@Test
	public void clearsTheShowFlagForAPrivateLine()
	{
		int[] stack = stack(ChatMessageType.PRIVATECHATOUT);
		PrivateChatRedactor.apply(stack, stack.length);
		assertEquals(0, stack[0]);

		int[] game = stack(ChatMessageType.GAMEMESSAGE);
		PrivateChatRedactor.apply(game, game.length);
		assertEquals(1, game[0]);
	}

	@Test
	public void ignoresAShortStack()
	{
		int[] stack = new int[] {1, ChatMessageType.PRIVATECHAT.getType()};
		PrivateChatRedactor.apply(stack, stack.length);
		assertEquals(1, stack[0]);
	}

	private static int[] stack(ChatMessageType type)
	{
		// Show flag, message type, message id. Matches the chatFilterCheck script.
		return new int[] {1, type.getType(), 42};
	}
}
