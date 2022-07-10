package dev.bithole.debugrenderers.mixin;

import dev.bithole.debugrenderers.SpawnInfoSender;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin {

    @Inject(at = @At("HEAD"), method = "tickChunks()V")
    private void tickChunksStart(CallbackInfo info) {
        SpawnInfoSender.clear();
    }

    @Inject(at = @At("TAIL"), method = "tickChunks()V")
    private void tickChunksEnd(CallbackInfo info) {
        SpawnInfoSender.send((ServerWorld)((ServerChunkManager)(Object)this).getWorld());
    }

}
