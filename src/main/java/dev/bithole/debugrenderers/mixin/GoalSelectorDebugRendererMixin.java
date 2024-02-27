package dev.bithole.debugrenderers.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.GoalSelectorDebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.custom.DebugGoalSelectorCustomPayload;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Goal selectors aren't removed when they become inactive, leading to a lot of clutter
@Mixin(GoalSelectorDebugRenderer.class)
public abstract class GoalSelectorDebugRendererMixin {

    private static final Map<Integer, Long> lastSeen = new HashMap<>();

    @Shadow
    @Final
    private Int2ObjectMap<GoalSelectorDebugRenderer.Entity> goalSelectors;


    @Inject(at = @At("TAIL"), method="setGoalSelectorList")
    public void setGoalSelectorList(int index, BlockPos pos, List<DebugGoalSelectorCustomPayload.Goal> goals, CallbackInfo ci) {
        lastSeen.put(index, MinecraftClient.getInstance().world.getTime());
    }

    @Inject(at = @At("TAIL"), method="render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;DDD)V")
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ, CallbackInfo info) {

        Iterator<Map.Entry<Integer, Long>> it = lastSeen.entrySet().iterator();
        long time = MinecraftClient.getInstance().world.getTime();

        while(it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            if(time - entry.getValue() > 20L) {
                it.remove();
                goalSelectors.remove(entry.getKey());
            }
        }

    }

}
