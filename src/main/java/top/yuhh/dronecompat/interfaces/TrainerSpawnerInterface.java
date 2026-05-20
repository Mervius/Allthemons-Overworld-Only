package top.yuhh.dronecompat.interfaces;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface TrainerSpawnerInterface {

    default void droneCompat$attemptSpawnForDrone(Entity e, Player owner, Object rctInstance, Object rctManager, Object rctConfig) {
    }

}
