package com.cushty.pets;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PetMessagesTest
{
	@Test
	public void classifiesPetMessages()
	{
		assertEquals(PetMessages.Result.NEW_PET,
			PetMessages.classify("You have a funny feeling like you're being followed."));
		assertEquals(PetMessages.Result.NEW_PET,
			PetMessages.classify("You feel something weird sneaking into your backpack."));
		assertEquals(PetMessages.Result.DUPLICATE,
			PetMessages.classify("You have a funny feeling like you would have been followed..."));
		assertEquals(PetMessages.Result.NONE, PetMessages.classify("Oh dear, you are dead!"));
		assertEquals(PetMessages.Result.NONE, PetMessages.classify(null));
	}
}
