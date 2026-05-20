package top.yuhh.dronecompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.components.PlayerTabOverlay$ScoreDisplayEntry")
public interface ScoreDisplayEntryAccessor {

    @Accessor("score")
    int getScore();

    @Accessor("scoreWidth")
    int getWidth();

}
