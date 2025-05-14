package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

import static me.drex.message.api.LocalizedMessage.builder;
import static me.drex.worldmanager.command.WorldManagerCommand.UNKNOWN_WORLD;
import static me.drex.worldmanager.command.WorldManagerCommand.WORLD_SUGGESTIONS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TeleportCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("tp")
            .requires(Permissions.require("worldmanager.command.worldmanager.teleport", 2))
            .then(
                argument("id", ResourceLocationArgument.id())
                    .suggests(WORLD_SUGGESTIONS)
                    .executes(context -> teleport(context.getSource(), ResourceLocationArgument.getId(context, "id")))
            );
    }

    public static int teleport(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel serverLevel = source.getServer().getLevel(resourceKey);
        if (serverLevel == null) {
            throw UNKNOWN_WORLD.create();
        }

        TeleportTransition teleportTransition = TeleportTransition.missingRespawnBlock(serverLevel, player, TeleportTransition.DO_NOTHING);
        player.teleport(teleportTransition);
        source.sendSuccess(() -> builder("worldmanager.command.teleport").addPlaceholder("id", id.toString()).build(), false);
        return 1;
    }
}
