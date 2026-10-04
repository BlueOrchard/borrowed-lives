package com.borrowedlives.worldgen;

import com.borrowedlives.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;

/**
 * The shrine itself: a worn 9x9 stone brick floor, a raised 5x5 dais with the altar on it, and
 * four broken pillars at the corners of the dais.
 */
public class AltarPiece extends ScatteredFeaturePiece {
    private static final int SIZE = 9;
    private static final int HEIGHT = 7;
    private static final int CENTRE = SIZE / 2;

    public AltarPiece(RandomSource random, int x, int z) {
        super(ModRegistries.ALTAR_PIECE.get(), x, 64, z, SIZE, HEIGHT, SIZE, getRandomHorizontalDirection(random));
    }

    public AltarPiece(CompoundTag tag) {
        super(ModRegistries.ALTAR_PIECE.get(), tag);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Sink by one so local y=0 replaces the top layer of ground instead of floating above it.
        if (!this.updateAverageGroundHeight(level, box, -1)) {
            return;
        }

        this.generateAirBox(level, box, 0, 1, 0, SIZE - 1, HEIGHT - 1, SIZE - 1);

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                boolean edge = x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1;
                // Leave gaps in the outer ring so the floor looks worn away.
                if (edge && random.nextInt(4) == 0) {
                    continue;
                }
                this.placeBlock(level, brick(random), x, 0, z, box);
                this.fillColumnDown(level, Blocks.COBBLESTONE.defaultBlockState(), x, -1, z, box);
            }
        }

        for (int x = CENTRE - 2; x <= CENTRE + 2; x++) {
            for (int z = CENTRE - 2; z <= CENTRE + 2; z++) {
                this.placeBlock(level, brick(random), x, 1, z, box);
            }
        }
        this.placeBlock(level, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), CENTRE, 1, CENTRE, box);
        this.placeBlock(level, ModRegistries.ALTAR.get().defaultBlockState(), CENTRE, 2, CENTRE, box);

        for (int x : new int[] {CENTRE - 2, CENTRE + 2}) {
            for (int z : new int[] {CENTRE - 2, CENTRE + 2}) {
                pillar(level, random, box, x, z);
            }
        }
    }

    private void pillar(WorldGenLevel level, RandomSource random, BoundingBox box, int x, int z) {
        int top = 2 + random.nextInt(3);
        for (int y = 2; y <= top; y++) {
            this.placeBlock(level, brick(random), x, y, z, box);
        }
        // Only the pillars still standing at full height keep their lantern.
        if (top == 4) {
            this.placeBlock(level, Blocks.SOUL_LANTERN.defaultBlockState(), x, top + 1, z, box);
        }
    }

    private static BlockState brick(RandomSource random) {
        int roll = random.nextInt(10);
        if (roll < 5) {
            return Blocks.STONE_BRICKS.defaultBlockState();
        }
        return roll < 8 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    }
}
