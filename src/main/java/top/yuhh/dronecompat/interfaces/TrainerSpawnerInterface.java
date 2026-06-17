package top.yuhh.dronecompat.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface TrainerSpawnerInterface {

    default void droneCompat$attemptSpawnForDrone(Entity e, Player owner, Object rctInstance, Object rctManager, Object rctConfig) {
    }

    default Object droneCompat$attemptSpawnForDrone(Object spawner, Entity drone, Player owner, String trainerId, BlockPos pos, boolean setHome, boolean noOrigin, boolean guaruantee, double globalChance, double globalChanceMin, Object rctInstance, Object rctManager) {
        return null;
    }

}
