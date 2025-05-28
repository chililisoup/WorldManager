package me.drex.worldmanager.gui.util;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import static me.drex.message.api.LocalizedMessage.builder;
import static me.drex.message.api.LocalizedMessage.localized;

public class ConfirmTypeGui extends AnvilInputGui {
    private final String text;
    private final Runnable action;
    private final SimpleGui previousGui;

    public ConfirmTypeGui(ServerPlayer player, String text, Runnable action, SimpleGui previousGui) {
        super(player, false);
        this.text = text;
        this.action = action;
        this.previousGui = previousGui;
        setTitle(builder("worldmanager.gui.confirm_type.title").addPlaceholder("text", text).build());
    }

    @Override
    public void onInput(String input) {
        if (input.equals(text)) {
            setSlot(2,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setName(localized("worldmanager.gui.confirm_type.confirm.name"))
                    .addLoreLine(localized("worldmanager.gui.confirm_type.confirm.lore"))
                    .setSkullOwner(SkullTextures.CHECKMARK)
                    .setCallback(action)
            );
        } else {
            clearSlot(2);
        }
        setSlot(1,
            new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(localized("worldmanager.gui.confirm_type.cancel.name"))
                .addLoreLine(localized("worldmanager.gui.confirm_type.cancel.lore"))
                .setSkullOwner(SkullTextures.BACKWARD)
                .setCallback(previousGui::open)
        );
    }
}
