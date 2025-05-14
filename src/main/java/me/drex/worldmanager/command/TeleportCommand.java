package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.playerdata.api.PlayerDataApi;
import me.drex.worldmanager.WorldManager;
import me.drex.worldmanager.data.PlayerData;
import me.drex.worldmanager.save.Location;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

import java.util.Collection;
import java.util.List;

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
                    .executes(context -> teleport(context.getSource(), ResourceLocationArgument.getId(context, "id"), List.of(context.getSource().getPlayerOrException())))
                    .then(
                        argument("targets", EntityArgument.players())
                            .executes(context -> teleport(context.getSource(), ResourceLocationArgument.getId(context, "id"), EntityArgument.getPlayers(context, "targets")))
                    )
            );
    }

    public static int teleport(CommandSourceStack source, ResourceLocation id, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
        MinecraftServer server = source.getServer();
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        WorldConfig config = savedData.getConfig(id);
        ServerLevel serverLevel = source.getServer().getLevel(resourceKey);
        if (config == null || serverLevel == null) {
            throw UNKNOWN_WORLD.create();
        }
        for (ServerPlayer player : targets) {
            PlayerData playerData = PlayerDataApi.getCustomDataFor(player, WorldManager.STORAGE);
            TeleportTransition teleportTransition = null;
            if (playerData != null) {
                Location lastLocation = playerData.locations().get(resourceKey);
                if (lastLocation != null) {
                    teleportTransition = lastLocation.toTeleportTransition(serverLevel);
                }
            }
            if (teleportTransition == null) {
                teleportTransition = config.data.spawnLocation
                    .map(location -> location.toTeleportTransition(serverLevel))
                    .orElseGet(() -> TeleportTransition.missingRespawnBlock(serverLevel, player, TeleportTransition.DO_NOTHING));
            }
            player.teleport(teleportTransition);
        }
        source.sendSuccess(() -> builder("worldmanager.command.teleport").addPlaceholder("id", id.toString()).build(), false);
        return 1;
    }
}
