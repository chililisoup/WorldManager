package me.drex.worldmanager.extractor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static me.drex.worldmanager.command.ImportCommand.*;
import static net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE;

public class TarGzArchiveExtractor implements ArchiveExtractor {
    @Override
    public boolean supports(Path path) {
        String name = FilenameUtils.getName(path.toString().toLowerCase());
        return name.endsWith(".tar.gz") || name.endsWith(".tgz");
    }

    @Override
    public WorldConfig extract(Path tarGzPath, Path targetPath, MinecraftServer server) throws CommandSyntaxException, IOException {
        Optional<WorldConfig> config = Optional.empty();

        try (InputStream fi = Files.newInputStream(tarGzPath);
             InputStream bi = new BufferedInputStream(fi);
             InputStream gzi = new GzipCompressorInputStream(bi);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzi)) {

            Path root = Path.of(".");
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                Path entryPath = Paths.get(entry.getName());
                if (entryPath.getFileName().toString().equals(LEVEL_DATA_FILE.getId())) {
                    Path parent = entryPath.getParent();
                    if (parent != null) root = parent;

                    config = parseWorldConfig(tarIn, server);
                    break;
                }
            }

            if (config.isEmpty()) throw MISSING_LEVEL_DAT.create(tarGzPath);

            try (InputStream fi2 = Files.newInputStream(tarGzPath);
                 InputStream bi2 = new BufferedInputStream(fi2);
                 InputStream gzi2 = new GzipCompressorInputStream(bi2);
                 TarArchiveInputStream tarIn2 = new TarArchiveInputStream(gzi2)) {

                while ((entry = tarIn2.getNextEntry()) != null) {
                    Path entryPath = Paths.get(entry.getName());
                    Path relativize = root.relativize(entryPath);

                    if (relativize.getNameCount() == 0) continue;
                    String topDir = relativize.getName(0).toString();
                    if (!DIMENSION_PREFIXES.contains(topDir)) continue;

                    Path resolvedPath = targetPath.resolve(relativize);

                    if (entry.isDirectory()) {
                        Files.createDirectories(resolvedPath);
                    } else {
                        Files.createDirectories(resolvedPath.getParent());
                        Files.copy(tarIn2, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }

        return config.get();
    }
}
