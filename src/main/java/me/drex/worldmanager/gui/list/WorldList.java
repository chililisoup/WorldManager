package me.drex.worldmanager.gui.list;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.worldmanager.gui.util.PagedGui;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

import static me.drex.message.api.LocalizedMessage.localized;

public class WorldList extends PagedGui<Map.Entry<ResourceLocation, WorldConfig>> {
    public WorldList(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player);
        setTitle(localized("worldmanager.gui.world_list.title"));
        build();
    }

    @Override
    protected List<Map.Entry<ResourceLocation, WorldConfig>> elements() {
        return WorldManagerSavedData.getSavedData(player.server).getWorlds().entrySet().stream().toList();
    }

    @Override
    protected GuiElementBuilder toGuiElement(Map.Entry<ResourceLocation, WorldConfig> entry) {
        WorldConfig config = entry.getValue();
        ItemStack icon = config.data.icon;
        GuiElementBuilder builder;
        if (icon.isEmpty()) {
            builder = new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19");
        } else {
            builder = new GuiElementBuilder(icon);
        }
        return builder.setName(Component.literal(entry.getKey().toString()));
    }

}
