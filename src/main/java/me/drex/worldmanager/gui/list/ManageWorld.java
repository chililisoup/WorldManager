package me.drex.worldmanager.gui.list;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.message.api.LocalizedMessage;
import me.drex.worldmanager.command.SpawnCommand;
import me.drex.worldmanager.command.TeleportCommand;
import me.drex.worldmanager.gui.util.ConfirmTypeGui;
import me.drex.worldmanager.gui.util.GuiElements;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;

import static me.drex.message.api.LocalizedMessage.builder;

public class ManageWorld extends SimpleGui {
    private final ResourceLocation id;
    private final WorldConfig config;
    private final SimpleGui previousGui;

    public ManageWorld(ServerPlayer player, ResourceLocation id, WorldConfig config, SimpleGui previousGui) {
        super(MenuType.GENERIC_9x3, player, false);
        this.id = id;
        this.config = config;
        this.previousGui = previousGui;
        setTitle(builder("worldmanager.gui.manage_world.title").addPlaceholder("id", id.toString()).build());
        build();
    }

    public void build() {
        setSlot(0,
            guiElementBuilder(Items.ENDER_PEARL, "teleport")
                .setCallback(() -> {
                    if (Permissions.check(player, "worldmanager.command.worldmanager.teleport", 2)) {
                        if (TeleportCommand.teleport(player, config, id)) {
                            close();
                        }
                    }
                })
        );
        setSlot(1,
            guiElementBuilder(Items.GRASS_BLOCK, "spawn")
                .setCallback(() -> {
                    if (Permissions.check(player, "worldmanager.command.worldmanager.spawn", 2)) {
                        if (SpawnCommand.spawn(player, config, id)) {
                            close();
                        }
                    }
                })
        );
        setSlot(2,
            guiElementBuilder(config.data.iconGuiElement(), "icon")
                .setCallback(() -> {
                    if (Permissions.check(player, "worldmanager.command.worldmanager.seticon", 2)) {
                        ItemStack item = player.getMainHandItem();
                        if (!item.isEmpty()) {
                            config.data.icon = item;
                        }
                        WorldManagerSavedData.getSavedData(player.getServer()).setDirty();
                        build();
                    }
                })
        );
        setSlot(3,
            guiElementBuilder(Items.CLOCK, "daylight_cycle")
                .setCallback(() -> toggleGameRule(GameRules.RULE_DAYLIGHT))
        );
        setSlot(4,
            guiElementBuilder(config.data.rules.getBoolean(GameRules.RULE_WEATHER_CYCLE) ? Items.WATER_BUCKET : Items.BUCKET, "weather_cycle")
                .setCallback(() -> toggleGameRule(GameRules.RULE_WEATHER_CYCLE))
        );
        setSlot(5,
            guiElementBuilder(Items.ZOMBIE_SPAWN_EGG, "mob_spawning")
                .setCallback(() -> toggleGameRule(GameRules.RULE_DOMOBSPAWNING))
        );
        setSlot(6,
            guiElementBuilder(Items.CREEPER_HEAD, "mob_griefing")
                .setCallback(() -> toggleGameRule(GameRules.RULE_MOBGRIEFING))
        );
        setSlot(18, GuiElements.back(previousGui));
        setSlot(26,
            guiElementBuilder(Items.TNT, "delete")
                .setCallback(() -> {
                    if (Permissions.check(player, "worldmanager.command.worldmanager.delete", 2)) {
                        new ConfirmTypeGui(player, id.toString(), () -> {
                            WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(player.getServer());
                            if (savedData.removeWorld(id)) {
                                close();
                            } else {
                                this.open();
                            }
                        }, this).open();
                    }
                })
        );
    }

    private void toggleGameRule(GameRules.Key<GameRules.BooleanValue> key) {
        if (!Permissions.check(player, "worldmanager.command.worldmanager.gamerule", 2))
            return;

        config.data.rules.toggle(key);
        MinecraftServer server = player.getServer();
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        savedData.setDirty();
        build();
    }

    private GuiElementBuilder guiElementBuilder(Item item, String id) {
        return guiElementBuilder(new GuiElementBuilder(item), id);
    }

    private GuiElementBuilder guiElementBuilder(GuiElementBuilder builder, String id) {
        return builder
            .setName(LocalizedMessage.builder("worldmanager.gui.manage_world." + id + ".name")
                .build())
            .addLoreLine(LocalizedMessage.builder("worldmanager.gui.manage_world." + id + ".lore")
                .addPlaceholder("daylight_cycle", config.data.rules.getBoolean(GameRules.RULE_DAYLIGHT))
                .addPlaceholder("weather_cycle", config.data.rules.getBoolean(GameRules.RULE_WEATHER_CYCLE))
                .addPlaceholder("mob_spawning", config.data.rules.getBoolean(GameRules.RULE_DOMOBSPAWNING))
                .addPlaceholder("mob_griefing", config.data.rules.getBoolean(GameRules.RULE_MOBGRIEFING))
                .build());
    }
}
