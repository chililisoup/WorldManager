package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.message.api.LocalizedMessage;
import me.drex.worldmanager.save.Location;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

import static me.drex.worldmanager.command.WorldManagerCommand.UNKNOWN_WORLD;
import static me.drex.worldmanager.command.WorldManagerCommand.WORLD_SUGGESTIONS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SetIconCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext commandBuildContext) {
        return literal("seticon")
            .requires(Permissions.require("worldmanager.command.worldmanager.seticon", 2))
            .then(
                argument("id", ResourceLocationArgument.id())
                    .suggests(WORLD_SUGGESTIONS)
                    .then(
                        Commands.argument("icon", ItemArgument.item(commandBuildContext))
                            .executes(context -> setIcon(context.getSource(), ResourceLocationArgument.getId(context, "id"), ItemArgument.getItem(context, "icon")))
                    )
            );
    }

    public static int setIcon(CommandSourceStack source, ResourceLocation id, ItemInput itemInput) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        WorldConfig config = savedData.getConfig(id);

        if (config == null) {
            throw UNKNOWN_WORLD.create();
        }
        ItemStack icon = itemInput.createItemStack(1, false);
        config.data.icon = icon;
        savedData.setDirty();

        source.sendSuccess(() -> LocalizedMessage.builder("worldmanager.command.seticon")
            .addPlaceholder("id", id.toString())
            .addPlaceholder("icon", icon.getDisplayName())
            .build(), false);
        return 1;
    }
}
