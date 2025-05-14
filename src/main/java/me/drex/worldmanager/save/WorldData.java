package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public class WorldData {
    public Optional<Location> spawnLocation = Optional.empty();

    public static final Codec<WorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Location.CODEC.optionalFieldOf("spawn_location").forGetter(worldData -> worldData.spawnLocation)
    ).apply(instance, instance.stable((location) -> {
        WorldData data = new WorldData();
        data.spawnLocation = location;
        return data;
    })));
}
