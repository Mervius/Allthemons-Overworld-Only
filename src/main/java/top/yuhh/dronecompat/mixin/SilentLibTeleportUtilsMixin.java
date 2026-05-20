package top.yuhh.dronecompat.mixin;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;

import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.silentchaos512.lib.util.TeleportUtils")
public class SilentLibTeleportUtilsMixin {

    @Inject(method = "teleport(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceKey;DDDLnet/minecraft/core/Direction;)V", at = @At("HEAD"), cancellable = true)
    private static void cancelTeleport(Player player, ResourceKey<Level> dimension, double destX, double destY, double destZ, @Nullable Direction direction ,CallbackInfo ci) {
        ResourceKey<Level> currentDimension = player.level().dimension();
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("preventDimensionChange");
        if (objective != null) {
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(player, objective);
            int score = scoreAccess.get();
            if (score == 0 && (!currentDimension.equals(dimension)) && !(player instanceof FakePlayer)) {
                System.out.println("Cancelled dimension change");
                ci.cancel();
            }
        }
    }
}
