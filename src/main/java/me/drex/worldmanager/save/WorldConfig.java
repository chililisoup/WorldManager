package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

public record WorldConfig(Holder<DimensionType> type, ChunkGenerator generator, long seed) {

    public static final Codec<WorldConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DimensionType.CODEC.fieldOf("type").forGetter(WorldConfig::type),
        ChunkGenerator.CODEC.fieldOf("generator").forGetter(WorldConfig::generator),
        Codec.LONG.fieldOf("seed").forGetter(WorldConfig::seed)
    ).apply(instance, instance.stable(
        WorldConfig::new
    )));

    public RuntimeWorldConfig toRuntimeWorldConfig() {
        return new RuntimeWorldConfig()
            .setGenerator(generator)
            .setDimensionType(type)
            .setSeed(seed);
    }

}
