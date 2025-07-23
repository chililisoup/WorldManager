package me.drex.worldmanager.extractor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;

import static me.drex.worldmanager.command.ImportCommand.*;
import static net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE;

public class FolderArchiveExtractor implements ArchiveExtractor {
    @Override
    public boolean supports(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    public WorldConfig extract(Path folderPath, Path targetPath, MinecraftServer server) throws CommandSyntaxException, IOException {
        Optional<WorldConfig> config = Optional.empty();
        Path root = Path.of(".");
        try (Stream<Path> pathStream = Files.find(folderPath, 10, (path, basicFileAttributes) -> path.getFileName().toString().equals(LEVEL_DATA_FILE.getId()))) {
            Optional<Path> first = pathStream.findFirst();
            if (first.isPresent()) {
                Path parent = first.get().getParent();
                if (parent != null) root = parent;

                config = parseWorldConfig(Files.newInputStream(first.get()), server);
            }
        }
        if (config.isEmpty()) throw MISSING_LEVEL_DAT.create(folderPath);

        // Copy files
        try (Stream<Path> files = Files.walk(root)) {
            for (Path path : files.toList()) {
                Path relativize = root.relativize(path);
                String topDir = relativize.getName(0).toString();
                if (!DIMENSION_PREFIXES.contains(topDir)) continue;

                Path resolvedPath = targetPath.resolve(relativize);

                if (Files.isDirectory(path)) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    try (InputStream inputStream = Files.newInputStream(path)) {
                        Files.copy(inputStream, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        return config.get();
    }
}
