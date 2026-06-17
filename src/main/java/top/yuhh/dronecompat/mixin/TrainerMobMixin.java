package top.yuhh.dronecompat.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "com.gitlab.srcmc.rctmod.world.entities.TrainerMob")
public class TrainerMobMixin {
    /**
     * @author Daniel Hagemeier
     * @reason Allow Trainers to change dimensions
     */
    @Overwrite
    public boolean canChangeDimensions(Level level1, Level level2) {
        return true;
    }

}
