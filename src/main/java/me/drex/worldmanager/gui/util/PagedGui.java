package me.drex.worldmanager.gui.util;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.List;

import static me.drex.message.api.LocalizedMessage.localized;

public abstract class PagedGui<T> extends SimpleGui {
    private static final int WIDTH = 9;
    protected final SimpleGui previousGui;

    private int page = 0;

    public PagedGui(MenuType<ChestMenu> menuType, ServerPlayer player) {
        this(menuType, player, null);
    }

    public PagedGui(MenuType<ChestMenu> menuType, ServerPlayer player, SimpleGui previousGui) {
        super(menuType, player, false);
        this.previousGui = previousGui;
        assert height > 1;
        assert width == WIDTH;
    }

    public void build() {
        List<T> elements = elements();
        var slots = getVirtualSize() - WIDTH;
        for (int slotIndex = 0; slotIndex < slots; slotIndex++) {
            var elementIndex = page * slots + slotIndex;
            if (elementIndex >= elements.size()) {
                clearSlot(slotIndex);
                continue;
            }
            var element = elements.get(elementIndex);
            setSlot(slotIndex, toGuiElement(element));
        }

        setSlot(slots, new GuiElementBuilder(Items.PLAYER_HEAD)
            .setSkullOwner(SkullTextures.ARROW_LEFT)
            .setName(localized("worldmanager.gui.paged.previous_page.name"))
            .setCallback(() -> {
                page = Math.max(page - 1, 0);
                build();
            })
        );
        setSlot(slots + 4, GuiElements.back(previousGui));
        setSlot(slots + 8, new GuiElementBuilder(Items.PLAYER_HEAD)
            .setSkullOwner(SkullTextures.ARROW_RIGHT)
            .setName(localized("worldmanager.gui.paged.next_page.name"))
            .setCallback(() -> {
                var maxPage = (elements.size() - 1) / slots;
                page = Math.min(page + 1, maxPage);
                build();
            })
        );
    }

    protected abstract List<T> elements();

    protected abstract GuiElementBuilder toGuiElement(T t);
}
