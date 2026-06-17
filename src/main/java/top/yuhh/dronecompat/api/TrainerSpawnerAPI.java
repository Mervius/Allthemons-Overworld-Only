package top.yuhh.dronecompat.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import top.yuhh.dronecompat.interfaces.TrainerSpawnerInterface;

public final class TrainerSpawnerAPI {

    public static void attemptSpawnForDrone(Object spawner, Entity drone, Player owner, Object rctInstance, Object rctManager, Object rctConfig) {
        ((TrainerSpawnerInterface) spawner).droneCompat$attemptSpawnForDrone(drone, owner, rctInstance, rctManager, rctConfig);
    }

    public static Object attemptSpawnForDrone(Object spawner, Entity drone, Player owner, String trainerId, BlockPos pos, boolean setHome, boolean noOrigin, boolean guaruantee, double globalChance, double globalChanceMin, Object rctInstance, Object rctManager) {
        return ((TrainerSpawnerInterface) spawner).droneCompat$attemptSpawnForDrone(spawner, drone, owner, trainerId, pos, setHome, noOrigin, guaruantee, globalChance, globalChanceMin, rctInstance, rctManager);
    }
}