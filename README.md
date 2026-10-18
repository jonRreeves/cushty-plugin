# Cushty

RuneLite plugin that shares your own Old School RuneScape drops, pets, and screenshots with a clan Discord.

The plugin does not include a server, and it does not talk to Discord itself. You point it at an HTTPS server you choose, and that server posts to your Discord. You authenticate with a bearer token that server gave you. Until both are set and you turn a feature on, the plugin does not contact anything.

## What it sends

Each upload is one HTTPS request to the server URL you entered:

- A PNG of the **RuneLite game view**. It is taken from the game canvas (`DrawManager`), so it is the game picture only: not your desktop, not other windows, and not the RuneLite sidebar. Anyone standing on screen is in the shot, the same as a normal screenshot.
- Your bearer token, so the server can tell which member you are. This is a token that server issued. It is not your Jagex password, email, or session.
- Your current world, when you are logged in.
- Your RSN, when **Include RSN** is on. That option defaults to on. Turn it off and posts use your Discord name.

What else is included depends on the event:

| Event | When it sends | Extra fields |
| --- | --- | --- |
| Manual screenshot | You press the hotkey, or Send on the sidebar | Optional caption you typed (500 characters) |
| Valuable drop | A stack in the drop is worth at least your minimum (5M gp or more, Grand Exchange price) | Item id, name, quantity, GE price, and the drop source. On a player kill the source is that player's name, because that is the name on the loot event. |
| Pet | The game announces a pet, including a duplicate | Duplicate or not, and the game message |

Nothing is sent on a timer, and nothing is sent while the features are off. **Test connection** calls the server you configured and does not upload a screenshot.

## Boundaries

Gameplay input is untouched: no clicks, no chat typing, no changes to outgoing packets. The plugin does not read Jagex account credentials. It does not build a directory of other players. There is no default server, so the plugin author receives a screenshot only if you point the plugin at a server they operate.

Every feature that uploads defaults to off. Turning one on asks you to confirm this:

> This plugin submits your IP address to a 3rd party website not controlled or verified by the RuneLite Developers.

## Setup

1. Get a server URL and a bearer token from whoever runs your clan's server.
2. In RuneLite, open **Configuration → Cushty**.
3. Paste **Server URL** and **Token**.
4. Turn on the features you want and confirm the warning.
5. Open the Cushty sidebar and click **Test connection**. It should say you are connected as your Discord name.

The drop minimum cannot be set below 5,000,000 gp. Values already saved below that are raised to 5M when a drop is checked.

## Development

Needs a JDK that can compile Java 11 (11 or 17 is fine).

```bash
./gradlew test
./gradlew run
```

`run` starts RuneLite in developer mode with Cushty loaded. Jagex accounts need the [RuneLite Jagex account guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
