package top.yuhh.dronecompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Random;

import top.yuhh.dronecompat.DroneCompat;

@Mixin(targets = "ultra.wormholes.event.WormholeEventManager")
public class WormholeEventManagerMixin {

    @Shadow
    private static @Nullable MinecraftServer server;

    @Shadow
    @Final
    private static Random RANDOM;

    @Shadow
    private static @Nullable BlockPos findSpawnPos(ServerLevel level, BlockPos origin) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static void scheduleNextIfEnabled(String reason) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static boolean openEvent(ServerLevel level, BlockPos spawnPos, @Nullable String forcedUltraBeastId) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Unique
    private static Class<?> DRONE_CLASS;

    @Unique
    private static Class<?> UltraWormholeModReference;

    @Unique
    private static Method GET_OWNER;

    @Unique
    private static Field THE_OTHER;

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
            GET_OWNER = DRONE_CLASS.getMethod("getOwner");
            UltraWormholeModReference = Class.forName("com.thevortex.allthemodium.reference.Reference");
            THE_OTHER = UltraWormholeModReference.getField("THE_OTHER");
        } catch (ReflectiveOperationException e) {
            DRONE_CLASS = null;
            GET_OWNER = null;
            UltraWormholeModReference = null;
            THE_OTHER = null;
        }
    }

    @Inject(method = "tryOpenEvent(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private static void tryOpen(String forcedUltraBeastId, CallbackInfoReturnable<Boolean> cir) {
        droneCompat$init();

        if (DRONE_CLASS == null) {
            return;
        }

        try {
            if (server != null) {
                ServerLevel level = server.getLevel((ResourceKey<Level>) THE_OTHER.get(null));
                if (level != null) {
                    ArrayList<Entity> Drones = new ArrayList<>();
                    for (Entity entity : level.getEntities().getAll()) {
                        if (DRONE_CLASS.isInstance(entity)) {
                            Player owner = (Player) GET_OWNER.invoke(entity);

                            if (owner != null) {
                                if (owner.level() == entity.level() || Drones.contains(entity)) {
                                    continue;
                                }
                                Drones.add(entity);
                            }
                        }
                    }
                    if (!Drones.isEmpty()) {
                        Entity target = Drones.get(RANDOM.nextInt(Drones.size()));
                        if (target != null) {

                            BlockPos spawnPos = findSpawnPos(level, target.blockPosition());
                            if (spawnPos == null) {
                                DroneCompat.LOGGER.warn("Could not find valid spawn position near {} — skipping event.", target.getName().getString());
                                scheduleNextIfEnabled("auto-spawn skipped due to invalid position");
                                cir.cancel();
                                cir.setReturnValue(false);
                            } else if (!openEvent(level, spawnPos, forcedUltraBeastId)) {
                                scheduleNextIfEnabled("auto-spawn failed during event open");
                                cir.cancel();
                                cir.setReturnValue(false);
                            } else {
                                cir.cancel();
                                cir.setReturnValue(true);
                            }
                        }
                    }
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }
}
