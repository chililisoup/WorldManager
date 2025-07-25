package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

public class WorldData {
    public Optional<Location> spawnLocation = Optional.empty();
    public ItemStack icon = ItemStack.EMPTY;
    public GameRuleHolder rules = new GameRuleHolder();

    public static final Codec<WorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Location.CODEC.optionalFieldOf("spawn_location").forGetter(worldData -> worldData.spawnLocation),
        ItemStack.CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(worldData -> worldData.icon),
        GameRuleHolder.CODEC.optionalFieldOf("game_rules", new GameRuleHolder()).forGetter(worldData -> worldData.rules)
    ).apply(instance, instance.stable((location, icon, rules) -> {
        WorldData data = new WorldData();
        data.spawnLocation = location;
        data.icon = icon;
        data.rules.init(rules);
        return data;
    })));

    public GuiElementBuilder iconGuiElement() {
        if (icon.isEmpty()) {
            return new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19");
        } else {
            return new GuiElementBuilder(icon);
        }
    }
}
