package me.drex.worldmanager.save;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ChunkGenerators {
    public static final Map<ResourceLocation, Function<RegistryAccess, ChunkGenerator>> PRESETS = new HashMap<>();

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.getAllLevels().forEach(level -> {
                PRESETS.put(level.dimension().location(), server1 -> level.getChunkSource().getGenerator());
            });
        });

        PRESETS.put(ResourceLocation.fromNamespaceAndPath("fantasy", "void"), registryAccess ->
            new VoidChunkGenerator(registryAccess.lookupOrThrow(Registries.BIOME).get(Biomes.THE_VOID).orElseThrow()));
        PRESETS.put(ResourceLocation.withDefaultNamespace("debug"), registryAccess ->
            new DebugLevelSource(registryAccess.lookupOrThrow(Registries.BIOME).get(Biomes.PLAINS).orElseThrow()));
        // TODO flat generator (requires arguments)
    }
}
