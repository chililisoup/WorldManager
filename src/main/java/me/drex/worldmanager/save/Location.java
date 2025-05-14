package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record Location(Vec3 pos, Vec2 rot) {
    public static final Codec<Location> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Vec3.CODEC.fieldOf("pos").forGetter(Location::pos),
        Vec2.CODEC.fieldOf("rot").forGetter(Location::rot)
    ).apply(instance, instance.stable(Location::new)));

    public Location(Entity entity) {
        this(entity.position(), entity.getRotationVector());
    }

    public Location(CommandSourceStack source) {
        this(source.getPosition(), source.getRotation());
    }

    public TeleportTransition toTeleportTransition(ServerLevel level) {
        return new TeleportTransition(level, pos, Vec3.ZERO, rot.y, rot.x, TeleportTransition.DO_NOTHING);
    }
}
