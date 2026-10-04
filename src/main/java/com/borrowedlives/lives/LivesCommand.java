package com.borrowedlives.lives;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.borrowedlives.Config;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /lives get|set|add|revive, for operators. */
public final class LivesCommand {
    private static final SimpleCommandExceptionType INACTIVE = new SimpleCommandExceptionType(
            Component.translatable("commands.borrowedlives.inactive"));
    private static final SimpleCommandExceptionType NOT_DEAD = new SimpleCommandExceptionType(
            Component.translatable("commands.borrowedlives.revive.not_dead"));

    private LivesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lives")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(LivesCommand::get)))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> set(ctx, false)))))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> set(ctx, true)))))
                .then(Commands.literal("revive")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(LivesCommand::revive))));
    }

    private static ServerPlayer target(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!LivesManager.isActive(ctx.getSource().getServer())) {
            throw INACTIVE.create();
        }
        return EntityArgument.getPlayer(ctx, "player");
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = target(ctx);
        int lives = LivesManager.getLives(player);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.borrowedlives.get", player.getDisplayName(), lives, Config.maxLives()), false);
        return lives;
    }

    private static int set(CommandContext<CommandSourceStack> ctx, boolean relative) throws CommandSyntaxException {
        ServerPlayer player = target(ctx);
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        LivesManager.setLives(player, relative ? LivesManager.getLives(player) + amount : amount);
        int lives = LivesManager.getLives(player);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.borrowedlives.set", player.getDisplayName(), lives, Config.maxLives()), true);
        return lives;
    }

    private static int revive(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = target(ctx);
        if (LivesManager.getLives(player) > 0) {
            throw NOT_DEAD.create();
        }
        LivesManager.revive(player);
        int lives = LivesManager.getLives(player);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.borrowedlives.set", player.getDisplayName(), lives, Config.maxLives()), true);
        return lives;
    }
}
