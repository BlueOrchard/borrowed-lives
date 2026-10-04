# Borrowed Lives

A Minecraft mod for NeoForge 1.21.1 that gives every player a limited number
of lives. Lose them all and you are out, until your teammates carry your soul
to an altar and pay whatever it demands.

It is built for co-op play: death matters and every fight is tense, but unlike
Hardcore mode a lost player can always be brought back by friends willing to
go on a quest for them.

## How it works

**Lives.** Each player starts with 3 lives, shown in a heart beside the
hotbar. Every death costs one, whatever the cause.

**Final death.** When the last life goes, the player becomes a spectator and
their soul drops where they died. The soul cannot be destroyed by fire, lava,
explosions or cactus, and it glows through walls, but like any dropped item it
despawns after five minutes. If nobody reaches it in time, that player stays
dead.

**The altar.** Ruined stone shrines generate across the Overworld, each with
an Altar of Souls at its centre. Craft an Altar Compass (a compass and a
diamond) and use it to lock on to the nearest one.

**The price.** Place a soul on an altar to learn what it demands: one kind of
item and an amount, rolled at random when the player died. It might be 64
cobblestone, or it might be a nether star. Every price is a vanilla item from
the Overworld or the Nether. Pay it in full, over as many trips as it takes,
and the player returns at the altar with one life.

**Heart of Life.** A living player can win back one lost life, up to the
maximum, with a Heart of Life: a totem of undying in the centre of the grid, a
netherite ingot above it and diamonds in the other seven slots.

## Using the altar

| Action | Result |
| --- | --- |
| Right-click with a soul | Places it and reveals the price |
| Right-click with the demanded item | Pays towards the price |
| Right-click with an empty hand | Shows what is still owed |
| Sneak and right-click with an empty hand | Takes the soul back, keeping what was paid |

## Commands

Operators only.

| Command | Effect |
| --- | --- |
| `/lives get <player>` | Shows a player's lives |
| `/lives set <player> <n>` | Sets a player's lives |
| `/lives add <player> <n>` | Adds lives, or removes them with a negative number |
| `/lives revive <player>` | Brings back a dead player where they are |

## Configuration

Settings are per world, in `serverconfig/borrowedlives-server.toml`.

| Key | Default | Meaning |
| --- | --- | --- |
| `maxLives` | 3 | Starting and maximum lives per player |
| `livesOnRevive` | 1 | Lives a player has after an altar revive |

The list of possible revive prices is the loot table
`borrowedlives:revive_requirement`, so a datapack can replace it.

The mod does nothing in vanilla Hardcore worlds.

## Installing

Borrowed Lives is needed on both the server and every client. Put the jar in
the `mods` folder of a NeoForge 1.21.1 installation.

## Building from source

Gradle needs JDK 17 or newer to run and downloads the Java 21 toolchain it
compiles with.

```bash
./gradlew build
```

The jar is written to `build/libs`. To start the game with the mod loaded, or
to run the automated tests of the rules:

```bash
./gradlew runClient
```

```bash
./gradlew runGameTestServer
```

The full design is in [SPEC.md](SPEC.md).

## License

[MIT](LICENSE)
