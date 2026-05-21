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

import top.yuhh.dronecompat.interfaces.TrainerSpawnerInterface;

@Mixin(targets = "com.gitlab.srcmc.rctmod.api.service.TrainerSpawner")
public abstract class TrainerSpawnerMixin implements TrainerSpawnerInterface {

    @Shadow
    private boolean canSpawnFor(Player player, boolean noOrigin, double globalChance, double globalChanceMin) {
        return false;
    }

    @Shadow
    public boolean isMarkedAt(Level level, BlockPos pos) {
        return false;
    }

    @Shadow
    public abstract BlockPos nextPos(Player player);

    @Unique
    private static Method NEXT_SPAWN_CANDIDATE;

    @Unique
    private static Method SPAWN_CHANCE;

    @Unique
    private static Method SPAWN_CHANCE_MIN;

    @Unique
    private static Method SPAWN_FOR;

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
            Class<?> configClass = Class.forName("com.gitlab.srcmc.rctmod.api.config.IServerConfig");
            SPAWN_CHANCE = configClass.getMethod("globalSpawnChance");
            SPAWN_CHANCE_MIN = configClass.getMethod("globalSpawnChanceMinimum");
            SPAWN_FOR = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerSpawner").getDeclaredMethod("spawnFor", Player.class, String.class, BlockPos.class);
        } catch (ReflectiveOperationException e) {
            NEXT_SPAWN_CANDIDATE = null;
            SPAWN_CHANCE = null;
            SPAWN_CHANCE_MIN = null;
            SPAWN_FOR = null;
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
                        }

                        return;
                    }
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }
}
