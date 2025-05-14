package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

// TODO Save world time
public record WorldConfig(Holder<DimensionType> type, ChunkGenerator generator, long seed, boolean tickTime) {

    public static final Codec<WorldConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DimensionType.CODEC.fieldOf("type").forGetter(WorldConfig::type),
        ChunkGenerator.CODEC.fieldOf("generator").forGetter(WorldConfig::generator),
        Codec.LONG.optionalFieldOf("seed", 0L).forGetter(WorldConfig::seed),
        Codec.BOOL.optionalFieldOf("tick_time", true).forGetter(WorldConfig::tickTime)
    ).apply(instance, instance.stable(
        WorldConfig::new
    )));

    public WorldConfig withSeed(long seed) {
        return new WorldConfig(type, generator, seed, tickTime);
    }

    public RuntimeWorldConfig toRuntimeWorldConfig() {
        return new RuntimeWorldConfig()
            .setGenerator(generator)
            .setDimensionType(type)
            .setSeed(seed)
            .setShouldTickTime(tickTime);
    }

}
