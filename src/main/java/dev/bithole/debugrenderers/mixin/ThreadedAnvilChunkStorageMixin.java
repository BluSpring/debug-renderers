package dev.bithole.debugrenderers.mixin;

import net.minecraft.server.network.ChunkDataSender;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkDataSender.class)
public class ThreadedAnvilChunkStorageMixin {

    @Inject(at = @At("TAIL"), method = "sendChunkData")
    private static void sendChunkDataPackets(ServerPlayNetworkHandler handler, ServerWorld world, WorldChunk chunk, CallbackInfo ci) {
        for(StructureStart start: chunk.getStructureStarts().values()) {
            DebugInfoSender.sendStructureStart((ServerWorld)chunk.getWorld(), start);
        }
    }

}