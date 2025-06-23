package me.drex.worldmanager.extractor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Path;

public interface ArchiveExtractor {
    boolean supports(Path path);

    WorldConfig extract(Path archive, Path target, MinecraftServer server) throws CommandSyntaxException, IOException;
}
