package me.drex.worldmanager.mixin;

import eu.pb4.playerdata.api.PlayerDataApi;
import me.drex.worldmanager.WorldManager;
import me.drex.worldmanager.data.PlayerData;
import me.drex.worldmanager.save.Location;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal./*? if >=1.21.2 {*/ TeleportTransition /*?} else {*/ /*DimensionTransition *//*?}*/;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Shadow public abstract ServerLevel serverLevel();

    @Inject(
        //? if >= 1.21.2 {
        method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
        //?} else {
        /*method = "changeDimension",
        *///?}
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setServerLevel(Lnet/minecraft/server/level/ServerLevel;)V"
        )
    )
    public void saveLocation(/*? if >=1.21.2 {*/ TeleportTransition /*?} else {*/ /*DimensionTransition *//*?}*/ dimensionTransition, CallbackInfoReturnable<Entity> cir) {
        var player = (ServerPlayer) (Object) this;
        PlayerData playerData = PlayerDataApi.getCustomDataFor(player, WorldManager.STORAGE);
        if (playerData == null) {
            playerData = new PlayerData(Collections.emptyMap());
            PlayerDataApi.setCustomDataFor(player, WorldManager.STORAGE, playerData);
        }
        playerData.locations().put(serverLevel().dimension(), new Location(player));
    }
}
