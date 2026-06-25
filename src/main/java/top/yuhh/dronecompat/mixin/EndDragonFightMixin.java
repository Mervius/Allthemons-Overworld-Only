package top.yuhh.dronecompat.mixin;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@Mixin(value = EndDragonFight.class, priority = 500)
public class EndDragonFightMixin {

    @Shadow
    @Final
    private ServerBossEvent dragonEvent;

    @Shadow
    private boolean dragonKilled;

    @Shadow
    private int ticksSinceLastPlayerScan;

    @Shadow
    private void updatePlayers() {}

    @Shadow
    private boolean isArenaLoaded() {
        return false;
    }

    @Shadow
    private boolean needsStateScanning;

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Nullable
    private List<EndCrystal> respawnCrystals;

    @Shadow
    public void tryRespawn() {}

    @Shadow
    private int respawnTime;

    @Shadow
    @Nullable
    private UUID dragonUUID;

    @Shadow
    private void findOrCreateDragon() {}

    @Shadow
    private int ticksSinceCrystalsScanned;

    @Shadow
    private void updateCrystalCount() {}

    @Shadow
    private int ticksSinceDragonSeen;

    @Unique
    private static Class<?> DRONE_CLASS;

    @Unique
    private static Method STATE;

    @Unique
    private static Method TICK;

    @Unique
    private static Field STAGE;

    @Unique
    private static Field SPAWN;

    @Unique
    private static boolean droneCompat$initialized = false;

    @Unique
    private void droneCompat$init() {

        if (droneCompat$initialized) {
            return;
        }
        droneCompat$initialized = true;

        try {
            DRONE_CLASS = Class.forName("me.desht.pneumaticcraft.api.drone.IDrone");
            STATE = ((Object) this).getClass().getDeclaredMethod("scanForInitialState");
            STATE.setAccessible(true);
            TICK = Class.forName("com.yungnickyoung.minecraft.betterendisland.world.DragonRespawnStage").getMethod("tick", ServerLevel.class, EndDragonFight.class, List.class, int.class);
            STAGE = ((Object) this).getClass().getDeclaredField("bei$dragonRespawnStage");
            STAGE.setAccessible(true);
            SPAWN = ((Object) this).getClass().getDeclaredField("bei$hasDragonEverSpawned");
            SPAWN.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            DRONE_CLASS = null;
            STATE = null;
            TICK = null;
            STAGE = null;
            SPAWN = null;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tickFight(CallbackInfo ci) {
        droneCompat$init();

        if (DRONE_CLASS == null) {
            return;
        }

        try {

            this.dragonEvent.setVisible(!this.dragonKilled && (boolean)SPAWN.get(this));

            if (this.dragonEvent.getPlayers().isEmpty()) {

                if (++this.ticksSinceLastPlayerScan >= 20) {
                    this.updatePlayers();
                    this.ticksSinceLastPlayerScan = 0;
                }

                boolean droneNearFight = false;
                AABB box = new AABB(-200, this.level.getMinBuildHeight(), -200, 200, this.level.getMaxBuildHeight(), 200);
                for (Entity drone : this.level.getEntitiesOfClass(Entity.class, box)) {
                    if (DRONE_CLASS.isInstance(drone)) {
                        droneNearFight = true;
                        break;
                    }
                }

                if (droneNearFight) {
                    this.level.getChunkSource().addRegionTicket(TicketType.DRAGON, new ChunkPos(0, 0), 9, Unit.INSTANCE);
                    boolean isArenaLoaded = this.isArenaLoaded();
                    if (this.needsStateScanning && isArenaLoaded) {
                        STATE.invoke(this);
                        this.needsStateScanning = false;
                    }

                    if (STAGE.get(this) != null) {
                        if (this.respawnCrystals == null && isArenaLoaded) {
                            STAGE.set(this, null);
                            this.tryRespawn();
                        }

                        TICK.invoke(STAGE.get(this), this.level, (EndDragonFight) (Object)this, this.respawnCrystals, this.respawnTime++);
                    }

                    if (!this.dragonKilled) {
                        if ((this.dragonUUID == null || ++this.ticksSinceDragonSeen >= 1200) && isArenaLoaded &&  (boolean)SPAWN.get(this)) {
                            this.findOrCreateDragon();
                            this.ticksSinceDragonSeen = 0;
                        }

                        if (++this.ticksSinceCrystalsScanned >= 100 && isArenaLoaded) {
                            this.updateCrystalCount();
                            this.ticksSinceCrystalsScanned = 0;
                        }
                    }
                } else {
                    this.level.getChunkSource().removeRegionTicket(TicketType.DRAGON, new ChunkPos(0, 0), 9, Unit.INSTANCE);
                }
                ci.cancel();
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}