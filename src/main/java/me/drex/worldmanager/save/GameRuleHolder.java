package me.drex.worldmanager.save;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import me.drex.worldmanager.WorldManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GameRuleHolder {
    public static final Codec<GameRuleHolder> CODEC = Codec.PASSTHROUGH
        .comapFlatMap(GameRuleHolder::of, GameRuleHolder::getDynamic);

    private static final GameRules DEFAULT = new GameRules(FeatureFlagSet.of());

    private final CompoundTag rules;
    private @NotNull Dynamic<?> heldDynamic;
    private @Nullable GameRules attachedRules;
    private @Nullable MinecraftServer server;

    private GameRuleHolder(@NotNull Dynamic<?> dynamic) {
        this.heldDynamic = dynamic;
        this.rules = this.getTag(dynamic);
    }

    public GameRuleHolder() {
        this(new Dynamic<>(NbtOps.INSTANCE, new CompoundTag()));
    }

    private static DataResult<GameRuleHolder> of(Dynamic<?> dynamic) {
        return DataResult.success(new GameRuleHolder(dynamic));
    }

    public void init(GameRuleHolder other) {
        if (this.attachedRules != null) {
            WorldManager.LOGGER.warn("Attempted to initialize in-use GameRuleHolder");
            return;
        }
        this.heldDynamic = other.heldDynamic;
    }

    private void createTempRules(MinecraftServer server) {
        this.attachedRules = new GameRules(server.createCommandSourceStack().enabledFeatures(), this.heldDynamic);
        this.setServer(server);
    }

    public void attachGameRules(GameRules rules, MinecraftServer server) {
        if (this.attachedRules == null) this.createTempRules(server);
        rules.assignFrom(this.attachedRules, server);
        this.attachedRules = rules;
    }

    public void setServer(@Nullable MinecraftServer server) {
        this.server = server;
    }

    public void set(GameRules.Key<?> key, String value) {
        this.rules.putString(key.getId(), value);
        if (this.attachedRules != null) {
            GameRules.Value<?> rule = this.attachedRules.getRule(key);
            rule.deserialize(value);
            rule.onChanged(this.server);
        }
    }

    public void set(GameRules.Key<GameRules.BooleanValue> key, boolean value) {
        this.set(key, Boolean.toString(value));
    }

    public void set(GameRules.Key<GameRules.IntegerValue> key, int value) {
        this.set(key, Integer.toString(value));
    }

    public void toggle(GameRules.Key<GameRules.BooleanValue> key) {
        this.set(key, !this.getBoolean(key));
    }

    public boolean getBoolean(GameRules.Key<GameRules.BooleanValue> key) {
        if (this.attachedRules != null) return this.attachedRules.getBoolean(key);

        if (this.rules.get(key.getId()) instanceof StringTag(String value))
            return Boolean.parseBoolean(value);

        return DEFAULT.getBoolean(key);
    }

    public int getInt(GameRules.Key<GameRules.IntegerValue> key) {
        if (this.attachedRules != null) return this.attachedRules.getInt(key);

        if (this.rules.get(key.getId()) instanceof StringTag(String value) && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException var2) {
                WorldManager.LOGGER.warn("Failed to parse integer {}", value);
            }
        }

        return DEFAULT.getInt(key);
    }

    public Pair<String, Integer> setFromArgument(GameRules.Key<?> key, CommandContext<CommandSourceStack> context, String argument) {
        GameRules.Value<?> rule = this.getRule(key);
        rule.setFromArgument(context, argument);
        this.rules.putString(key.getId(), rule.serialize());
        return Pair.of(rule.toString(), rule.getCommandResult());
    }

    public String getRuleString(GameRules.Key<?> key) {
        return this.getRule(key).toString();
    }

    private <T extends GameRules.Value<T>> T getRule(GameRules.Key<T> key) {
        assert this.attachedRules != null;
        return this.attachedRules.getRule(key);
    }

    private CompoundTag getTag(Dynamic<?> dynamic) {
        return dynamic.getValue() instanceof CompoundTag tag ? tag : new CompoundTag();
    }

    public Dynamic<?> getDynamic() {
        return this.attachedRules == null ?
            this.heldDynamic :
            new Dynamic<>(NbtOps.INSTANCE, this.attachedRules.createTag());
    }
}
