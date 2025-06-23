package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.drex.worldmanager.gui.CreateWorld;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import static me.drex.message.api.LocalizedMessage.builder;
import static me.drex.message.api.LocalizedMessage.localized;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CreateCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("create")
            .requires(Permissions.require("worldmanager.command.worldmanager.create", 2))
            .then(
                argument("id", ResourceLocationArgument.id())
                    .executes(context -> {
                        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
                        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
                        MinecraftServer server = context.getSource().getServer();
                        ServerLevel level = server.getLevel(resourceKey);
                        if (level != null) {
                            throw new SimpleCommandExceptionType(localized("worldmanager.command.exception.world_already_exists")).create();
                        }

                        new CreateWorld(context.getSource().getPlayerOrException(), id).open();
                        return 1;
                    }).then(
                        argument("nbt", CompoundTagArgument.compoundTag())
                            .executes(context -> {
                                ResourceLocation id = ResourceLocationArgument.getId(context, "id");
                                MinecraftServer server = context.getSource().getServer();
                                validLevelId(id, server);

                                CompoundTag tag = CompoundTagArgument.getCompoundTag(context, "nbt");
                                var config = WorldConfig.CODEC.decode(RegistryOps.create(NbtOps.INSTANCE, server.registryAccess()), tag)
                                    .getOrThrow(s -> new SimpleCommandExceptionType(Component.literal(s)).create())
                                    .getFirst();

                                Fantasy fantasy = Fantasy.get(server);

                                RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(id, config.toRuntimeWorldConfig());

                                WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
                                savedData.addWorld(id, config, handle);
                                context.getSource().sendSuccess(() -> builder("worldmanager.command.create").addPlaceholder("id", id.toString()).build(), false);
                                return 1;
                            })
                    )
            );
    }

    public static void validLevelId(ResourceLocation id, MinecraftServer server) throws CommandSyntaxException {
        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel level = server.getLevel(resourceKey);
        if (level != null) {
            throw new SimpleCommandExceptionType(localized("worldmanager.command.exception.world_already_exists")).create();
        }
    }
}
