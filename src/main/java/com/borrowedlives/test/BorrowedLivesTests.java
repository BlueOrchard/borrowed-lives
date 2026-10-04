package com.borrowedlives.test;

import java.util.List;

import com.borrowedlives.Config;
import com.borrowedlives.BorrowedLives;
import com.borrowedlives.altar.AltarBlockEntity;
import com.borrowedlives.item.AltarCompassItem;
import com.borrowedlives.lives.LivesManager;
import com.borrowedlives.registry.ModRegistries;
import com.borrowedlives.soul.Requirement;
import com.borrowedlives.soul.SoulData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Server-side checks of the rules in SPEC.md. Run with the gameTestServer Gradle run. */
@GameTestHolder(BorrowedLives.MODID)
@PrefixGameTestTemplate(false)
public class BorrowedLivesTests {
    private static final BlockPos CENTRE = new BlockPos(2, 1, 2);

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(CENTRE.getCenter()));
        return player;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static ItemEntity findSoul(ServerPlayer owner) {
        List<ItemEntity> souls = owner.serverLevel().getEntitiesOfClass(ItemEntity.class,
                owner.getBoundingBox().inflate(4), entity -> {
                    SoulData data = entity.getItem().get(ModRegistries.SOUL_DATA);
                    return data != null && data.owner().equals(owner.getUUID());
                });
        return souls.isEmpty() ? null : souls.get(0);
    }

    /** Eliminates the player and returns the soul they dropped, picked up off the ground. */
    private static ItemStack eliminate(GameTestHelper helper, ServerPlayer player) {
        LivesManager.setLives(player, 0);
        ItemEntity entity = findSoul(player);
        check(helper, entity != null, "No soul was dropped on elimination");
        ItemStack soul = entity.getItem().copy();
        entity.discard();
        return soul;
    }

    private static AltarBlockEntity altar(GameTestHelper helper) {
        helper.setBlock(CENTRE, ModRegistries.ALTAR.get());
        return (AltarBlockEntity) helper.getBlockEntity(CENTRE);
    }

    @GameTest(template = "empty")
    public static void newPlayerStartsWithMaxLives(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        check(helper, LivesManager.getLives(player) == Config.maxLives(), "New player should start at maxLives");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void realDeathCostsOneLife(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.kill();
        check(helper, player.isDeadOrDying(), "Player should have died");
        check(helper, LivesManager.getLives(player) == Config.maxLives() - 1, "A death should cost exactly one life");
        check(helper, findSoul(player) == null, "No soul should drop while lives remain");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lastLifeDropsSoulAndSpectates(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        LivesManager.setLives(player, 1);
        LivesManager.onDeath(player);

        check(helper, LivesManager.getLives(player) == 0, "Lives should be 0");
        check(helper, player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR, "Eliminated player should spectate");
        ItemEntity soul = findSoul(player);
        check(helper, soul != null, "A soul should drop on the final death");
        SoulData data = soul.getItem().get(ModRegistries.SOUL_DATA);
        check(helper, !data.revealed() && data.deposited() == 0, "A fresh soul should be unrevealed and unpaid");
        check(helper, LivesManager.isSoulCurrent(player.server, data), "A fresh soul should be current");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soulCannotBeDamaged(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        LivesManager.setLives(player, 0);
        ItemEntity soul = findSoul(player);
        ServerLevel level = helper.getLevel();

        check(helper, soul.fireImmune(), "Soul should be immune to fire and lava");
        soul.hurt(level.damageSources().explosion(null, null), 100.0F);
        soul.hurt(level.damageSources().cactus(), 100.0F);
        soul.hurt(level.damageSources().lava(), 100.0F);
        check(helper, soul.isAlive(), "Soul should survive explosions, cactus and lava");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void altarRevivesWhenRequirementIsPaid(GameTestHelper helper) {
        ServerPlayer dead = player(helper);
        ServerPlayer friend = player(helper);
        ItemStack soul = eliminate(helper, dead);
        AltarBlockEntity altar = altar(helper);

        altar.useItem(friend, soul);
        check(helper, soul.isEmpty(), "Placing the soul should take it from the hand");
        SoulData placed = altar.getSoul().get(ModRegistries.SOUL_DATA);
        check(helper, placed != null && placed.revealed(), "The altar should hold the soul and reveal its price");

        // Pay in two parts to cover partial offerings, when the price is more than one item.
        Requirement requirement = placed.requirement();
        int first = requirement.count() / 2;
        if (first > 0) {
            ItemStack part = new ItemStack(requirement.item(), first);
            altar.useItem(friend, part);
            check(helper, part.isEmpty(), "A partial offering should be consumed");
            check(helper, altar.getSoul().get(ModRegistries.SOUL_DATA).deposited() == first, "Progress should be recorded");
            check(helper, LivesManager.getLives(dead) == 0, "Player should stay dead until fully paid");
        }
        // Offer more than is owed: only the remainder should be taken.
        ItemStack rest = new ItemStack(requirement.item(), requirement.count() - first);
        rest.grow(requirement.item().getDefaultMaxStackSize() > rest.getCount() ? 1 : 0);
        int offered = rest.getCount();
        altar.useItem(friend, rest);

        check(helper, rest.getCount() == offered - (requirement.count() - first), "Only what is owed should be taken");
        check(helper, altar.getSoul().isEmpty(), "The soul should be consumed");
        check(helper, LivesManager.getLives(dead) == Config.livesOnRevive(), "Player should have livesOnRevive lives");
        check(helper, dead.gameMode.getGameModeForPlayer() != GameType.SPECTATOR, "Revived player should leave spectator");
        check(helper, dead.blockPosition().distManhattan(helper.absolutePos(CENTRE)) <= 2, "Revived player should be at the altar");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wrongItemIsNotTaken(GameTestHelper helper) {
        ServerPlayer dead = player(helper);
        ServerPlayer friend = player(helper);
        AltarBlockEntity altar = altar(helper);
        altar.useItem(friend, eliminate(helper, dead));

        Requirement requirement = altar.getSoul().get(ModRegistries.SOUL_DATA).requirement();
        ItemStack wrong = new ItemStack(requirement.item() == net.minecraft.world.item.Items.STICK
                ? net.minecraft.world.item.Items.FEATHER : net.minecraft.world.item.Items.STICK, 8);
        altar.useItem(friend, wrong);
        check(helper, wrong.getCount() == 8, "An item the altar did not ask for should not be taken");
        check(helper, LivesManager.getLives(dead) == 0, "Player should still be dead");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soulCanBeTakenBackWithProgress(GameTestHelper helper) {
        ServerPlayer dead = player(helper);
        ServerPlayer friend = player(helper);
        AltarBlockEntity altar = altar(helper);
        altar.useItem(friend, eliminate(helper, dead));

        friend.setShiftKeyDown(true);
        altar.useEmptyHanded(friend);
        check(helper, altar.getSoul().isEmpty(), "Sneak-use should take the soul off the altar");
        ItemStack returned = friend.getInventory().items.stream()
                .filter(stack -> stack.has(ModRegistries.SOUL_DATA)).findFirst().orElse(ItemStack.EMPTY);
        check(helper, !returned.isEmpty(), "The soul should go to the player's inventory");
        check(helper, returned.get(ModRegistries.SOUL_DATA).revealed(), "The soul should stay revealed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void staleSoulIsRejected(GameTestHelper helper) {
        ServerPlayer dead = player(helper);
        ServerPlayer friend = player(helper);
        ItemStack oldSoul = eliminate(helper, dead);
        LivesManager.revive(dead);
        AltarBlockEntity altar = altar(helper);

        altar.useItem(friend, oldSoul);
        check(helper, altar.getSoul().isEmpty() && !oldSoul.isEmpty(), "A soul for a living player should be refused");

        // Dying again makes a new soul; the old one must stay worthless.
        ItemStack newSoul = eliminate(helper, dead);
        altar.useItem(friend, oldSoul);
        check(helper, altar.getSoul().isEmpty(), "A soul from an earlier death should be refused");
        altar.useItem(friend, newSoul);
        check(helper, !altar.getSoul().isEmpty(), "The current soul should be accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lifeHeartRestoresOneLifeUpToMax(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ItemStack hearts = new ItemStack(ModRegistries.LIFE_HEART.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, hearts);

        hearts.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        check(helper, hearts.getCount() == 2, "A heart should not be used up at full lives");

        LivesManager.setLives(player, Config.maxLives() - 1);
        hearts.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        check(helper, LivesManager.getLives(player) == Config.maxLives(), "A heart should restore one life");
        check(helper, hearts.getCount() == 1, "Using a heart should consume it");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shrineGeneratesWithAltar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(CENTRE);
        level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(),
                "place structure borrowedlives:altar " + origin.getX() + " " + origin.getY() + " " + origin.getZ());

        // The shrine fills part of the chunk it was placed in, at the local ground height.
        ChunkPos chunk = new ChunkPos(origin);
        boolean found = BlockPos.betweenClosedStream(chunk.getMinBlockX(), origin.getY() - 12, chunk.getMinBlockZ(),
                chunk.getMaxBlockX(), origin.getY() + 12, chunk.getMaxBlockZ())
                .anyMatch(pos -> level.getBlockState(pos).is(ModRegistries.ALTAR.get())
                        && level.getBlockEntity(pos) instanceof AltarBlockEntity
                        && level.getBlockState(pos.below()).is(Blocks.CHISELED_STONE_BRICKS));
        check(helper, found, "Placing the structure should build a shrine with a working altar");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dataFilesLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        check(helper, level.getServer().reloadableRegistries().getLootTable(Requirement.TABLE) != LootTable.EMPTY,
                "The revive requirement table should load");
        for (String recipe : List.of("life_heart", "altar_compass")) {
            check(helper, level.getRecipeManager().byKey(BorrowedLives.id(recipe)).isPresent(), "Missing recipe: " + recipe);
        }
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        check(helper, structures.containsKey(ResourceKey.create(Registries.STRUCTURE, BorrowedLives.id("altar"))),
                "The altar structure should be registered");
        check(helper, structures.getTag(AltarCompassItem.ALTARS).map(tag -> tag.size() == 1).orElse(false),
                "The altar structure tag should contain the altar");
        helper.succeed();
    }
}
