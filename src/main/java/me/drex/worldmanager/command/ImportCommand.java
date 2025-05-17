package me.drex.worldmanager.command;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.github.junrar.rarfile.FileHeader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import me.drex.worldmanager.WorldManager;
import me.drex.worldmanager.mixin.MinecraftServerAccessor;
import me.drex.worldmanager.save.Location;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldData;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.FilenameUtils;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static me.drex.message.api.LocalizedMessage.builder;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE;

public class ImportCommand {

    public static final Set<String> DIMENSION_PREFIXES = Set.of("data", "region", "entities", "poi");
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of("zip", "rar");
    public static final SuggestionProvider<CommandSourceStack> PATHS = (context, builder) ->
    {
        try {
            return SharedSuggestionProvider.suggest(
                Files.list(FabricLoader.getInstance().getGameDir()).filter(path -> {
                    if (Files.isDirectory(path)) {
                        return true;
                    } else {
                        String extension = FilenameUtils.getExtension(path.toString());
                        return SUPPORTED_EXTENSIONS.contains(extension);
                    }
                }).map(Path::toString).toList(), builder
            );
        } catch (IOException e) {
            return Suggestions.empty();
        }
    };

    public static final DynamicCommandExceptionType MISSING_LEVEL_DAT = new DynamicCommandExceptionType((file) -> builder("worldmanager.command.import.exception.level_dat").addPlaceholder("file", file.toString()).build());
    public static final DynamicCommandExceptionType RAR5 = new DynamicCommandExceptionType((file) -> builder("worldmanager.command.import.exception.rar5").addPlaceholder("file", file.toString()).build());
    public static final Dynamic2CommandExceptionType UNKNOWN_EXTENSION = new Dynamic2CommandExceptionType((file, extension) -> builder("worldmanager.command.import.exception.unknown_extension")
        .addPlaceholder("file", file.toString())
        .addPlaceholder("extension", extension.toString())
        .build());
    public static final DynamicCommandExceptionType IO_EXCEPTION = new DynamicCommandExceptionType((file) -> builder("worldmanager.command.import.exception.ioexception").addPlaceholder("file", file.toString()).build());


    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("import")
            .requires(Permissions.require("worldmanager.command.worldmanager.import", 4))
            .then(
                argument("id", ResourceLocationArgument.id())
                    .then(
                        argument("path", StringArgumentType.greedyString())
                            .suggests(PATHS)
                            .executes(context -> {
                                ResourceLocation id = ResourceLocationArgument.getId(context, "id");
                                MinecraftServer server = context.getSource().getServer();
                                CreateCommand.validLevelId(id, server);

                                Fantasy fantasy = Fantasy.get(server);
                                LevelStorageSource.LevelStorageAccess storageSource = ((MinecraftServerAccessor) server).getStorageSource();
                                Path targetPath = storageSource.getDimensionPath(ResourceKey.create(Registries.DIMENSION, id));

                                Path localPath = Path.of(StringArgumentType.getString(context, "path"));
                                Path fullPath = FabricLoader.getInstance().getGameDir().resolve(localPath);
                                WorldConfig config;
                                try {
                                    if (Files.isDirectory(fullPath)) {
                                        config = copyFolder(fullPath, targetPath, server);
                                    } else {
                                        String extension = FilenameUtils.getExtension(fullPath.toString());
                                        if (extension.equalsIgnoreCase("zip")) {
                                            config = extractZip(fullPath, targetPath, server);
                                        } else if (extension.equalsIgnoreCase("rar")) {
                                            config = extractRar(fullPath, targetPath, server);
                                        } else {
                                            throw UNKNOWN_EXTENSION.create(fullPath, extension);
                                        }
                                    }
                                } catch (IOException e) {
                                    throw IO_EXCEPTION.create(fullPath);
                                }

                                RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(id, config.toRuntimeWorldConfig());
                                WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
                                savedData.addWorld(id, config, handle);
                                context.getSource().sendSuccess(() -> builder("worldmanager.command.import").addPlaceholder("id", id.toString()).build(), false);
                                return 1;
                            })
                    )
            );
    }

    private static WorldConfig extractZip(Path zipPath, Path targetPath, MinecraftServer server) throws CommandSyntaxException, IOException {
        Optional<WorldConfig> config = Optional.empty();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Path root = Path.of(".");
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                Path path = Path.of(zipEntry.getName());
                if (path.getFileName().toString().equals(LEVEL_DATA_FILE.getId())) {
                    root = path.getParent();
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

    private static WorldConfig extractRar(Path rarPath, Path targetPath, MinecraftServer server) throws CommandSyntaxException, IOException {
        Optional<WorldConfig> config = Optional.empty();
        try {
            Archive archive = new Archive(Files.newInputStream(rarPath));
            Path root = Path.of(".");
            List<FileHeader> fileHeaders = archive.getFileHeaders();
            for (FileHeader fileHeader : fileHeaders) {
                String fileName = fileHeader.getFileName().replace('\\', '/');
                Path path = Paths.get(fileName);
                if (path.getFileName().toString().equals(LEVEL_DATA_FILE.getId())) {
                    root = path.getParent();
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

    private static WorldConfig copyFolder(Path folderPath, Path targetPath, MinecraftServer server) throws CommandSyntaxException, IOException {
        Optional<WorldConfig> config = Optional.empty();
        Path root = Path.of(".");
        try (Stream<Path> pathStream = Files.find(folderPath, 10, (path, basicFileAttributes) -> path.getFileName().toString().equals(LEVEL_DATA_FILE.getId()))) {
            Optional<Path> first = pathStream.findFirst();
            if (first.isPresent()) {
                root = first.get().getParent();
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

    //? if >= 1.21.5 {
    private static Optional<WorldConfig> parseWorldConfig(InputStream is, MinecraftServer server) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
        return tag.getCompound("Data")
            .flatMap(data -> {
                var spawnX = data.getIntOr("SpawnX", 0);
                var spawnY = data.getIntOr("SpawnY", 0);
                var spawnZ = data.getIntOr("SpawnZ", 0);
                var spawnAngle = data.getFloatOr("SpawnAngle", 0);
                return data.getCompound("WorldGenSettings")
                    .flatMap(worldGenSettings -> {
                        long seed = worldGenSettings.getLongOr("seed", 0);
                        return worldGenSettings.getCompound("dimensions")
                            // TODO add option to pick dimension
                            .flatMap(dimensions -> dimensions.getCompound("minecraft:overworld")
                                .flatMap(overworld -> {
                                    return createWorldConfig(server, overworld, spawnX, spawnY, spawnZ, spawnAngle, seed);
                                }));
                    });
            });
    }
    //?} else {
    /*private static Optional<WorldConfig> parseWorldConfig(InputStream is, MinecraftServer server) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
        var data = tag.getCompound("Data");

        var spawnX = data.getInt("SpawnX");
        var spawnY = data.getInt("SpawnY");
        var spawnZ = data.getInt("SpawnZ");
        var spawnAngle = data.getFloat("SpawnAngle");
        var worldGenSettings = data.getCompound("WorldGenSettings");
        long seed = worldGenSettings.getLong("seed");
        // TODO add option to pick dimension
        var overworld = worldGenSettings.getCompound("dimensions").getCompound("minecraft:overworld");
        return createWorldConfig(server, overworld, spawnX, spawnY, spawnZ, spawnAngle, seed);
    }
    *///?}

    private static Optional<WorldConfig> createWorldConfig(MinecraftServer server, CompoundTag overworld, int spawnX, int spawnY, int spawnZ, float spawnAngle, long seed) {
        try {
            WorldConfig config = WorldConfig.CODEC.decode(RegistryOps.create(NbtOps.INSTANCE, server.registryAccess()), overworld)
                .getOrThrow(IllegalStateException::new)
                .getFirst();
            WorldData worldData = new WorldData();
            worldData.spawnLocation = Optional.of(new Location(new Vec3(spawnX, spawnY, spawnZ), new Vec2(spawnAngle, 0)));
            config.seed = seed;
            config.data = worldData;
            return Optional.of(config);
        } catch (IllegalStateException e) {
            WorldManager.LOGGER.error("Failed to decode level.dat", e);
            return Optional.empty();
        }
    }

}
