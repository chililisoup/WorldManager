package me.drex.worldmanager.gui.configure;

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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static me.drex.message.api.LocalizedMessage.localized;

public class SelectChunkGenerator extends PagedGui<ChunkGenerators.Preset> {
    private final Consumer<ChunkGenerator> consumer;

    public SelectChunkGenerator(ServerPlayer player, SimpleGui previousGui, Consumer<ChunkGenerator> consumer) {
        super(MenuType.GENERIC_9x3, player, previousGui);
        this.consumer = consumer;
        setTitle(localized("worldmanager.gui.select_generator.title"));
        build();
    }

    @Override
    protected List<ChunkGenerators.Preset> elements() {
        return ChunkGenerators.PRESETS;
    }

    @Override
    protected GuiElementBuilder toGuiElement(ChunkGenerators.Preset preset) {
        var access = player.registryAccess();
        var chunkGenerator = preset.generator();
        Optional<Tag> result = ChunkGenerator.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, access), chunkGenerator).result();
        Component lore = Component.empty();
        if (result.isPresent()) {
            lore = new TextComponentTagVisitor("    ").visit(result.get());
        }
        List<Component> lines = new LinkedList<>();
        ComponentUtil.parseNewLines(lore, lines);

        return new GuiElementBuilder(preset.icon().asItem())
            .setName(preset.title())
//            .setLore(lines)
            .setCallback(() -> {
                consumer.accept(chunkGenerator);
                previousGui.open();
            });
    }
}
