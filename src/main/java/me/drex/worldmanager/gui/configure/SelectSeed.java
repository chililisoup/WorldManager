package me.drex.worldmanager.gui.configure;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.worldmanager.gui.util.GuiElements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

import static me.drex.message.api.LocalizedMessage.localized;

public class SelectSeed extends AnvilInputGui {

    public SelectSeed(ServerPlayer player, SimpleGui previousGui, Consumer<Long> consumer) {
        super(player, false);
        setTitle(localized("worldmanager.gui.select_seed.title"));
        setSlot(1, GuiElements.back(previousGui));
        setSlot(2, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE)
            .setName(localized("worldmanager.gui.select_seed.confirm.name"))
            .setCallback(() -> {
                String input = getInput();
                try {
                    long seed = Long.parseLong(input);
                    consumer.accept(seed);
                } catch (NumberFormatException e) {
                }
                previousGui.open();
            })
        );
    }
}
