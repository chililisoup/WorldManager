package me.drex.worldmanager.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import me.drex.message.api.LocalizedMessage;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

import static me.drex.worldmanager.command.WorldManagerCommand.CUSTOM_WORLD_SUGGESTIONS;
import static me.drex.worldmanager.command.WorldManagerCommand.UNKNOWN_WORLD;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class GameRuleCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext commandBuildContext) {
        final LiteralArgumentBuilder<CommandSourceStack> builder = literal("gamerule")
            .requires(Permissions.require("worldmanager.command.worldmanager.gamerule", 2));

        new GameRules(commandBuildContext.enabledFeatures()).visitGameRuleTypes(
            new GameRules.GameRuleTypeVisitor() {
                public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                    var valueArgument = type.createArgument("value");
                    var worldArgument = argument("id", ResourceLocationArgument.id())
                        .suggests(CUSTOM_WORLD_SUGGESTIONS);

                    Command<CommandSourceStack> getRule = context -> queryRule(context, key);
                    Command<CommandSourceStack> setRule = context -> setRule(context, key);

                    builder.then(Commands.literal(key.getId())
                        .then(valueArgument
                            .then(worldArgument.executes(setRule))
                            .executes(setRule)
                        )
                        .then(worldArgument.executes(getRule))
                        .executes(getRule)
                    );
                }
            }
        );

        return builder;
    }

    public static int setRule(CommandContext<CommandSourceStack> context, GameRules.Key<?> key) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        ResourceLocation id = getId(context);
        WorldConfig config = savedData.getConfig(id);

        if (config == null) {
            throw UNKNOWN_WORLD.create();
        }

        Pair<String, Integer> result = config.data.rules.setFromArgument(key, context, "value");

        source.sendSuccess(() -> LocalizedMessage.builder("worldmanager.command.gamerule.set")
            .addPlaceholder("id", id.toString())
            .addPlaceholder("key", key.getId())
            .addPlaceholder("value", result.getFirst())
            .build(), false);
        return result.getSecond();
    }

    public static int queryRule(CommandContext<CommandSourceStack> context, GameRules.Key<?> key) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        ResourceLocation id = getId(context);
        WorldConfig config = savedData.getConfig(id);

        if (config == null) {
            throw UNKNOWN_WORLD.create();
        }

        String result = config.data.rules.getRuleString(key);

        source.sendSuccess(() -> LocalizedMessage.builder("worldmanager.command.gamerule.query")
            .addPlaceholder("id", id.toString())
            .addPlaceholder("key", key.getId())
            .addPlaceholder("value", result)
            .build(), false);
        return 1;
    }

     public static ResourceLocation getId(CommandContext<CommandSourceStack> context) {
         CommandSourceStack source = context.getSource();
         try {
             return ResourceLocationArgument.getId(context, "id");
         } catch (IllegalArgumentException e) {
             return source.getLevel().dimension().location();
         }
     }
}
