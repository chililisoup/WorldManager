package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.message.api.LocalizedMessage;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;

import static me.drex.worldmanager.command.WorldManagerCommand.CUSTOM_WORLD_SUGGESTIONS;
import static me.drex.worldmanager.command.WorldManagerCommand.UNKNOWN_WORLD;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DeleteCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("delete")
            .requires(Permissions.require("worldmanager.command.worldmanager.delete", 2))
            .then(
                argument("id", ResourceLocationArgument.id())
                    .suggests(CUSTOM_WORLD_SUGGESTIONS)
                    .executes(context -> delete(context.getSource(), ResourceLocationArgument.getId(context, "id")))
            );
    }

    public static int delete(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(source.getServer());
        boolean success = savedData.removeWorld(id);

        if (!success) {
            throw UNKNOWN_WORLD.create();
        }
        source.sendSuccess(() -> LocalizedMessage.builder("worldmanager.command.delete").addPlaceholder("id", id.toString()).build(), false);
        return 1;
    }
}
