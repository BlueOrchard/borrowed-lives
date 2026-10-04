package com.borrowedlives.registry;

import java.util.function.Supplier;

import com.borrowedlives.BorrowedLives;
import com.borrowedlives.altar.AltarBlock;
import com.borrowedlives.altar.AltarBlockEntity;
import com.borrowedlives.item.AltarCompassItem;
import com.borrowedlives.item.LifeHeartItem;
import com.borrowedlives.soul.SoulData;
import com.borrowedlives.soul.SoulItem;
import com.borrowedlives.worldgen.AltarPiece;
import com.borrowedlives.worldgen.AltarStructure;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistries {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BorrowedLives.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BorrowedLives.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BorrowedLives.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BorrowedLives.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, BorrowedLives.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, BorrowedLives.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SoulData>> SOUL_DATA =
            COMPONENTS.registerComponentType("soul", builder -> builder
                    .persistent(SoulData.CODEC)
                    .networkSynchronized(SoulData.STREAM_CODEC));

    // Unbreakable in survival and immune to explosions, so an altar can never be moved or destroyed.
    public static final DeferredBlock<AltarBlock> ALTAR = BLOCKS.registerBlock("altar", AltarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()
                    .lightLevel(state -> 7)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> ALTAR_ITEM = ITEMS.registerSimpleBlockItem("altar", ALTAR);

    public static final Supplier<BlockEntityType<AltarBlockEntity>> ALTAR_BLOCK_ENTITY = BLOCK_ENTITIES.register("altar",
            () -> BlockEntityType.Builder.of(AltarBlockEntity::new, ALTAR.get()).build(null));

    public static final DeferredItem<SoulItem> SOUL = ITEMS.registerItem("soul", SoulItem::new,
            new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    public static final DeferredItem<LifeHeartItem> LIFE_HEART = ITEMS.registerItem("life_heart", LifeHeartItem::new,
            new Item.Properties().stacksTo(16).fireResistant().rarity(Rarity.RARE));
    public static final DeferredItem<AltarCompassItem> ALTAR_COMPASS = ITEMS.registerItem("altar_compass",
            AltarCompassItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<StructureType<?>, StructureType<AltarStructure>> ALTAR_STRUCTURE =
            STRUCTURE_TYPES.register("altar", () -> () -> AltarStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ALTAR_PIECE =
            STRUCTURE_PIECES.register("altar", () -> (StructurePieceType.ContextlessType) AltarPiece::new);

    private ModRegistries() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        COMPONENTS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        modEventBus.addListener(ModRegistries::addCreative);
    }

    // The soul is left out: it only makes sense when it belongs to a dead player.
    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(LIFE_HEART);
            event.accept(ALTAR_COMPASS);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ALTAR_ITEM);
        }
    }
}
