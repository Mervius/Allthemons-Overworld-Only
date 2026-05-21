package top.yuhh.dronecompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;

import net.neoforged.neoforge.common.util.FakePlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.necro.raid.dens.common.blocks.block.RaidCrystalBlock")
public class RaidCrystalBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    protected void useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        ResourceKey<Level> currentDimension = player.level().dimension();
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("preventDimensionChange");
        if (objective != null) {
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(player, objective);
            int score = scoreAccess.get();
            if (score == 0 && !(player instanceof FakePlayer)) {
                System.out.println("Cancelled dimension change");
                cir.setReturnValue(InteractionResult.SUCCESS);
                cir.cancel();
            }
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    protected void useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        ResourceKey<Level> currentDimension = player.level().dimension();
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("preventDimensionChange");
        if (objective != null) {
            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(player, objective);
            int score = scoreAccess.get();
            if (score == 0 && !(player instanceof FakePlayer)) {
                System.out.println("Cancelled dimension change");
                cir.setReturnValue(ItemInteractionResult.SUCCESS);
                cir.cancel();
            }
        }
    }
}
