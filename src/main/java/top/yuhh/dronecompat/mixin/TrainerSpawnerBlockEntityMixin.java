package top.yuhh.dronecompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.yuhh.dronecompat.api.TrainerSpawnerAPI;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Mixin(targets = "com.gitlab.srcmc.rctmod.world.blocks.entities.TrainerSpawnerBlockEntity")
public class TrainerSpawnerBlockEntityMixin {


    @Shadow
    private double minPlayerDistance;

    @Shadow
    private Set<String> trainerIds;

    @Shadow
    public Set<String> getTrainerIds() {
        return this.trainerIds;
    }

    @Shadow
    private AABB aabb;

    @Unique
    private static Class<?> DRONE_CLASS;

    @Unique
    private static Class<?> RCT_CLASS;

    @Unique
    private static Method BOOSTED;

    @Unique
    private static Method GET_CONFIG;

    @Unique
    private static Method GET_FAKE_PLAYER;

    @Unique
    private static Method GET_INSTANCE;

    @Unique
    private static Method GET_MANAGER;

    @Unique
    private static Method GET_OWNER;

    @Unique
    private static Method GET_SPAWNER;

    @Unique
    private static Method SET_OWNER;

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
            RCT_CLASS = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            BOOSTED = Class.forName("com.gitlab.srcmc.rctmod.world.blocks.TrainerSpawnerBlock").getMethod("isBoosted", BlockState.class);
            GET_CONFIG = RCT_CLASS.getMethod("getServerConfig");
            GET_FAKE_PLAYER = DRONE_CLASS.getMethod("getFakePlayer");
            GET_INSTANCE = RCT_CLASS.getMethod("getInstance");
            GET_MANAGER = RCT_CLASS.getMethod("getTrainerManager");
            GET_OWNER = DRONE_CLASS.getMethod("getOwner");
            GET_SPAWNER = RCT_CLASS.getMethod("getTrainerSpawner");
            SET_OWNER = Class.forName("com.gitlab.srcmc.rctmod.world.blocks.entities.TrainerSpawnerBlockEntity").getMethod("setOwner", Class.forName("com.gitlab.srcmc.rctmod.world.entities.TrainerMob"));
        } catch (ReflectiveOperationException e) {
            DRONE_CLASS = null;
            RCT_CLASS = null;
            BOOSTED = null;
            GET_CONFIG = null;
            GET_FAKE_PLAYER = null;
            GET_INSTANCE = null;
            GET_MANAGER = null;
            GET_OWNER = null;
            GET_SPAWNER = null;
        }
    }

    @Inject(method = "attemptSpawn", at = @At("HEAD"))
    private void attemptSpawnForDrone(Level level, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        droneCompat$init();

        if (DRONE_CLASS == null) {
            return;
        }

        try {
            boolean guarantee = (boolean) BOOSTED.invoke(null, blockState);
            Object rctInstance = GET_INSTANCE.invoke(RCT_CLASS);
            Object rctManager = GET_MANAGER.invoke(rctInstance);
            Object rctConfig = GET_CONFIG.invoke(rctInstance);
            Object trainers = GET_SPAWNER.invoke(rctInstance);
            ArrayList<String> trainerIDs = new ArrayList<>(this.getTrainerIds());

            Collections.shuffle(trainerIDs);

            Vec3 pos = blockPos.getCenter();
            ArrayList<Player> owners = new ArrayList<>();
            for (Entity drone : level.getEntitiesOfClass(Entity.class, aabb)) {
                if (DRONE_CLASS.isInstance(drone) && !(drone.distanceToSqr(pos) < Math.pow(minPlayerDistance, (double) 2.0F) / (double) 2.0F)) {
                    Player owner = (Player) GET_OWNER.invoke(drone);
                    Player fakePlayer = (Player) GET_FAKE_PLAYER.invoke(drone);

                    if (owner != null && fakePlayer != null) {
                        if (owner.level() == drone.level() || owners.contains(owner)) {
                            continue;
                        }
                        owners.add(owner);

                        for (String trainerId : trainerIDs) {
                        Object m = TrainerSpawnerAPI.attemptSpawnForDrone(trainers, drone, owner, trainerId, ((BlockEntity)(Object)this).getBlockPos().above(), true, true, guarantee, (double)1.0F, (double)1.0F, rctInstance, rctManager);
                        if (m != null) {
                                SET_OWNER.invoke(this, m);
                            }
                        }
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
