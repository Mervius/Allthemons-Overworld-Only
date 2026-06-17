package top.yuhh.dronecompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.common.util.FakePlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import top.yuhh.dronecompat.DroneCompat;
import top.yuhh.dronecompat.interfaces.TrainerSpawnerInterface;

@Mixin(targets = "com.gitlab.srcmc.rctmod.api.service.TrainerSpawner")
public abstract class TrainerSpawnerMixin implements TrainerSpawnerInterface {

    @Shadow
    private boolean canSpawnFor(Player player, boolean noOrigin, double globalChance, double globalChanceMin) {
        return false;
    }

    @Shadow
    private static boolean canSpawnAt(Level level, BlockPos blockPos) {
        return false;
    }

    @Shadow
    public boolean isMarkedAt(Level level, BlockPos pos) {
        return false;
    }

    @Shadow
    private boolean isUnique(String identity, Level level, BlockPos pos) {
        return false;
    }

    @Shadow
    public abstract BlockPos nextPos(Player player);

    @Unique
    private static Method NEXT_SPAWN_CANDIDATE;

    @Unique
    private static Method GET_DATA;

    @Unique
    private static Method SPAWN_CHANCE;

    @Unique
    private static Method SPAWN_CHANCE_MIN;

    @Unique
    private static Method SPAWN_FOR;

    @Unique
    private static Method SPAWN_FOR2;

    @Unique
    private static Method VALID_ID;

    @Unique
    private static boolean droneCompat$initialized = false;

    @Unique
    private static void droneCompat$init() {

        if (droneCompat$initialized) {
            return;
        }
        droneCompat$initialized = true;

        try {
            NEXT_SPAWN_CANDIDATE = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerSpawner").getDeclaredMethod("nextSpawnCandidate", Player.class, BlockPos.class);
            NEXT_SPAWN_CANDIDATE.setAccessible(true);
            GET_DATA = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager").getMethod("getData", String.class);
            Class<?> configClass = Class.forName("com.gitlab.srcmc.rctmod.api.config.IServerConfig");
            SPAWN_CHANCE = configClass.getMethod("globalSpawnChance");
            SPAWN_CHANCE_MIN = configClass.getMethod("globalSpawnChanceMinimum");
            SPAWN_FOR = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerSpawner").getDeclaredMethod("spawnFor", Player.class, String.class, BlockPos.class);
            SPAWN_FOR2 = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerSpawner").getDeclaredMethod("spawnFor", Player.class, String.class, BlockPos.class, boolean.class, boolean.class);
            VALID_ID = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager").getMethod("isValidId", String.class);
        } catch (ReflectiveOperationException e) {
            System.out.println("AAAAA " + e);
            NEXT_SPAWN_CANDIDATE = null;
            GET_DATA = null;
            SPAWN_CHANCE = null;
            SPAWN_CHANCE_MIN = null;
            SPAWN_FOR = null;
            SPAWN_FOR2 = null;
            VALID_ID = null;
        }
    }

    @Override
    public void droneCompat$attemptSpawnForDrone(Entity drone, Player owner, Object rct, Object rctManager, Object rctConfig) {
        droneCompat$init();

        if (NEXT_SPAWN_CANDIDATE == null) {
            return;
        }

        try {
            double spawnChance = (double) SPAWN_CHANCE.invoke(rctConfig);
            double spawnChanceMin = (double) SPAWN_CHANCE_MIN.invoke(rctConfig);
            if (this.canSpawnFor(owner, false, spawnChance, spawnChanceMin)) {
                FakePlayer tempFake = new FakePlayer((ServerLevel) drone.level(), owner.getGameProfile());
                tempFake.setPos(drone.position());
                tempFake.setUUID(owner.getUUID());
                for (int i = 0; i < 8; ++i) {
                    BlockPos pos = this.nextPos(tempFake);
                    if (pos != null && !this.isMarkedAt(drone.level(), pos)) {
                        SpawnCandidateAccessor tempCandidate = (SpawnCandidateAccessor) NEXT_SPAWN_CANDIDATE.invoke(this, tempFake, pos);
                        if (tempCandidate != null) {
                            Object mob = SPAWN_FOR.invoke(this, tempFake, tempCandidate.getId(), pos);
                            DroneCompat.LOGGER.info("Natural trainer spawn: {}", mob);
                        }

                        return;
                    }
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public Object droneCompat$attemptSpawnForDrone(Object spawner, Entity drone, Player owner, String trainerId, BlockPos pos, boolean setHome, boolean noOrigin, boolean guarantee, double globalChance, double globalChanceMin, Object rctInstance, Object rctManager) {
        droneCompat$init();

        if (NEXT_SPAWN_CANDIDATE == null) {
            return null;
        }

        Level level = drone.level();
        try {
            boolean valid = (boolean) VALID_ID.invoke(rctManager, trainerId);
            if (valid && (noOrigin || !this.isMarkedAt(level, pos)) && canSpawnAt(level, pos) && this.canSpawnFor(owner, noOrigin, globalChance, globalChanceMin)) {
                Object tmd = GET_DATA.invoke(rctManager,trainerId);
                Method team = Class.forName("com.gitlab.srcmc.rctmod.api.data.pack.TrainerMobData").getMethod("getTrainerTeam");
                Method identity = Class.forName("com.gitlab.srcmc.rctmod.api.data.pack.TrainerTeam").getMethod("getIdentity");
                Method chance = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerSpawner").getDeclaredMethod("computeChance", Player.class, String.class, Class.forName("com.gitlab.srcmc.rctmod.api.data.pack.TrainerMobData"));
                chance.setAccessible(true);
                double chance2 = (double) chance.invoke(this, owner, trainerId, tmd);
                FakePlayer tempFake = new FakePlayer((ServerLevel) drone.level(), owner.getGameProfile());
                tempFake.setPos(drone.position());
                tempFake.setUUID(owner.getUUID());
                if (tmd != null && this.isUnique((String) identity.invoke(team.invoke(tmd)), level, pos) && (guarantee || chance2 >= owner.getRandom().nextDouble())) {
                    Object mob = SPAWN_FOR2.invoke(this, tempFake, trainerId, pos, setHome, noOrigin);
                    DroneCompat.LOGGER.info("Unnatural trainer spawn: {}", mob);
                    return mob;
                }
            }

            return null;
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
