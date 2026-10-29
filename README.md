# Newwind Utils

Client-side Fabric utilities for Newwind: independent bind timers, customizable
screen and above-nametag HUDs with live previews, custom item registration,
server cooldown observations, an offline god-item browser, material cost
calculators, and configurable ground radius guides.

Minecraft 26.3 · Java 25 · Fabric Loader 0.20.1+ · Fabric API for 26.3.

## Controls

- U: HUD/configuration and preview
- B: item browser, upgrade costs and per-item radius guides
- K: register held item / custom tracking rules
- L: arm/finish a manually confirmed timing measurement
- J: inspect held custom model data

Keys can be changed in Minecraft Controls. Existing configuration is retained.

## Data and limitations

The item catalog is a 2026-10-29 snapshot of the [Newwind wiki](https://wiki.newwindserver.com/gods).
Each entry retains its source URL. It contains functional summaries, enchantment
levels and upgrade ingredient counts, not auction histories or item poems.
Two missing detail pages are marked unavailable.

Sharpening/reinforcing prices were supplied by the player and are editable:
Sharpness 10–20: 90 embers/level; 20–30: 90 dragon eggs/level.
Unbreaking 0–10: 90 gold coins/level; 10–20: 90 star remnants/level.
Four embers equal one dragon egg; 64 eggs equal one stack.
Unspecified price ranges are labeled incomplete. Recipe quotes do not subtract
removal refunds or estimate market prices.

Effect radii are unknown by default. Enter tested values in each item's Radius
screen. Outlines are horizontal guides and do not confirm an ability's server
activation or full area of effect. Movement-item cooldowns are grouped as binds
by user request, including ordinary cooldowns on those items.

## Build

Use Java 25, then run `./gradlew build` (Windows: `.\gradlew.bat build`).
The output JAR is in `build/libs/`.
See QUICK-START.txt for installation and testing instructions.

## Version 1.1.1

The browser opens to categories in a compact centered panel. Renamed items are
recognized through original names, stable server metadata, or unique mechanic
lore. An item page can explicitly associate a held item with its catalog entry.
Saved identities do not depend on player renames or enchantment levels.

Radius rules support HELD, WORN, INVENTORY and ALWAYS feet guides. Heliast
uses landed-pool outlines instead of a feet guide: newly fired projectiles are
associated with newly spawned area-cloud entities, whose actual position and
radius are drawn independently of particle visibility. This needs verification
with Newwind; particle-only pools cannot be detected this way.

Browser icons use matching real inventory items where available, preserving
armor trims and custom models. Wiki trim definitions were not available.

## Version 1.1.2

Key detail pages have a View drops button. Drop entries open item details;
Back restores the drop list, and Back to key restores the key page. The offline
snapshot contains 410 explicit Obtaining links for 20 keys. Two keys without
published matching sources have an empty list marked unavailable. Drop odds
and undocumented loot are not invented.

Heliast outlines only pools associated with your released shot, after impact.
It accepts normal lingering-potion area clouds, follows their actual position
and radius, and clears the outline when the cloud disappears. Holding the bow
alone draws nothing. Ownership is used where available, otherwise launch and
impact proximity are used; simultaneous nearby shots can be ambiguous.
