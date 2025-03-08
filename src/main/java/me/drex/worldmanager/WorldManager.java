package me.drex.worldmanager;

import me.drex.worldmanager.command.WorldManagerCommand;
import me.drex.worldmanager.save.ChunkGenerators;
import me.drex.worldmanager.save.WorldManagerSavedData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldManager implements ModInitializer {

    public static final String MOD_ID = "worldmanager";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ChunkGenerators.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
            savedData.loadWorlds(server);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            WorldManagerCommand.register(dispatcher);
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}