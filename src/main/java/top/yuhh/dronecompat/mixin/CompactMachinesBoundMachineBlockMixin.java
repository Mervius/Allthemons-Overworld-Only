package top.yuhh.dronecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;

import net.neoforged.neoforge.common.util.FakePlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "dev.compactmods.machines.machine.block.BoundCompactMachineBlock")
public class CompactMachinesBoundMachineBlockMixin {

    @Unique
    private static Class<?> COMPACT_CLASS;

    @Unique
    private static Class<?> ROOM_ENTRY_RESULT;

    @Unique
    private static Enum<?> FAILED;

    @Unique
    private static Method COMPACT_DIM;


    @Unique
    private static boolean droneCompat$initialized = false;

    @Unique
    private static void droneCompat$init() {

        if (droneCompat$initialized) {
            return;
        }
        droneCompat$initialized = true;

        try {
            COMPACT_CLASS = Class.forName("dev.compactmods.machines.api.dimension.CompactDimension");
            COMPACT_DIM = COMPACT_CLASS.getMethod("forServer", MinecraftServer.class);
            ROOM_ENTRY_RESULT = Class.forName("dev.compactmods.machines.api.room.history.RoomEntryResult");
            FAILED = Enum.valueOf((Class<Enum>) ROOM_ENTRY_RESULT, "FAILED_ROOM_INVALID");
        } catch (ReflectiveOperationException e) {
            COMPACT_CLASS = null;
            COMPACT_DIM = null;
            ROOM_ENTRY_RESULT = null;
            FAILED = null;
        }
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Ldev/compactmods/machines/room/RoomHelper;teleportPlayerIntoMachine(Lnet/minecraft/world/level/Level;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/GlobalPos;Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<?> teleportPlayerIntoMachine(Level machineLevel, ServerPlayer player, GlobalPos machinePos, String roomCode, Operation<CompletableFuture<?>> original) {
        droneCompat$init();
        if (COMPACT_CLASS != null) {
            try {
                ResourceKey<Level> currentDimension = player.level().dimension();
                ResourceKey<Level> compactDimension = ((ServerLevel) COMPACT_DIM.invoke(null, player.server)).dimension();
                Scoreboard scoreboard = player.getScoreboard();
                Objective objective = scoreboard.getObjective("preventDimensionChange");
                if (objective != null) {
                    ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(player, objective);
                    int score = scoreAccess.get();
                    if (score == 0 && (!currentDimension.equals(compactDimension)) && !(player instanceof FakePlayer)) {
                        System.out.println("Cancelled dimension change");
                        return CompletableFuture.completedFuture(FAILED);
                    }
                }
                return original.call(machineLevel, player, machinePos, roomCode);
            } catch (InvocationTargetException | IllegalAccessException e) {
                return original.call(machineLevel, player, machinePos, roomCode);
            }

        } return original.call(machineLevel, player, machinePos, roomCode);
    }
}
