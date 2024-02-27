package dev.bithole.debugrenderers.mixin;

import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.TargetPathNode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(Path.class)
public abstract class PathMixin {

    @Shadow @Nullable private Path.DebugNodeInfo debugNodeInfos;

    @Inject(at = @At("HEAD"), method = "toBuf")
    public void toBuffer(CallbackInfo info) {

        // categorize nodes into open/closed based on the `closed` parameter
        // this *seems* right, but I have no idea whether it is...
        List<PathNode> openSet = new ArrayList<>(),
                       closedSet = new ArrayList<>();

        Path self = (Path)(Object)this;
        for (int i = 0; i < self.getLength(); i++) {
            PathNode node = self.getNode(i);
            if (node.visited)
                closedSet.add(node);
            else
                openSet.add(node);
        }
        this.debugNodeInfos = new Path.DebugNodeInfo(openSet.toArray(new PathNode[0]), closedSet.toArray(new PathNode[0]), Collections.singleton(new TargetPathNode(0, 0, 0)));

    }

}
