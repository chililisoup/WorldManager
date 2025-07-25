package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.drex.worldmanager.util.CodecUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

import java.util.Optional;

// TODO Save world time
public final class WorldConfig {

    public static final Codec<WorldConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DimensionType.CODEC.fieldOf("type").forGetter(wc -> wc.type),
        ChunkGenerator.CODEC.fieldOf("generator").forGetter(wc -> wc.generator),
        Codec.LONG.optionalFieldOf("seed", 0L).forGetter(wc -> wc.seed),
        Codec.BOOL.optionalFieldOf("tick_time").forGetter(wc -> Optional.empty()),
        CodecUtil.optionalFieldOf(WorldData.CODEC, "data", WorldData::new, false).forGetter(wc -> wc.data)
    ).apply(instance, instance.stable(WorldConfig::new)));
    public final Holder<DimensionType> type;
    public final ChunkGenerator generator;
    public long seed;
    public WorldData data;

    public WorldConfig(Holder<DimensionType> type, ChunkGenerator generator, long seed, WorldData data) {
        this.type = type;
        this.generator = generator;
        this.seed = seed;
        this.data = data;
    }

    public WorldConfig(Holder<DimensionType> type, ChunkGenerator generator, long seed, Optional<Boolean> tickTime, WorldData data) {
        this(type, generator, seed, data);
        tickTime.ifPresent(value -> this.data.rules.set(GameRules.RULE_DAYLIGHT, value));
    }

    public RuntimeWorldConfig toRuntimeWorldConfig() {
        return new RuntimeWorldConfig()
            .setGenerator(generator)
            .setDimensionType(type)
            .setSeed(seed)
            .setShouldTickTime(true);
    }
}
