package dev.bithole.debugrenderers.mixin;

import com.google.common.collect.Lists;
import dev.bithole.debugrenderers.DebugRenderersMod;
import dev.bithole.debugrenderers.IDMapper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.custom.*;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.NameGenerator;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillageGossipType;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.PositionSourceType;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// WARNING: flaky code ahead due to poor documentation
@Mixin(DebugInfoSender.class)
public abstract class DebugInfoSenderMixin {
    private static final IDMapper<Path> pathIDMapper = new IDMapper<>();
    private static final IDMapper<GoalSelector> goalSelectorIDMapper = new IDMapper<>();

    @Shadow
    private static void sendToAll(ServerWorld world, CustomPayload payload) {
    }

    @Shadow
    public static void sendBrainDebugData(LivingEntity living) {
    }

    @Inject(at = @At("HEAD"), method = "sendPathfindingData(Lnet/minecraft/world/World;Lnet/minecraft/entity/mob/MobEntity;Lnet/minecraft/entity/ai/pathing/Path;F)V")
    private static void sendPathfindingData(World world, MobEntity mob, @Nullable Path path, float nodeReachProximity, CallbackInfo info) {
        if (!world.isClient() && path != null) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(pathIDMapper.getID(path));
            path.toBuf(buf);
            buf.writeFloat(nodeReachProximity);
            sendToAll((ServerWorld) world, new DebugPathCustomPayload(buf));
        }
    }

    @Inject(at = @At("HEAD"), method = "sendNeighborUpdate(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V")
    private static void sendNeighborUpdate(World world, BlockPos pos, CallbackInfo info) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarLong(world.getTime());
        buf.writeBlockPos(pos);
        sendToAll((ServerWorld) world, new DebugNeighborsUpdateCustomPayload(buf));
    }

    @Inject(at = @At("HEAD"), method = "sendBeeDebugData(Lnet/minecraft/entity/passive/BeeEntity;)V")
    private static void sendBeeDebugData(BeeEntity bee, CallbackInfo info) {

        if (bee.getWorld().isClient) return;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(bee.getUuid());
        buf.writeInt(bee.getId());
        buf.writeVec3d(bee.getPos());

        // TODO: figure out if this is the right Path, there could be several!
        Path path = bee.getNavigation().getCurrentPath();
        if (path != null) {
            buf.writeBoolean(true);
            path.toBuf(buf);
        } else {
            buf.writeBoolean(false);
        }

        buf.writeNullable(bee.getHivePos(), PacketByteBuf::writeBlockPos);
        buf.writeNullable(bee.getFlowerPos(), PacketByteBuf::writeBlockPos);
        buf.writeInt(bee.getMoveGoalTicks());

        // TODO: figure out if these are the right goals
        List<String> goals = bee.getGoalSelector().getRunningGoals().map(goal -> goal.getGoal().toString()).toList();
        buf.writeVarInt(goals.size());
        for (String goal : goals) {
            buf.writeString(DebugRenderersMod.remap(goal));
        }

        // looks like Yarn's mapping for this is wrong but oh well... i succumbed to the sweet elixir that are the Mojang's mappings and now i can never do anything to fix it
        List<BlockPos> blacklistedHives = bee.getPossibleHives();
        buf.writeVarInt(blacklistedHives.size());
        for (BlockPos pos : blacklistedHives) {
            buf.writeBlockPos(pos);
        }

        sendToAll((ServerWorld) bee.getWorld(), new DebugBeeCustomPayload(buf));
    }

    @Inject(at = @At("HEAD"), method = "sendBeehiveDebugData(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BeehiveBlockEntity;)V")
    private static void sendBeehiveDebugData(World world, BlockPos pos, BlockState state, BeehiveBlockEntity blockEntity, CallbackInfo info) {
        if (world.isClient()) return;
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeBlockPos(pos);
        buf.writeString(""); // TODO: figure out what this field ("type") could possibly mean
        buf.writeInt(blockEntity.getBeeCount());
        buf.writeInt(BeehiveBlockEntity.getHoneyLevel(state));
        buf.writeBoolean(blockEntity.isSmoked());

        sendToAll((ServerWorld) world, new DebugHiveCustomPayload(buf));
    }

    private static void writeBlockBox(BlockBox box, PacketByteBuf buf) {
        buf.writeInt(box.getMinX());
        buf.writeInt(box.getMinY());
        buf.writeInt(box.getMinZ());
        buf.writeInt(box.getMaxX());
        buf.writeInt(box.getMaxY());
        buf.writeInt(box.getMaxZ());
    }

    @Inject(at = @At("HEAD"), method = "sendStructureStart(Lnet/minecraft/world/StructureWorldAccess;Lnet/minecraft/structure/StructureStart;)V")
    private static void sendStructureStart(StructureWorldAccess world, StructureStart structureStart, CallbackInfo info) {
        if (world.isClient()) return;
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeRegistryKey(world.toServerWorld().getDimensionKey());
        writeBlockBox(structureStart.getBoundingBox(), buf);
        List<StructurePiece> children = structureStart.getChildren();
        buf.writeInt(children.size());
        for (StructurePiece piece : children) {
            writeBlockBox(piece.getBoundingBox(), buf);
            buf.writeBoolean(false); // TODO: what does this do? the code shows that it controls the color... what is the color supposed to indicate?
        }

        sendToAll(world.toServerWorld(), new DebugStructuresCustomPayload(buf));
    }

    @Inject(at = @At("HEAD"), method = "sendGameEvent(Lnet/minecraft/world/World;Lnet/minecraft/world/event/GameEvent;Lnet/minecraft/util/math/Vec3d;)V")
    private static void sendGameEvent(World world, GameEvent event, Vec3d pos, CallbackInfo info) {
        if (world.isClient()) return;
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeRegistryKey(event.getRegistryEntry().registryKey());
        buf.writeVec3d(pos);

        sendToAll((ServerWorld) world, new DebugGameEventCustomPayload(buf));
    }

    @Inject(at = @At("HEAD"), method = "sendGameEventListener(Lnet/minecraft/world/World;Lnet/minecraft/world/event/listener/GameEventListener;)V")
    private static void sendGameEventListener(World world, GameEventListener eventListener, CallbackInfo info) {
        if (world.isClient()) return;
        PacketByteBuf buf = PacketByteBufs.create();

        PositionSource posSource = eventListener.getPositionSource();

        PositionSourceType.write(posSource, buf);
        buf.writeVarInt(eventListener.getRange());

        sendToAll((ServerWorld) world, new DebugGameEventListenersCustomPayload(buf));
    }

    @Inject(at = @At("HEAD"), method = "sendRaids(Lnet/minecraft/server/world/ServerWorld;Ljava/util/Collection;)V")
    private static void sendRaids(ServerWorld world, Collection<Raid> raids, CallbackInfo info) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeInt(raids.size());
        for (Raid raid : raids) {
            buf.writeBlockPos(raid.getCenter());
        }

        sendToAll(world, new DebugRaidsCustomPayload(buf));
    }

    @Inject(at = @At("HEAD"), method = "sendGoalSelector(Lnet/minecraft/world/World;Lnet/minecraft/entity/mob/MobEntity;Lnet/minecraft/entity/ai/goal/GoalSelector;)V")
    private static void sendGoalSelector(World world, MobEntity mob, GoalSelector goalSelector, CallbackInfo info) {
        if(world.isClient()) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(goalSelectorIDMapper.getID(goalSelector));
        buf.writeBlockPos(mob.getBlockPos());
        Set<PrioritizedGoal> goals = goalSelector.getGoals();
        buf.writeInt(goals.size());
        for(PrioritizedGoal goal: goals) {
            buf.writeInt(goal.getPriority());
            buf.writeBoolean(goal.isRunning());
            buf.writeString(DebugRenderersMod.remap(goal.getGoal().toString()), 255);
        }

        sendToAll((ServerWorld)world, new DebugGoalSelectorCustomPayload(buf));
    }

    @Inject(at = @At("HEAD"), method = "sendBrainDebugData(Lnet/minecraft/entity/LivingEntity;)V")
    private static void sendBrainDebugData(LivingEntity entity, CallbackInfo info) {
        if(entity.getWorld().isClient()) return;
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeUuid(entity.getUuid());
        buf.writeInt(entity.getId());
        buf.writeString(entity.getNameForScoreboard()); // TODO: which name is this?

        if(entity instanceof VillagerEntity) {
            VillagerEntity villager = (VillagerEntity)entity;
            buf.writeString(villager.getVillagerData().getProfession().toString());
            buf.writeInt(villager.getExperience());
        } else {
            buf.writeString("");
            buf.writeInt(0);
        }

        buf.writeFloat(entity.getHealth());
        buf.writeFloat(entity.getMaxHealth());

        buf.writeVec3d(entity.getPos());

        var brain = entity.getBrain();
        long l = entity.getWorld().getTime();

        if (entity instanceof InventoryOwner inventoryOwner) {
            var inventory = inventoryOwner.getInventory();
            buf.writeString(inventory.isEmpty() ? "" : StringHelper.truncate(inventory.toString(), 255, true));
        } else {
            buf.writeString("");
        }

        buf.writeOptional(brain.hasMemoryModule(MemoryModuleType.PATH) ? brain.getOptionalMemory(MemoryModuleType.PATH) : Optional.empty(), (packetByteBuf, path) -> path.toBuf(packetByteBuf));

        if (entity instanceof VillagerEntity) {
            VillagerEntity villagerEntity = (VillagerEntity)entity;
            boolean bl = villagerEntity.canSummonGolem(l);
            buf.writeBoolean(bl);
        } else {
            buf.writeBoolean(false);
        }

        if (entity.getType() == EntityType.WARDEN) {
            WardenEntity wardenEntity = (WardenEntity) entity;
            buf.writeInt(wardenEntity.getAnger());
        } else {
            buf.writeInt(-1);
        }

        buf.writeCollection(brain.getPossibleActivities(), (buf2, activity) -> buf2.writeString(activity.getId()));
        Set<String> set = brain.getRunningTasks().stream().map(Task::toString).collect(Collectors.toSet());
        buf.writeCollection(set, PacketByteBuf::writeString);
        buf.writeCollection(DebugInfoSender.listMemories(entity, l), (buf2, memory) -> {
            String string = StringHelper.truncate(memory, 255, true);
            buf2.writeString(string);
        });

        if (entity instanceof VillagerEntity) {
            Set set2 = Stream.of(MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT).map(brain::getOptionalMemory).flatMap(Optional::stream).map(GlobalPos::getPos).collect(Collectors.toSet());
            buf.writeCollection(set2, PacketByteBuf::writeBlockPos);
        } else {
            buf.writeVarInt(0);
        }
        if (entity instanceof VillagerEntity) {
            Set set2 = Stream.of(MemoryModuleType.POTENTIAL_JOB_SITE).map(brain::getOptionalMemory).flatMap(Optional::stream).map(GlobalPos::getPos).collect(Collectors.toSet());
            buf.writeCollection(set2, PacketByteBuf::writeBlockPos);
        } else {
            buf.writeVarInt(0);
        }
        if (entity instanceof VillagerEntity) {
            Map<UUID, Object2IntMap<VillageGossipType>> map = ((VillagerEntity)entity).getGossip().getEntityReputationAssociatedGossips();
            ArrayList<String> list = Lists.newArrayList();
            map.forEach((uuid, gossips) -> {
                String string = NameGenerator.name(uuid);
                gossips.forEach((type, value) -> list.add(string + ": " + type + ": " + value));
            });
            buf.writeCollection(list, PacketByteBuf::writeString);
        } else {
            buf.writeVarInt(0);
        }

        sendToAll((ServerWorld)entity.getWorld(), new DebugBrainCustomPayload(buf));
    }

}
