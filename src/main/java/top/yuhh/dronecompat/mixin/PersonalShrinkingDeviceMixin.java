package top.yuhh.dronecompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(targets = "dev.compactmods.machines.shrinking.PersonalShrinkingDevice")
public class PersonalShrinkingDeviceMixin {

    @Unique
    private static Class<?> API_CLASS;

    @Unique
    private static Class<?> ROOM_EXIT_RESULT;

    @Unique
    private static Enum<?> FAILED;

    @Unique
    private static Enum<?> OVERWORLD;

    @Unique
    private static Field ENTRY_POINT_FIELD;

    @Unique
    private static Field LOCATION_FIELD;

    @Unique
    private static Field POS_FIELD;

    @Unique
    private static Method API_METHOD;

    @Unique
    private static Method ENTRY_METHOD;

    @Unique
    private static Method HIST_METHOD;

    @Unique
    private static boolean droneCompat$initialized = false;

    @Unique
    private static void droneCompat$init() {

        if (droneCompat$initialized) {
            return;
        }
        droneCompat$initialized = true;

        try {
            API_CLASS = Class.forName("dev.compactmods.machines.api.room.history.IPlayerHistoryApi");
            ROOM_EXIT_RESULT = Class.forName("dev.compactmods.machines.api.room.history.RoomExitResult");
            FAILED = Enum.valueOf((Class<Enum>) ROOM_EXIT_RESULT, "FAILED_NOT_IN_COMPACT_DIM");
            OVERWORLD = Enum.valueOf((Class<Enum>) ROOM_EXIT_RESULT, "SUCCESS_WENT_TO_SPAWN");
            ENTRY_POINT_FIELD = Class.forName("dev.compactmods.machines.api.room.history.PlayerRoomHistoryEntry").getDeclaredField("entryPoint");
            ENTRY_POINT_FIELD.setAccessible(true);
            LOCATION_FIELD = Class.forName("dev.compactmods.machines.api.room.history.RoomEntryPoint").getDeclaredField("entryLocation");
            LOCATION_FIELD.setAccessible(true);
            POS_FIELD = Class.forName("dev.compactmods.machines.api.location.GlobalPosWithRotation").getDeclaredField("dimension");
            POS_FIELD.setAccessible(true);
            API_METHOD = Class.forName("dev.compactmods.machines.api.CompactMachines").getMethod("playerHistoryApi");
            ENTRY_METHOD = API_CLASS.getMethod("entryPoints");
            HIST_METHOD = Class.forName("dev.compactmods.machines.api.room.history.IPlayerEntryPointHistoryManager").getMethod("lastHistory", Player.class);
        } catch (ReflectiveOperationException e) {
            API_CLASS = null;
            ROOM_EXIT_RESULT = null;
            FAILED = null;
            OVERWORLD = null;
            ENTRY_POINT_FIELD = null;
            LOCATION_FIELD = null;
            POS_FIELD = null;
            API_METHOD = null;
            ENTRY_METHOD = null;
            HIST_METHOD = null;
        }
    }

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Ldev/compactmods/machines/room/RoomHelper;teleportPlayerOutOfRoom(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/concurrent/CompletableFuture;"))
    private static CompletableFuture<?> cancelUse(ServerPlayer serverPlayer, Operation<CompletableFuture<?>> original) {
        droneCompat$init();

        try {
            Object api = API_METHOD.invoke(null);
            Object manager = ENTRY_METHOD.invoke(api);
            Object hist = HIST_METHOD.invoke(manager, serverPlayer);

            Object histOptional = ((Optional<?>) hist).orElse(null);
            if (histOptional == null) {
                Class.forName("dev.compactmods.machines.util.PlayerUtil").getMethod("teleportPlayerToRespawnOrOverworld", MinecraftServer.class, ServerPlayer.class).invoke(null, serverPlayer.server, serverPlayer);
                return CompletableFuture.completedFuture(OVERWORLD);
            }
            Object entry = ENTRY_POINT_FIELD.get(histOptional);

            ResourceKey<Level> newDimension = (ResourceKey<Level>) POS_FIELD.get(LOCATION_FIELD.get(entry));
            if (newDimension == null) {
                Class.forName("dev.compactmods.machines.util.PlayerUtil").getMethod("teleportPlayerToRespawnOrOverworld", MinecraftServer.class, ServerPlayer.class).invoke(null, serverPlayer.server, serverPlayer);
                return CompletableFuture.completedFuture(OVERWORLD);
            }

            ResourceKey<Level> currentDimension = serverPlayer.level().dimension();
            Scoreboard scoreboard = serverPlayer.getScoreboard();
            Objective objective = scoreboard.getObjective("preventDimensionChange");
            if (objective != null) {
                ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(serverPlayer, objective);
                int score = scoreAccess.get();
                if (score == 0 && (!currentDimension.equals(newDimension)) && !(serverPlayer instanceof FakePlayer)) {
                    System.out.println("Cancelled dimension change");
                    return CompletableFuture.completedFuture(FAILED);
                }
            }

        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException |
                 NoSuchMethodException e) {
            return original.call(serverPlayer);
        }
        return original.call(serverPlayer);
    }
}