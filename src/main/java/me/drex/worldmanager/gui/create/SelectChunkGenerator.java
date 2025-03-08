package me.drex.worldmanager.gui.create;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.message.impl.util.ComponentUtil;
import me.drex.worldmanager.gui.util.PagedGui;
import me.drex.worldmanager.save.ChunkGenerators;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static me.drex.message.api.LocalizedMessage.localized;

public class SelectChunkGenerator extends PagedGui<Map.Entry<ResourceLocation, ChunkGenerator>> {
    private final Consumer<ChunkGenerator> consumer;
    private final List<Map.Entry<ResourceLocation, ChunkGenerator>> elements;

    public SelectChunkGenerator(ServerPlayer player, SimpleGui previousGui, Consumer<ChunkGenerator> consumer) {
        super(MenuType.GENERIC_9x3, player, previousGui);
        this.consumer = consumer;
        setTitle(localized("worldmanager.gui.select_generator.title"));
        this.elements = ChunkGenerators.PRESETS.entrySet().stream()
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().apply(player.registryAccess()))).toList();
        build();
    }

    @Override
    protected List<Map.Entry<ResourceLocation, ChunkGenerator>> elements() {
        return elements;
    }

    @Override
    protected GuiElementBuilder toGuiElement(Map.Entry<ResourceLocation, ChunkGenerator> entry) {
        var access = player.registryAccess();
        var chunkGenerator = entry.getValue();
        Optional<Tag> result = ChunkGenerator.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, access), chunkGenerator).result();
        Component lore = Component.empty();
        if (result.isPresent()) {
            lore = new TextComponentTagVisitor("    ").visit(result.get());
        }
        List<Component> lines = new LinkedList<>();
        ComponentUtil.parseNewLines(lore, lines);

        return new GuiElementBuilder(toIcon(chunkGenerator))
            .setName(Component.literal(entry.getKey().toString()))
            .setLore(lines)
            .setCallback(() -> {
                consumer.accept(chunkGenerator);
                previousGui.open();
            });
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
