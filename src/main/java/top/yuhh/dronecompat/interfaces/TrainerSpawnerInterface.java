package top.yuhh.dronecompat.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public interface TrainerSpawnerInterface {

    default void droneCompat$attemptSpawnForDrone(Entity e, Player owner, Object rctInstance, Object rctManager, Object rctConfig) {
    }

}
