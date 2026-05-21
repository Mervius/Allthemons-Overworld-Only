package top.yuhh.dronecompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.neoforge.common.util.FakePlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.EnumSet;

@Mixin(targets = "com.thevortex.allthemodium.blocks.TeleportPad")
public abstract class TeleportPadMixin extends Block {

    public TeleportPadMixin(Properties properties) {
        super(properties);
    }

    @Unique
    private static Class<?> DRONE_CLASS;

    @Unique
    private static Method GET_FAKE_PLAYER;

    @Unique
    private static boolean droneCompat$initialized = false;

    @Unique
    private static void droneCompat$init() {

        if (droneCompat$initialized) {
            return;
        }
        droneCompat$initialized = true;

        try {
            DRONE_CLASS = Class.forName("me.desht.pneumaticcraft.api.drone.IDrone");
            GET_FAKE_PLAYER = DRONE_CLASS.getMethod("getFakePlayer");
        } catch (ReflectiveOperationException ignored) {
            DRONE_CLASS = null;
            GET_FAKE_PLAYER = null;
        }

    }

    @Shadow
    private int config() {
        return 0;
    }

    @Shadow
    private BlockPos findSafeExit(ServerLevel level, BlockPos entryPos) {
        return null;
    }

    @Shadow
    public static @Nullable ResourceKey<Level> getPartner(ResourceKey<Level> level, int packMode) {
        return null;
    }

    @Unique
    public void droneCompat$transferDrone(Entity drone, BlockPos pos) {

        MinecraftServer server = drone.getServer();
        if (server == null) {
            return;
        }

        ResourceKey<Level> dimKey = getPartner(drone.level().dimension(), config());
        if (dimKey == null) {
            return;
        }

        ServerLevel targetLevel = server.getLevel(dimKey);
        if (targetLevel == null) {
            return;
        }

        BlockPos targetPos = findSafeExit(targetLevel, pos);
        droneCompat$teleportDrone(drone, targetLevel, targetPos);
    }

    @Unique
    private void droneCompat$teleportDrone(Entity drone, ServerLevel level, BlockPos targetPos) {

        ChunkPos chunkPos = new ChunkPos(targetPos);

        level.getChunkSource().addRegionTicket(
                TicketType.PORTAL,
                chunkPos,
                1,
                targetPos
        );

        level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, true);

        level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                targetPos.getX(), targetPos.getY(), targetPos.getZ(),
                0, 1, 0);

        drone.teleportTo(level,
                targetPos.getX() + 0.5D, targetPos.getY() + 0.25D, targetPos.getZ() + 0.5D,
                EnumSet.noneOf(RelativeMovement.class), drone.getYRot(), drone.getXRot());
    }

    @SuppressWarnings("all")
    @Inject(method = "teleport", at = @At("HEAD"))
    private void teleportPlayer(ServerPlayer player, ServerLevel level, BlockPos targetPos, CallbackInfo ci) {

        ChunkPos chunkPos = new ChunkPos(targetPos);
        level.getChunkSource().addRegionTicket(
                TicketType.PORTAL,
                chunkPos,
                1,
                targetPos
        );
        level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, true);
    }

    @SuppressWarnings("all")
    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    private void droneUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        droneCompat$init();

        if (DRONE_CLASS == null || GET_FAKE_PLAYER == null) {
            return;
        }

        if (!(player instanceof FakePlayer) || !player.isShiftKeyDown()) {
            return;
        }

        AABB box = new AABB(pos).inflate(50);

        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {

            if (DRONE_CLASS.isInstance(entity)) {
                try {
                    Object fp = GET_FAKE_PLAYER.invoke(entity);

                    if (fp == player) {
                        droneCompat$transferDrone(entity, pos);
                        level.addAlwaysVisibleParticle(
                                ParticleTypes.SOUL_FIRE_FLAME,
                                pos.getX(), pos.getY() + 1, pos.getZ(),
                                0, 1, 0);
                        return;

                    }
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

}
