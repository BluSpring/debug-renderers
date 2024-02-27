package dev.bithole.debugrenderers.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class DebugTickTime implements CustomPayload {
    private final long tickTime;

    public DebugTickTime(long tickTime) {
        this.tickTime = tickTime;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeLong(this.tickTime);
    }

    @Override
    public Identifier id() {
        return MiscInfoSender.DEBUG_TICKTIME;
    }
}
