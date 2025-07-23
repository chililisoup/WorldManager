package me.drex.worldmanager.extractor;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.github.junrar.rarfile.FileHeader;
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
import java.util.List;
import java.util.Optional;

import static me.drex.worldmanager.command.ImportCommand.*;
import static net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE;

public class RarArchiveExtractor implements ArchiveExtractor {
    @Override
    public boolean supports(Path path) {
        String extension = FilenameUtils.getExtension(path.toString());
        return extension.equalsIgnoreCase("rar");
    }

    @Override
    public WorldConfig extract(Path rarPath, Path targetPath, MinecraftServer server) throws CommandSyntaxException, IOException {
        Optional<WorldConfig> config = Optional.empty();
        try {
            Archive archive = new Archive(Files.newInputStream(rarPath));
            Path root = Path.of(".");
            List<FileHeader> fileHeaders = archive.getFileHeaders();
            for (FileHeader fileHeader : fileHeaders) {
                String fileName = fileHeader.getFileName().replace('\\', '/');
                Path path = Paths.get(fileName);
                if (path.getFileName().toString().equals(LEVEL_DATA_FILE.getId())) {
                    Path parent = path.getParent();
                    if (parent != null) root = parent;

                    try (var is = archive.getInputStream(fileHeader)) {
                        config = parseWorldConfig(is, server);
                    }
                    break;
                }
            }
            if (config.isEmpty()) throw MISSING_LEVEL_DAT.create(rarPath);

            // Copy files
            for (FileHeader fileHeader : fileHeaders) {
                Path entryPath = Paths.get(fileHeader.getFileName().replace('\\', '/'));
                Path relativize = root.relativize(entryPath);

                String topDir = relativize.getName(0).toString();
                if (!DIMENSION_PREFIXES.contains(topDir)) continue;

                Path resolvedPath = targetPath.resolve(relativize);

                if (fileHeader.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    try (InputStream inputStream = archive.getInputStream(fileHeader)) {
                        Files.copy(inputStream, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            return config.get();
        } catch (RarException e) {
            if (e instanceof UnsupportedRarV5Exception) {
                throw RAR5.create(rarPath);
            } else {
                throw new IOException(e);
            }
        }
    }
}
