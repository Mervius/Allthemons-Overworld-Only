package top.yuhh.dronecompat.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(Level level, BlockPos blockPos, float yRot, GameProfile profile) {
        super(level, blockPos, yRot, profile);
    }

    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Inject(method = "changeDimension", at = @At("HEAD"), cancellable = true)
    public void changeDimensionA(DimensionTransition dimensionTransition, CallbackInfoReturnable<Entity> cir) {
        ResourceKey<Level> currentDimension = this.level().dimension();
        ResourceKey<Level> dimension = dimensionTransition.newLevel().dimension();
        Scoreboard scoreboard = this.getScoreboard();
        Objective objective = scoreboard.getObjective("preventDimensionChange");
        if (objective != null) {
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(this, objective);
            int score = scoreAccess.get();
            if (score == 0 && (!currentDimension.equals(dimension)) && !((Player) this instanceof FakePlayer)) {
                System.out.println("Cancelled dimension change");
                cir.cancel();
            }
        }
    }
}
