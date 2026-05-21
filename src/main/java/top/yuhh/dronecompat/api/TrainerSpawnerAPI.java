package top.yuhh.dronecompat.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import top.yuhh.dronecompat.interfaces.TrainerSpawnerInterface;

public final class TrainerSpawnerAPI {

    public static void attemptSpawnForDrone(Object spawner, Entity drone, Player owner, Object rctInstance, Object rctManager, Object rctConfig) {
        ((TrainerSpawnerInterface) spawner).droneCompat$attemptSpawnForDrone(drone, owner, rctInstance, rctManager, rctConfig);
    }
}