package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class WorldData {
    public Optional<Location> spawnLocation = Optional.empty();
    public ItemStack icon = ItemStack.EMPTY;

    public static final Codec<WorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Location.CODEC.optionalFieldOf("spawn_location").forGetter(worldData -> worldData.spawnLocation),
        ItemStack.CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(worldData -> worldData.icon)
    ).apply(instance, instance.stable((location, icon) -> {
        WorldData data = new WorldData();
        data.spawnLocation = location;
        data.icon = icon;
        return data;
    })));
}
