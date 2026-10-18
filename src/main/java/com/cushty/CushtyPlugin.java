package com.cushty;

import com.cushty.api.CushtyApiClient;
import com.cushty.api.EventPayload;
import com.cushty.capture.ScreenshotCapture;
import com.cushty.loot.LootFilter;
import com.cushty.pets.PetMessages;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.http.api.loottracker.LootRecordType;
import okhttp3.HttpUrl;

@Slf4j
@PluginDescriptor(
	name = "Cushty",
	description = "Share your drops, pets, and game-view screenshots with a clan Discord you configure",
	tags = {"discord", "screenshot", "clan", "drops", "loot", "pet"}
)
public class CushtyPlugin extends Plugin
{
	/** Keep in line with the bot's MAX_UPLOAD_MB default. */
	private static final int MAX_UPLOAD_BYTES = 8 * 1024 * 1024;
	/** Wait a couple of ticks so the pet and the clan broadcast are visible in the shot. */
	private static final int PET_CAPTURE_DELAY_TICKS = 2;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CushtyConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ScreenshotCapture screenshotCapture;

	@Inject
	private CushtyApiClient api;

	private CushtyPanel panel;
	private NavigationButton navButton;
	private EventPayload pendingPet;
	private int pendingPetTicks;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.screenshotHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			sendManual(null, false);
		}
	};

	@Provides
	CushtyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CushtyConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new CushtyPanel(caption -> sendManual(caption, true), this::testConnection);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Cushty")
			.icon(icon)
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		keyManager.registerKeyListener(hotkeyListener);
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(hotkeyListener);
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		pendingPet = null;
	}

	// ---- Manual screenshots -------------------------------------------------------------------

	private void sendManual(String caption, boolean fromPanel)
	{
		if (!config.manualEnabled())
		{
			report("Turn on 'Enable manual screenshots' in the Cushty settings first.", true);
			return;
		}
		EventPayload payload = EventPayload.builder()
			.type(EventPayload.TYPE_MANUAL)
			.caption(caption == null || caption.isBlank() ? null : caption)
			.build();
		send(payload, fromPanel);
	}

	private void testConnection()
	{
		HttpUrl server = CushtyApiClient.parseServerUrl(config.serverUrl());
		String token = config.token().trim();
		if (server == null || token.isEmpty())
		{
			report("Set an HTTPS server URL and token first.", true);
			return;
		}
		api.checkConnection(server, token, listener(false));
	}

	// ---- Valuable drops -----------------------------------------------------------------------

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		handleLoot(event.getNpc().getName(), event.getItems());
	}

	@Subscribe
	public void onPlayerLootReceived(PlayerLootReceived event)
	{
		handleLoot(event.getPlayer().getName(), event.getItems());
	}

	@Subscribe
	public void onLootReceived(LootReceived event)
	{
		// NPC and player kills arrive via the events above; this covers chests, raids, pickpockets etc.
		if (event.getType() == LootRecordType.EVENT || event.getType() == LootRecordType.PICKPOCKET)
		{
			handleLoot(event.getName(), event.getItems());
		}
	}

	private void handleLoot(String source, Collection<ItemStack> items)
	{
		if (!config.lootEnabled() || items.isEmpty())
		{
			return;
		}

		Map<Integer, Integer> quantities = new LinkedHashMap<>();
		for (ItemStack stack : items)
		{
			quantities.merge(itemManager.canonicalize(stack.getId()), stack.getQuantity(), Integer::sum);
		}

		List<EventPayload.LootItem> drop = new ArrayList<>(quantities.size());
		quantities.forEach((id, quantity) -> drop.add(new EventPayload.LootItem(
			id,
			itemManager.getItemComposition(id).getName(),
			quantity,
			itemManager.getItemPrice(id))));

		LootFilter filter = new LootFilter(config.lootMinValue());
		LootFilter.Result result = filter.select(drop);
		if (result.isEmpty())
		{
			return;
		}

		String name = source == null || source.isBlank() ? "Unknown" : Text.removeTags(source);
		send(EventPayload.builder()
			.type(EventPayload.TYPE_LOOT)
			.loot(new EventPayload.Loot(name, result.getItems(), result.getTotalValue()))
			.build(), false);
	}

	// ---- Pets ---------------------------------------------------------------------------------

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.petsEnabled() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		String message = Text.removeTags(event.getMessage());
		PetMessages.Result result = PetMessages.classify(message);
		if (result == PetMessages.Result.NONE)
		{
			return;
		}
		pendingPet = EventPayload.builder()
			.type(EventPayload.TYPE_PET)
			.pet(new EventPayload.Pet(result == PetMessages.Result.DUPLICATE, message))
			.build();
		pendingPetTicks = PET_CAPTURE_DELAY_TICKS;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (pendingPet != null && --pendingPetTicks <= 0)
		{
			EventPayload pet = pendingPet;
			pendingPet = null;
			send(pet, false);
		}
	}

	// ---- Sending ------------------------------------------------------------------------------

	/**
	 * Captures the next frame and posts it. Safe to call from any thread.
	 *
	 * @param fromPanel whether the sidebar panel should show progress and clear its caption
	 */
	private void send(EventPayload payload, boolean fromPanel)
	{
		HttpUrl server = CushtyApiClient.parseServerUrl(config.serverUrl());
		String token = config.token().trim();
		if (server == null || token.isEmpty())
		{
			report("Set an HTTPS server URL and token in the Cushty settings.", true);
			return;
		}

		CushtyPanel currentPanel = panel;
		if (fromPanel && currentPanel != null)
		{
			currentPanel.setBusy(true);
		}

		clientThread.invokeLater(() ->
		{
			Player player = client.getLocalPlayer();
			if (config.includeRsn() && player != null && player.getName() != null)
			{
				payload.setRsn(Text.sanitize(player.getName()));
			}
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				payload.setWorld(client.getWorld());
			}

			CushtyApiClient.ResultListener listener = listener(fromPanel);
			screenshotCapture.captureNextFrame()
				.orTimeout(10, TimeUnit.SECONDS)
				.whenComplete((png, error) ->
				{
					if (error != null)
					{
						log.debug("Screenshot capture failed", error);
						listener.onFailure("Could not capture a screenshot");
					}
					else if (png.length > MAX_UPLOAD_BYTES)
					{
						listener.onFailure("Screenshot is too large to upload");
					}
					else
					{
						api.sendEvent(server, token, payload, png, listener);
					}
				});
		});
	}

	private CushtyApiClient.ResultListener listener(boolean fromPanel)
	{
		return new CushtyApiClient.ResultListener()
		{
			@Override
			public void onSuccess(String message)
			{
				finish(message, false);
			}

			@Override
			public void onFailure(String message)
			{
				finish(message, true);
			}

			private void finish(String message, boolean error)
			{
				CushtyPanel current = panel;
				if (fromPanel && current != null)
				{
					current.setBusy(false);
					if (!error)
					{
						current.clearCaption();
					}
				}
				report(message, error);
			}
		};
	}

	/** Shows a status message in the panel and, if enabled, in game chat. Safe from any thread. */
	private void report(String message, boolean error)
	{
		CushtyPanel current = panel;
		if (current != null)
		{
			current.setStatus(message, error);
		}
		if (!config.chatNotifications())
		{
			return;
		}
		String formatted = new ChatMessageBuilder()
			.append(ChatColorType.HIGHLIGHT)
			.append("[Cushty] ")
			.append(ChatColorType.NORMAL)
			.append(message)
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(formatted)
			.build());
	}
}
