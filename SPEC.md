# Borrowed Lives: spec

A mod that gives each player a limited number of lives. The aim is team-based
co-op play where every moment can be tense, without being as punishing as
Hardcore mode: death is costly, but teammates can bring you back.

**Platform:** Minecraft 1.21.1, NeoForge, required on both server and client.

## Lives

- Players start with 3 lives (server config `maxLives`).
- Every death costs one life, whatever the cause.
- Lives are stored server-side per player, per world, and synced to the client
  for the HUD.
- Changing `maxLives` later only affects new players; existing counts stay.
- The mod is inactive in vanilla Hardcore worlds.

## Life item

- Craftable consumable, the Heart of Life. Recipe: a totem of undying in the
  centre, a netherite ingot above it and diamonds in the other seven slots.
- Restores one life to a living player, never above `maxLives`.
- Cannot be used at full lives and has no effect on dead players.
- No cooldown and no cap: the recipe cost is the only limit on stockpiling.

## Final death and the soul

- At 0 lives the player is put into vanilla free-fly spectator mode. This
  persists across relogs and restarts.
- Their items drop as normal and a server-wide message announces it.
- They drop a Soul item, named for and bound to them, at the death spot.
- The soul is immune to fire, lava, explosions and cactus, and floats on lava
  like netherite gear.
- The dropped soul glows through walls so teammates can find it.
- A newly eliminated player respawns as a spectator at the spot where they died.
- The soul despawns after the vanilla 5 minutes on the ground. Picking it up
  and dropping it again restarts the timer, as with any item.
- Souls can be carried, stored in chests and passed between players.
- A soul lost to despawning or the void means that player stays dead, unless
  an op uses `/lives revive`.

Accepted consequences:

- Item timers only run while the chunk is loaded, so a soul in an unloaded
  chunk is frozen until someone returns.
- A final death in the void destroys the soul immediately.
- In singleplayer nobody can carry the soul, so final death is permanent
  unless cheats are on.

## Altar and revival

- Altars are rare ruined stone shrines generated in the Overworld, with an
  altar block in the centre.
- The altar block cannot be crafted and is unbreakable in survival.
- The Altar Compass (a compass plus one diamond) locks on to the nearest altar
  when used, and can be used again to search from a new position.
- Placing a soul on the altar reveals its revive requirement: one item type
  and an amount.
- The requirement comes from a curated weighted table in tiers, from common
  (64 cobblestone) to very rare (a nether star).
  - Only vanilla items obtainable in survival from the Overworld or Nether.
  - Nothing from the End, and nothing unobtainable.
  - It is the loot table `borrowedlives:revive_requirement`, so a datapack can
    replace it.
- The requirement is rolled once when the player dies and stored on the soul,
  so it is the same at every altar and cannot be rerolled.
- A player who is revived and later dies again gets a fresh roll.
- The soul sits visibly on the altar. Players right-click to deposit the
  required items, over several trips if needed.
- Right-clicking with an empty hand shows what is still owed. Sneaking while
  doing so takes the soul back, keeping its progress.
- A soul left over from an earlier death is refused by the altar.
- When the requirement is met, the player revives at the altar in survival
  with 1 life (config `livesOnRevive`).
- If the dead player is offline, they revive at the altar on their next login.

## HUD

- A pixel-art heart with the remaining lives centred in it, just left of the
  hotbar.
- Shifts further left when the offhand slot is showing there.
- Hidden in spectator mode.
- The number is green at 3 lives, yellow at 2 and red at 1.

## Admin commands (ops only)

- `/lives get <player>`
- `/lives set <player> <n>`
- `/lives add <player> <n>`
- `/lives revive <player>`

## Config

| Key | Default | Meaning |
| --- | --- | --- |
| `maxLives` | 3 | Starting and maximum lives per player |
| `livesOnRevive` | 1 | Lives a player has after an altar revive |

## Development

- Mod id `borrowedlives`, package `com.borrowedlives`.
- Build with `gradlew build`; the jar lands in `build/libs`.
- `gradlew runGameTestServer` runs the automated checks of these rules.
- `gradlew runClient` starts the game with the mod for manual testing.
- Gradle must run on JDK 17 or newer; it fetches the Java 21 toolchain itself.

## Build order

1. Project scaffold, lives system, config, commands and HUD.
2. Final death, spectator mode and the soul item.
3. Life item.
4. Altar block, requirement table and revive flow.
5. Altar structure worldgen and the Altar Compass.
