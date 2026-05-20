package top.yuhh.dronecompat.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;

import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.telepathicgrunt.the_bumblezone.services.neoforge.NeoPlatformService")
public class BumblezonePlatformServiceMixin {
    @Inject(method = "isDimensionAllowed", at = @At("HEAD"), cancellable = true)
    void preventdimensionALlowed(ServerPlayer serverPlayer, ResourceKey<net.minecraft.world.level.Level> dimension, CallbackInfoReturnable<Boolean> cir) {
        ResourceKey<Level> currentDimension = serverPlayer.level().dimension();
        Scoreboard scoreboard = serverPlayer.getScoreboard();
        Objective objective = scoreboard.getObjective("preventDimensionChange");
        if (objective != null) {
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(serverPlayer, objective);
            int score = scoreAccess.get();
            if (score == 0 && (!currentDimension.equals(dimension)) && !(serverPlayer instanceof FakePlayer)) {
                System.out.println("Cancelled dimension change");
                cir.setReturnValue(false);
            }
        }
    }
}
