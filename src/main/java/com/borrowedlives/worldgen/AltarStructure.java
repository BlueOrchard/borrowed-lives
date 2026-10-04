package com.borrowedlives.worldgen;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.borrowedlives.registry.ModRegistries;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** A small ruined shrine on the surface with an altar at its centre. */
public class AltarStructure extends Structure {
    public static final MapCodec<AltarStructure> CODEC = simpleCodec(AltarStructure::new);

    public AltarStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> builder.addPiece(
                new AltarPiece(context.random(), context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModRegistries.ALTAR_STRUCTURE.get();
    }
}
