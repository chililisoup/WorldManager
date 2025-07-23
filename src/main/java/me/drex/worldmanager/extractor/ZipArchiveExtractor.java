package me.drex.worldmanager.extractor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static me.drex.worldmanager.command.ImportCommand.*;
import static net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE;

public class ZipArchiveExtractor implements ArchiveExtractor {
    @Override
    public boolean supports(Path path) {
        String extension = FilenameUtils.getExtension(path.toString());
        return extension.equalsIgnoreCase("zip");
    }

    @Override
    public WorldConfig extract(Path zipPath, Path targetPath, MinecraftServer server) throws CommandSyntaxException, IOException {
        Optional<WorldConfig> config = Optional.empty();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Path root = Path.of(".");
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                Path path = Path.of(zipEntry.getName());
                if (path.getFileName().toString().equals(LEVEL_DATA_FILE.getId())) {
                    Path parent = path.getParent();
                    if (parent != null) root = parent;

                    try (var is = zipFile.getInputStream(zipEntry)) {
                        config = parseWorldConfig(is, server);
                    }
                    break;
                }
            }
            if (config.isEmpty()) throw MISSING_LEVEL_DAT.create(zipPath);

            // Copy files
            Iterator<? extends ZipEntry> iterator = zipFile.entries().asIterator();
            while (iterator.hasNext()) {
                ZipEntry zipEntry = iterator.next();
                Path entryPath = Paths.get(zipEntry.getName());
                Path relativize = root.relativize(entryPath);

                String topDir = relativize.getName(0).toString();
                if (!DIMENSION_PREFIXES.contains(topDir)) continue;

                Path resolvedPath = targetPath.resolve(relativize);

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                        Files.copy(inputStream, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        return config.get();
    }
}
