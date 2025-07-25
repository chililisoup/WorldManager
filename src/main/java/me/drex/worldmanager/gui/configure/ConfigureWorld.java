package me.drex.worldmanager.gui.configure;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.message.api.LocalizedMessage;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldData;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Optional;

public abstract class ConfigureWorld extends SimpleGui {

    protected final ResourceLocation id;
    protected Holder<DimensionType> type;
    protected ChunkGenerator generator;
    protected long seed;
    protected WorldData worldData;

    public ConfigureWorld(ServerPlayer player, ResourceLocation id) {
        super(MenuType.GENERIC_9x3, player, false);
        this.id = id;
        setTitle(LocalizedMessage.builder("worldmanager.gui.configure.title").addPlaceholder("id", id.toString()).build());
        setupDefaults(player.getServer());
        build();
    }

    private void setupDefaults(MinecraftServer server) {
        RegistryAccess.Frozen frozen = server.registryAccess();
        HolderLookup<DimensionType> dimensionType = frozen.lookupOrThrow(Registries.DIMENSION_TYPE);
        this.type = dimensionType.getOrThrow(BuiltinDimensionTypes.OVERWORLD);
        this.generator = server.overworld().getChunkSource().getGenerator();
        this.seed = 0;
        this.worldData = new WorldData();
        worldData.rules.set(GameRules.RULE_DAYLIGHT, true);
    }

    public void build() {
        setSlot(0,
            builder(Items.ARMOR_STAND, "type")
                .setCallback(() -> {
                    new SelectDimensionType(player, this, type -> {
                        this.type = type;
                        build();
                    }).open();
                })
        );
        setSlot(1,
            builder(Items.GRASS_BLOCK, "generator")
                .setCallback(() -> {
                    new SelectChunkGenerator(player, this, generator -> {
                        this.generator = generator;
                        build();
                    }).open();
                })
        );
        setSlot(2,
            builder(Items.WHEAT_SEEDS, "seed")
                .setCallback(() -> {
                    new SelectSeed(player, this, seed -> {
                        this.seed = seed;
                        build();
                    }).open();
                })
        );
        setSlot(3,
            builder(Items.CLOCK, "tick_time")
                .setCallback(() -> {
                    this.worldData.rules.toggle(GameRules.RULE_DAYLIGHT);
                    build();
                })
        );
        setSlot(4,
            builder(this.worldData.rules.getBoolean(GameRules.RULE_WEATHER_CYCLE) ? Items.WATER_BUCKET : Items.BUCKET, "weather_cycle")
                .setCallback(() -> {
                    this.worldData.rules.toggle(GameRules.RULE_WEATHER_CYCLE);
                    build();
                })
        );
        setSlot(5,
            builder(Items.ZOMBIE_SPAWN_EGG, "mob_spawning")
                .setCallback(() -> {
                    this.worldData.rules.toggle(GameRules.RULE_DOMOBSPAWNING);
                    build();
                })
        );
        setSlot(6,
            builder(Items.CREEPER_HEAD, "mob_griefing")
                .setCallback(() -> {
                    this.worldData.rules.toggle(GameRules.RULE_MOBGRIEFING);
                    build();
                })
        );

        for (int i = 2 * 9; i < 3 * 9; i++) {
            setSlot(i,
                builder(Items.GREEN_STAINED_GLASS_PANE, "confirm")
                    .setCallback(() -> {
                        WorldConfig config = new WorldConfig(
                            type,
                            generator,
                            seed,
                            worldData
                        );
                        confirm(config);
                        close();
                    })
            );
        }
    }

    protected abstract void confirm(WorldConfig config);

    private GuiElementBuilder builder(Item item, String id) {
        var access = player.registryAccess();
        Optional<Tag> result = ChunkGenerator.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, access), generator).result();
        Component lore = Component.empty();
        if (result.isPresent()) {
            lore = new TextComponentTagVisitor("    ").visit(result.get());
        }
        return new GuiElementBuilder(item)
            .setName(LocalizedMessage.builder("worldmanager.gui.configure." + id + ".name")
                .build())
            .addLoreLine(LocalizedMessage.builder("worldmanager.gui.configure." + id + ".lore")
                .addPlaceholder("type", type.unwrapKey().map(ResourceKey::location).map(ResourceLocation::toString).orElse("???"))
                .addPlaceholder("generator", generator.getTypeNameForDataFixer().map(ResourceKey::location).map(ResourceLocation::toString).orElse("???"))
                .addPlaceholder("generator_full", lore)
                .addPlaceholder("seed", seed)
                .addPlaceholder("tick_time", worldData.rules.getBoolean(GameRules.RULE_DAYLIGHT))
                .addPlaceholder("weather_cycle", worldData.rules.getBoolean(GameRules.RULE_WEATHER_CYCLE))
                .addPlaceholder("mob_spawning", worldData.rules.getBoolean(GameRules.RULE_DOMOBSPAWNING))
                .addPlaceholder("mob_griefing", worldData.rules.getBoolean(GameRules.RULE_MOBGRIEFING))
                .build());
    }
}
