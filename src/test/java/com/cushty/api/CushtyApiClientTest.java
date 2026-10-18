package com.cushty.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

public class CushtyApiClientTest
{
	@Test
	public void requiresHttpsExceptForLocalhost()
	{
		assertNotNull(CushtyApiClient.parseServerUrl("https://cushty.example.com"));
		assertNotNull(CushtyApiClient.parseServerUrl("  https://cushty.example.com/  "));
		assertNotNull(CushtyApiClient.parseServerUrl("http://localhost:8080"));
		assertNotNull(CushtyApiClient.parseServerUrl("http://127.0.0.1:8080"));
		assertNull(CushtyApiClient.parseServerUrl("http://cushty.example.com"));
		assertNull(CushtyApiClient.parseServerUrl("not a url"));
		assertNull(CushtyApiClient.parseServerUrl(""));
		assertNull(CushtyApiClient.parseServerUrl(null));
	}

	@Test
	public void serialisesPayloadMatchingProtocol()
	{
		EventPayload payload = EventPayload.builder()
			.type(EventPayload.TYPE_LOOT)
			.rsn("Zezima")
			.loot(new EventPayload.Loot("Vorkath", List.of(new EventPayload.LootItem(22006, "Skeletal visage", 1, 30_000_000L)), 30_250_000L))
			.build();

		// A plain Gson here only checks the JSON shape. The plugin injects RuneLite's Gson.
		assertEquals(
			"{\"type\":\"loot\",\"rsn\":\"Zezima\",\"loot\":{\"source\":\"Vorkath\",\"items\":[{\"id\":22006,\"name\":\"Skeletal visage\",\"quantity\":1,\"priceEach\":30000000}],\"totalValue\":30250000}}",
			new Gson().toJson(payload));
	}
}
