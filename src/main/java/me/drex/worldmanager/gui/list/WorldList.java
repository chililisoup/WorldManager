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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

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
        return new GuiElementBuilder(toIcon(entry.getValue().generator))
            .setName(Component.literal(entry.getKey().toString()));
    }

    public static Item toIcon(ChunkGenerator chunkGenerator) {
        if (chunkGenerator instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator) {
            return noiseBasedChunkGenerator.generatorSettings().value().defaultBlock().getBlock().asItem();
        } else if (chunkGenerator instanceof VoidChunkGenerator) {
            return Items.STRUCTURE_VOID;
        } else if (chunkGenerator instanceof DebugLevelSource) {
            return Items.COMMAND_BLOCK;
        }
        return Items.STONE;
    }
}
