package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

import static me.drex.message.api.LocalizedMessage.builder;
import static me.drex.worldmanager.command.WorldManagerCommand.UNKNOWN_WORLD;
import static me.drex.worldmanager.command.WorldManagerCommand.WORLD_SUGGESTIONS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SpawnCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("spawn")
            .requires(Permissions.require("worldmanager.command.worldmanager.spawn", 2))
            .then(
                argument("id", ResourceLocationArgument.id())
                    .suggests(WORLD_SUGGESTIONS)
                    .executes(context -> teleport(context.getSource(), ResourceLocationArgument.getId(context, "id")))
            );
    }

    public static int teleport(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
        MinecraftServer server = source.getServer();
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        WorldConfig config = savedData.getConfig(id);
        ServerLevel serverLevel = source.getServer().getLevel(resourceKey);
        if (config == null || serverLevel == null) {
            throw UNKNOWN_WORLD.create();
        }
        TeleportTransition teleportTransition = config.data.spawnLocation
            .map(location -> location.toTeleportTransition(serverLevel))
            .orElseGet(() -> TeleportTransition.missingRespawnBlock(serverLevel, player, TeleportTransition.DO_NOTHING));
        player.teleport(teleportTransition);
        source.sendSuccess(() -> builder("worldmanager.command.spawn").addPlaceholder("id", id.toString()).build(), false);
        return 1;
    }
}
