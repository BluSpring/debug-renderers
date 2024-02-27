package dev.bithole.debugrenderers.mixin;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.debug.PathfindingDebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathfindingDebugRenderer.class)
public class PathfindingDebugRendererMixin {

    @Shadow
    private static float getManhattanDistance(BlockPos pos, double x, double y, double z) {
        throw new AssertionError();
    }

    @Inject(at = @At("HEAD"), method="drawPathLines", cancellable = true)
    private static void drawPathLines(MatrixStack matrices, VertexConsumer vertexConsumers, Path path, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
    }

}
