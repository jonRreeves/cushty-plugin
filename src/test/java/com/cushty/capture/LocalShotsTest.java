package com.cushty.capture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalShotsTest
{
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void namesFilesFromTheEventTypeAndTime()
	{
		assertEquals(
			"cushty-manual-2026-10-30_10-06-00.png",
			LocalShots.fileName("manual", LocalDateTime.of(2026, 10, 30, 10, 6, 0)));
		assertEquals(
			"cushty-shot-2026-10-30_10-06-00.png",
			LocalShots.fileName("!!!", LocalDateTime.of(2026, 10, 30, 10, 6, 0)));
	}

	@Test
	public void writesThePngAndDoesNotOverwriteAnExistingFile() throws Exception
	{
		File dir = folder.newFolder("cushty");
		File first = LocalShots.save(dir, "manual", new byte[] {1, 2, 3});
		File second = LocalShots.save(dir, "manual", new byte[] {4, 5});

		assertTrue(first.getName().startsWith("cushty-manual-"));
		assertTrue(first.getName().endsWith(".png"));
		assertFalse(first.equals(second));
		assertEquals(3, Files.readAllBytes(first.toPath()).length);
		assertEquals(2, Files.readAllBytes(second.toPath()).length);
	}
}
