package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.HashMap;
import java.util.Map;

public class WorldManagerSavedData extends SavedData {

    private final Map<ResourceLocation, WorldConfig> worlds;
    private final Map<ResourceLocation, RuntimeWorldHandle> worldHandles = new HashMap<>();
    private static final Codec<WorldManagerSavedData> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, WorldConfig.CODEC)
        .xmap(WorldManagerSavedData::new, worldManagerSavedData -> worldManagerSavedData.worlds);

    public static final SavedDataType<WorldManagerSavedData> TYPE = new SavedDataType<>("worldmanager", WorldManagerSavedData::new, CODEC, null);
    
    private WorldManagerSavedData() {
        this.worlds = new HashMap<>();
    }

    private WorldManagerSavedData(Map<ResourceLocation, WorldConfig> worlds) {
        this.worlds = new HashMap<>(worlds);
    }

    public static WorldManagerSavedData getSavedData(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void loadWorlds(MinecraftServer server) {
        this.worlds.forEach((id, worldConfig) -> {
            RuntimeWorldHandle handle = Fantasy.get(server).getOrOpenPersistentWorld(id, worldConfig.toRuntimeWorldConfig());
            worldHandles.put(id, handle);
        });
    }

    public void addWorld(ResourceLocation id, WorldConfig config, RuntimeWorldHandle handle) {
        worlds.put(id, config);
        worldHandles.put(id, handle);
        setDirty();
    }

    public boolean removeWorld(ResourceLocation id) {
        WorldConfig config = worlds.remove(id);
        if (config != null) {
            RuntimeWorldHandle handle = worldHandles.remove(id);
            handle.delete();
            setDirty();
            return true;
        }
        return false;
    }

    public WorldConfig getConfig(ResourceLocation id) {
        return worlds.get(id);
    }

    public Map<ResourceLocation, WorldConfig> getWorlds() {
        return new HashMap<>(worlds);
    }

}
