package me.drex.worldmanager.gui.list;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.worldmanager.gui.create.SelectChunkGenerator;
import me.drex.worldmanager.gui.util.PagedGui;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;

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
        return new GuiElementBuilder(SelectChunkGenerator.toIcon(entry.getValue().generator()))
            .setName(Component.literal(entry.getKey().toString()));
    }
}
