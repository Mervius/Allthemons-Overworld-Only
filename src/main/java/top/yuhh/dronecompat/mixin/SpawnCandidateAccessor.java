package top.yuhh.dronecompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.gitlab.srcmc.rctmod.api.service.TrainerSpawner$SpawnCandidate")
public interface SpawnCandidateAccessor {

    @Accessor("id")
    String getId();

}