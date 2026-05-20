package top.yuhh.dronecompat.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static top.yuhh.dronecompat.DroneCompat.MODID;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @Inject(method = "renderTablistScore", at = @At("HEAD"), cancellable = true)
    public void renderScore(Objective objective, int y, @Coerce Object displayEntry, int minX, int maxX, UUID playerUuid, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (objective.getName().equals("preventDimensionChange")) {

            int score = ((ScoreDisplayEntryAccessor) displayEntry).getScore();
            ResourceLocation icon = score == 0 ? ResourceLocation.fromNamespaceAndPath(MODID, "icon/portal_1") : ResourceLocation.fromNamespaceAndPath(MODID, "icon/portal_0");

            ScoreDisplayEntryAccessor entry = (ScoreDisplayEntryAccessor) displayEntry;
            guiGraphics.blitSprite(icon, maxX - entry.getWidth() - 3, y, 8, 8);

            ci.cancel();
        }
    }
}
