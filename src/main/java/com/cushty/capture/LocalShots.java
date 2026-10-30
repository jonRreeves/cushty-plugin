package com.cushty.capture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Writes a captured screenshot beside RuneLite's other screenshots. Used only by a local developer client. */
public final class LocalShots
{
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	private LocalShots()
	{
	}

	public static File save(File directory, String type, byte[] png) throws IOException
	{
		if (!directory.mkdirs() && !directory.isDirectory())
		{
			throw new IOException("Could not create " + directory);
		}
		File file = unique(directory, fileName(type, LocalDateTime.now()));
		Files.write(file.toPath(), png);
		return file;
	}

	static String fileName(String type, LocalDateTime time)
	{
		String safe = type == null ? "" : type.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
		if (safe.isEmpty())
		{
			safe = "shot";
		}
		return "cushty-" + safe + "-" + STAMP.format(time) + ".png";
	}

	static File unique(File directory, String fileName) throws IOException
	{
		File file = new File(directory, fileName);
		if (!file.exists())
		{
			return file;
		}
		int dot = fileName.lastIndexOf('.');
		String stem = fileName.substring(0, dot);
		String extension = fileName.substring(dot);
		for (int i = 2; i < 1000; i++)
		{
			File candidate = new File(directory, stem + "-" + i + extension);
			if (!candidate.exists())
			{
				return candidate;
			}
		}
		throw new IOException("Could not find a free file name in " + directory);
	}
}
