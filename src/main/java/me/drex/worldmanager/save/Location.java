package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal./*? if >=1.21.2 {*/ TeleportTransition /*?} else {*/ /*DimensionTransition *//*?}*/;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record Location(Vec3 position, Vec2 rotation) {

    //? if < 1.21.5 {
    /*public static final Codec<Vec2> VEC_2_CODEC = Codec.FLOAT
                .listOf()
                .comapFlatMap(list -> Util.fixedSize(list, 2).map(listx -> new Vec2(listx.getFirst(), listx.get(1))), vec2 -> List.of(vec2.x, vec2.y));
    *///?}

    public static final Codec<Location> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Vec3.CODEC.fieldOf("position").forGetter(Location::position),
        //? if >= 1.21.5 {
        Vec2.CODEC.fieldOf("rotation").forGetter(Location::rotation)
        //?} else {
        /*VEC_2_CODEC.fieldOf("rotation").forGetter(Location::rotation)
        *///?}
    ).apply(instance, instance.stable(Location::new)));

    public Location(Entity entity) {
        this(entity.position(), entity.getRotationVector());
    }

    public Location(CommandSourceStack source) {
        this(source.getPosition(), source.getRotation());
    }

    public /*? if >=1.21.2 {*/ TeleportTransition /*?} else {*/ /*DimensionTransition *//*?}*/ toTeleportTransition(ServerLevel level) {


        return new /*? if >=1.21.2 {*/ TeleportTransition /*?} else {*/ /*DimensionTransition *//*?}*/(level, position, Vec3.ZERO, rotation.y, rotation.x, /*? if >=1.21.2 {*/ TeleportTransition /*?} else {*/ /*DimensionTransition *//*?}*/.DO_NOTHING);
    }
}
