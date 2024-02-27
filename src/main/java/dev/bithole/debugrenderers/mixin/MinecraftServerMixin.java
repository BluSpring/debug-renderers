package dev.bithole.debugrenderers.mixin;

import dev.bithole.debugrenderers.network.MiscInfoSender;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Shadow private int ticks;

    @Shadow @Final private long[] tickTimes;

    @Inject(at=@At("TAIL"), method="tick(Ljava/util/function/BooleanSupplier;)V")
    public void tick(BooleanSupplier shouldKeepTicking, CallbackInfo info) {
        MiscInfoSender.sendTickTime((MinecraftServer)(Object)this, this.tickTimes[this.ticks % 100]);
    }

}
