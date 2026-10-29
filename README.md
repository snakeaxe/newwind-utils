# Newwind Utils

A client-side Fabric mod for tracking Newwind combat effects with customizable screen HUDs and above-nametag timers.

## Download and install

Download `newwindutils-1.0.3.jar` from this repository's **Releases** section. Close Minecraft, put the JAR in your launcher instance's `mods` folder, and keep only one Newwind Utils JAR there.

Requirements:
- Minecraft Java Edition **26.3**
- Fabric Loader **0.20.1 or newer**
- Fabric API for Minecraft 26.3 (built against **0.165.0+26.3**)
- Java **25**

This is a client mod. Install it in your own Minecraft client.

## Features

- Separate **Bind**, **Elytra**, **Food**, and **Gapple** timers.
- Easter leggings start or refresh both Bind and Elytra timers. Elytra includes what players call a clip.
- All-food binds still include gaps; food and gapple durations are tracked independently.
- Choose screen cards or timer text above the tracked player's nametag.
- Screen HUD customization: position, size, player heads, palette, outlines, text shadows, bars, and seconds.
- Nametag customization: size, height, stacked/inline layout, spacing, precision, preset/custom RGB colors, time colors, bold, shadow, and background opacity.
- Raise nametag timers to clear cosmetic titles.
- Live sample previews, optional animation, and a full-screen view for checking screen-panel positions without starting a fight.
- A separate self HUD, placed in the top-right by default.
- Register items in-game and edit, disable, or delete saved rules.
- Record a manual effect duration from an incoming player hit.
- Display server-reported cooldowns on your own items and reuse recent duration samples.
- Configure target count, range, focus, and ownership filters.

## Controls

| Key | Action |
| --- | --- |
| **U** | Open settings |
| **K** | Register the item in your main hand |
| **L** | Arm, cancel, or finish a received-effect measurement |
| **J** | Inspect held-item custom model data |

Change conflicting bindings in Minecraft's key-binding menu under **NewwindUTILS**.

## Quick start

Press **U**. The first control selects Screen HUD or Above nametag. Only appearance controls for the selected mode are shown. Drag sliders, or double-click them to enter exact values. The preview uses sample timers and does not change your real tracked effects. Use **Full screen** to check configured screen positions. Nametag preview scale is illustrative; actual world size varies with distance.

To add an item, hold it and press **K**. Choose its effect, source, trigger, and duration, then save. Armor rules support chest armor and leggings. Triggers are melee hits or right-clicking a player. An optional lore line can distinguish variants sharing an item group.

To measure an effect, press **L** before a player hits you and press **L** again when it ends. Under **Custom Items / Measurements**, choose a candidate from the last incoming hit, then use **Recent** to select the measurement. Additional hits may refresh an effect and are flagged in the sample.

Read [QUICK-START.txt](QUICK-START.txt) for the complete instructions.

## Tracking accuracy

Opponent timers are estimates based on visible triggers and configured durations. The client cannot query an opponent's private cooldowns or prove that a random/chance-based effect activated.

A standard cooldown packet gives a duration for your own affected item/group. It does not identify the weapon or armor responsible, and ordinary item reuse cooldowns may also appear. These self-HUD entries are labelled **Server:**.

Stopwatch measurements include reaction time. The duration of an effect on you can differ from the opponent's ability recharge time. Use isolated tests to confirm new item rules.

## Reporting issues

Open an issue with your Minecraft/Fabric versions, mod version, reproduction steps, and a screenshot or relevant error text. For timer reports, identify the item and effect and explain whether the duration was configured, measured manually, or reported by the server.

## Building

Use JDK 25. In the project folder run:

```powershell
.\gradlew.bat build
```

Build output is written to `build/libs/`.

The supplied 1.0.3 JAR was compiled directly with Java 25 against the existing Minecraft/Fabric libraries and packaged with the project resources. Compilation and behavior checks passed. The local Gradle Minecraft-preparation issue prevented a full Gradle build in the preparation environment; visual behavior still needs player testing.

## License

The project retains its existing **CC0-1.0** license. See [LICENSE](LICENSE).
