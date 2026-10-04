package com.borrowedlives;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_LIVES = BUILDER
            .comment("Starting and maximum lives per player. Changing this only affects players who join afterwards.")
            .defineInRange("maxLives", 3, 1, 99);

    public static final ModConfigSpec.IntValue LIVES_ON_REVIVE = BUILDER
            .comment("Lives a player has after being revived at an altar. Capped at maxLives.")
            .defineInRange("livesOnRevive", 1, 1, 99);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int maxLives() {
        return MAX_LIVES.getAsInt();
    }

    public static int livesOnRevive() {
        return Math.min(LIVES_ON_REVIVE.getAsInt(), maxLives());
    }
}
