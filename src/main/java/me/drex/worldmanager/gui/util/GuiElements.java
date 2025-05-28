package me.drex.worldmanager.gui.util;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.world.item.Items;

import static me.drex.message.api.LocalizedMessage.localized;

public class GuiElements {
    public static GuiElementBuilder back(SimpleGui previousGui) {
        if (previousGui != null) {
            return new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(SkullTextures.BACKWARD)
                .setCallback(previousGui::open)
                .setName(localized("worldmanager.gui.generic.back.name"));
        } else {
            return new GuiElementBuilder(Items.AIR);
        }
    }
}
