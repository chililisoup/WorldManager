package me.drex.worldmanager.gui.create;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.message.impl.util.ComponentUtil;
import me.drex.worldmanager.gui.util.PagedGui;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static me.drex.message.api.LocalizedMessage.localized;

public class SelectDimensionType extends PagedGui<Holder.Reference<DimensionType>> {
    private final Consumer<Holder<DimensionType>> consumer;

    public SelectDimensionType(ServerPlayer player, SimpleGui previousGui, Consumer<Holder<DimensionType>> consumer) {
        super(MenuType.GENERIC_9x3, player, previousGui);
        this.consumer = consumer;
        setTitle(localized("worldmanager.gui.select_type.title"));
        build();
    }

    @Override
    protected List<Holder.Reference<DimensionType>> elements() {
        HolderLookup<DimensionType> dimensionType = player.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
        return dimensionType.listElements().toList();
    }

    @Override
    protected GuiElementBuilder toGuiElement(Holder.Reference<DimensionType> holder) {
        var access = player.registryAccess();
        Optional<Tag> result = DimensionType.DIRECT_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, access), holder.value()).result();
        Component lore = Component.empty();
        if (result.isPresent()) {
            lore = new TextComponentTagVisitor("    ").visit(result.get());
        }
        List<Component> lines = new LinkedList<>();
        ComponentUtil.parseNewLines(lore, lines);

        return new GuiElementBuilder(toIcon(holder.key()))
            .setName(Component.literal(holder.key().location().toString()))
            .setLore(lines)
            .setCallback(() -> {
                consumer.accept(holder);
                previousGui.open();
            });
    }

    public static Item toIcon(ResourceKey<DimensionType> resourceKey) {
        if (resourceKey == BuiltinDimensionTypes.OVERWORLD) return Items.GRASS_BLOCK;
        if (resourceKey == BuiltinDimensionTypes.NETHER) return Items.NETHERRACK;
        if (resourceKey == BuiltinDimensionTypes.END) return Items.END_STONE;
        if (resourceKey == BuiltinDimensionTypes.OVERWORLD_CAVES) return Items.IRON_ORE;
        return Items.STONE;
    }
}
