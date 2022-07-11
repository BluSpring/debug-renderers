package dev.bithole.debugrenderers;

import dev.bithole.debugrenderers.mixin.DebugInfoSenderMixin;
import dev.bithole.debugrenderers.mixin.DensityCapAccessor;
import dev.bithole.debugrenderers.mixin.SpawnDensityCapperAccessor;
import dev.bithole.debugrenderers.mixin.SpawnHelperInfoAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpawnInfoSender {

    public static final Identifier DEBUG_SPAWNING = new Identifier("debugrenderers", "spawning");
    private static final List<SpawnAttempt> spawnAttempts = new ArrayList<>();

    public static void clear() {
        spawnAttempts.clear();
    }

    public static void addSpawnAttempt(SpawnGroup group, BlockPos pos) {
        spawnAttempts.add(new SpawnAttempt(pos, group));
    }

    public static void send(ServerWorld world) {

        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeVarInt(spawnAttempts.size());
        for(SpawnAttempt attempt: spawnAttempts) {
            buf.writeString(attempt.group.asString());
            buf.writeBlockPos(attempt.pos);
        }

        NetworkHelper.sendToAll(world, buf, DEBUG_SPAWNING);

    }

    private static class SpawnAttempt {
        public final BlockPos pos;
        public final SpawnGroup group;
        public SpawnAttempt(BlockPos pos, SpawnGroup group) {
            this.pos = pos;
            this.group = group;
        }
    }

}
