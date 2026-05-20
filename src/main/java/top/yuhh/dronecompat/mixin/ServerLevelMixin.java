package top.yuhh.dronecompat.mixin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import top.yuhh.dronecompat.api.TrainerSpawnerAPI;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {

    protected ServerLevelMixin(WritableLevelData writeableLevelData, ResourceKey<Level> levelKey, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeHolder, Supplier<ProfilerFiller> profilerFillerProvider, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(writeableLevelData, levelKey, registryAccess, dimensionTypeHolder, profilerFillerProvider, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Shadow
    @Nullable
    public abstract EndDragonFight getDragonFight();

    @Shadow
    public Iterable<Entity> getAllEntities() {
        return null;
    }

    @Unique
    private static Class<?> BETTER_DRAGON_FIGHT_CLASS;

    @Unique
    private static Class<?> DRONE_CLASS;

    @Unique
    private static Class<?> RCT_CLASS;

    @Unique
    private static Method DO_INITIAL_DRAGON_SPAWN;

    @Unique
    private static Method GET_CONFIG;

    @Unique
    private static Method GET_DRAGON_RESPAWN_STAGE;

    @Unique
    private static Method GET_FAKE_PLAYER;

    @Unique
    private static Method GET_INSTANCE;

    @Unique
    private static Method GET_MANAGER;

    @Unique
    private static Method GET_OWNER;

    @Unique
    private static Method GET_SPAWN_COUNT;

    @Unique
    private static Method GET_SPAWNER;

    @Unique
    private static Method HAS_DRAGON_EVER_SPAWNED;

    @Unique
    private static Method MAX_SPAWNS;

    @Unique
    private static Method RELOAD_REQUIRED;

    @Unique
    private static Method TICKS_CONFIG;

    @Unique
    private static Method TICKS_MAX;

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
            GET_OWNER = DRONE_CLASS.getMethod("getOwner");
        } catch (ReflectiveOperationException e) {
            DRONE_CLASS = null;
            GET_FAKE_PLAYER = null;
            GET_OWNER = null;
        }

        try {
            BETTER_DRAGON_FIGHT_CLASS = Class.forName("com.yungnickyoung.minecraft.betterendisland.world.IBetterDragonFight");
            DO_INITIAL_DRAGON_SPAWN = BETTER_DRAGON_FIGHT_CLASS.getMethod("doInitialDragonSpawn");
            GET_DRAGON_RESPAWN_STAGE = BETTER_DRAGON_FIGHT_CLASS.getMethod("getDragonRespawnStage");
            HAS_DRAGON_EVER_SPAWNED = BETTER_DRAGON_FIGHT_CLASS.getMethod("hasDragonEverSpawned");
        } catch (ReflectiveOperationException e) {
            BETTER_DRAGON_FIGHT_CLASS = null;
            DO_INITIAL_DRAGON_SPAWN = null;
            GET_DRAGON_RESPAWN_STAGE = null;
            HAS_DRAGON_EVER_SPAWNED = null;
        }

        try {
            RCT_CLASS = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            GET_CONFIG = RCT_CLASS.getMethod("getServerConfig");
            GET_INSTANCE = RCT_CLASS.getMethod("getInstance");
            GET_MANAGER = RCT_CLASS.getMethod("getTrainerManager");
            GET_SPAWNER = RCT_CLASS.getMethod("getTrainerSpawner");
            Class<?> managerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager");
            RELOAD_REQUIRED = managerClass.getMethod("isReloadRequired");
            Class<?> configClass = Class.forName("com.gitlab.srcmc.rctmod.api.config.IServerConfig");
            MAX_SPAWNS = configClass.getMethod("maxTrainersPerPlayer");
            TICKS_CONFIG = configClass.getMethod("spawnIntervalTicks");
            TICKS_MAX = configClass.getMethod("spawnIntervalTicksMaximum");
            Class<?> spawnerClass = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerSpawner");
            GET_SPAWN_COUNT = spawnerClass.getMethod("getSpawnCount", UUID.class);
        } catch (ReflectiveOperationException e) {
            RCT_CLASS = null;
            GET_CONFIG = null;
            GET_INSTANCE = null;
            GET_MANAGER = null;
            GET_SPAWNER = null;
            RELOAD_REQUIRED = null;
            MAX_SPAWNS = null;
            TICKS_CONFIG = null;
            TICKS_MAX = null;
            GET_SPAWN_COUNT = null;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickForDrones(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        droneCompat$init();

        if (DRONE_CLASS == null) {
            return;
        }

        draoneCompat$dragonFight();
        droneCompat$spawnTrainers();
    }

    @Unique
    void draoneCompat$dragonFight() {
        if (BETTER_DRAGON_FIGHT_CLASS == null) {
            return;
        }
        if (!dimension().location().equals(BuiltinDimensionTypes.END.location()) || getDragonFight() != null) {
            return;
        }

        Object betterDragonFight = this.getDragonFight();
        if (!BETTER_DRAGON_FIGHT_CLASS.isInstance(betterDragonFight))  {
            return;
        }

        try {
            Boolean spawned = (Boolean) HAS_DRAGON_EVER_SPAWNED.invoke(betterDragonFight);
            Object stage = GET_DRAGON_RESPAWN_STAGE.invoke(betterDragonFight);

            if (Boolean.FALSE.equals(spawned) && stage == null && getGameTime() % 5 == 0) {
                AABB box = new AABB(-50, this.getMinBuildHeight(), -50, 50, this.getMaxBuildHeight(), 50);

                for (Entity drone : getEntitiesOfClass(Entity.class, box)) {
                    if (DRONE_CLASS.isInstance(drone)) {
                        if (drone.position().horizontalDistanceSqr() < 625) {
                            DO_INITIAL_DRAGON_SPAWN.invoke(betterDragonFight);
                            return;
                        }
                    }
                }

            }

        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Unique
    void droneCompat$spawnTrainers() {
        if (RCT_CLASS == null) {
            return;
        }

        try {
            Object rctInstance = GET_INSTANCE.invoke(RCT_CLASS);
            Object rctManager = GET_MANAGER.invoke(rctInstance);
            Boolean reload = (Boolean) RELOAD_REQUIRED.invoke(rctManager);
            if (!reload) {
                Object rctConfig = GET_CONFIG.invoke(rctInstance);
                Object trainers = GET_SPAWNER.invoke(rctInstance);

                int maxSpawns = (int) MAX_SPAWNS.invoke(rctConfig);
                int tickRange = Math.max(0, (int) TICKS_MAX.invoke(rctConfig) - (int) TICKS_CONFIG.invoke(rctConfig));

                if (maxSpawns > 0) {
                    ArrayList<Player> owners = new ArrayList<>();
                    for (Entity e : getAllEntities()) {

                        if (DRONE_CLASS.isInstance(e)) {
                            Player owner = (Player) GET_OWNER.invoke(e);
                            Player fakePlayer = (Player) GET_FAKE_PLAYER.invoke(e);

                            if (owner != null && fakePlayer != null) {
                                if (owner.level() == e.level() || owners.contains(owner)) {
                                    continue;
                                }
                                owners.add(owner);
                                int ownerSpawnCount = (int) GET_SPAWN_COUNT.invoke(trainers, owner.getUUID());
                                int ticks = (int) TICKS_CONFIG.invoke(rctConfig) + (tickRange * maxSpawns > 1 ? Math.min(1, ownerSpawnCount / Math.clamp(maxSpawns - 1, 1, Integer.MAX_VALUE)) : 1);
                                if (ticks == 0 || (owner.tickCount % ticks == 0)) {
                                    TrainerSpawnerAPI.attemptSpawnForDrone(trainers, e, owner, rctInstance, rctManager, rctConfig);
                                }
                            }

                        }
                    }
                }
            }
        } catch (InvocationTargetException | IllegalAccessException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }
}
