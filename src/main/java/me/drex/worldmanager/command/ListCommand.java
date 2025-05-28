package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.drex.worldmanager.gui.list.WorldList;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public class ListCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("list")
            .requires(Permissions.require("worldmanager.command.worldmanager.list", 2))
            .executes(context -> {
                new WorldList(context.getSource().getPlayerOrException()).open();
                return 1;
            });
    }
}
