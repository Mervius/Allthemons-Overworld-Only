package top.yuhh.dronecompat.mixin;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.NotNull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Mixin(targets = "dev.compactmods.machines.machine.block.UnboundCompactMachineBlock")
public class CompactMachinesUnboundMachineBlockMixin extends Block {

    public CompactMachinesUnboundMachineBlockMixin(Properties properties) {
        super(properties);
    }

    @Unique
    private static Class<?> COMPACT_ENTITY_CLASS;

    @Unique
    private static Class<?> MACHINE_BLOCK_ENTITY_CLASS;

    @Unique
    private static Class<?> MACHINE_COLOR_CLASS;

    @Unique
    private static Class<?> MACHINE_COLOR_SYNC_PACKET;

    @Unique
    private static Field INVALID;

    @Unique
    private static Field MACHINE_COLOR;

    @Unique
    private static Field ROOM_OWNER;

    @Unique
    private static Field SHRINK_CONFIG;

    @Unique
    private static Field SHRINKING_TAGS;

    @Unique
    private static Method COMPACT_DIM;

    @Unique
    private static Method ENTERING_ROOM;

    @Unique
    private static Method FROM_DYE_COLOR;

    @Unique
    private static Method NEW_ROOM;

    @Unique
    private static Method SHRINKING_CONFIG;

    @Unique
    private static Method SHRINKING_DEVICE_SUCCESS;

    @Unique
    private static Method TELEPORT_TO_ROOM;

    @Unique
    private static Method TEMPLATE_ID_METHOD;

    @Unique
    private static Method TEMPLATE_METHOD;

    @Unique
    private static boolean droneCompat$initialized = false;

    @Unique
    private static void droneCompat$init() {

        if (droneCompat$initialized) {
            return;
        }
        droneCompat$initialized = true;

        Class<?> DATA_COMPONENTS_CLASS;
        Class<?> COMPACT_CLASS;
        try {
            COMPACT_CLASS = Class.forName("dev.compactmods.machines.api.dimension.CompactDimension");
            COMPACT_ENTITY_CLASS = Class.forName("dev.compactmods.machines.api.machine.block.ICompactMachineBlockEntity");
            DATA_COMPONENTS_CLASS = Class.forName("dev.compactmods.machines.shrinking.Shrinking$DataComponents");
            MACHINE_BLOCK_ENTITY_CLASS = Class.forName("dev.compactmods.machines.machine.Machines$BlockEntities");
            MACHINE_COLOR_CLASS = Class.forName("dev.compactmods.machines.api.machine.MachineColor");
            MACHINE_COLOR_SYNC_PACKET = Class.forName("dev.compactmods.machines.network.machine.MachineColorSyncPacket");
            SHRINK_CONFIG = DATA_COMPONENTS_CLASS.getDeclaredField("SHRINKING_CONFIG");
            SHRINK_CONFIG.setAccessible(true);
            INVALID = Class.forName("dev.compactmods.machines.api.room.template.RoomTemplate").getField("INVALID_TEMPLATE");
            MACHINE_COLOR = Class.forName("dev.compactmods.machines.api.attachment.CMDataAttachments").getDeclaredField("MACHINE_COLOR");
            MACHINE_COLOR.setAccessible(true);
            ROOM_OWNER = Class.forName("dev.compactmods.machines.api.attachment.CMDataAttachments").getDeclaredField("ROOM_OWNER");
            ROOM_OWNER.setAccessible(true);
            SHRINKING_TAGS = Class.forName("dev.compactmods.machines.api.shrinking.PSDTags").getDeclaredField("ITEM");
            SHRINKING_TAGS.setAccessible(true);
            COMPACT_DIM = COMPACT_CLASS.getMethod("forServer", MinecraftServer.class);
            ENTERING_ROOM = Class.forName("dev.compactmods.machines.api.room.history.RoomEntryPoint").getMethod("playerEnteringMachine", Player.class);
            FROM_DYE_COLOR = MACHINE_COLOR_CLASS.getMethod("fromDyeColor", DyeColor.class);
            NEW_ROOM = Class.forName("dev.compactmods.machines.api.CompactMachines").getMethod("newRoom", MinecraftServer.class, Class.forName("dev.compactmods.machines.api.room.template.RoomTemplate"), UUID.class);
            SHRINKING_CONFIG = Class.forName("dev.compactmods.machines.shrinking.PersonalShrinkingDevice").getMethod("config", ItemStack.class);
            SHRINKING_DEVICE_SUCCESS = Class.forName("dev.compactmods.machines.shrinking.PersonalShrinkingDevice").getMethod("handleSuccessfulAtomicShift", ItemStack.class, ServerPlayer.class, Class.forName("dev.compactmods.machines.api.shrinking.component.ShrinkingDeviceConfiguration"));
            TELEPORT_TO_ROOM = Class.forName("dev.compactmods.machines.room.RoomHelper").getMethod("teleportPlayerIntoRoom", MinecraftServer.class, ServerPlayer.class, Class.forName("dev.compactmods.machines.api.room.RoomInstance"), Class.forName("dev.compactmods.machines.api.room.history.RoomEntryPoint"));
            TEMPLATE_ID_METHOD = Class.forName("dev.compactmods.machines.machine.block.UnboundCompactMachineEntity").getMethod("templateId");
            TEMPLATE_METHOD = Class.forName("dev.compactmods.machines.api.room.template.RoomTemplateHelper").getMethod("getTemplate", LevelReader.class, ResourceLocation.class);
        } catch (ReflectiveOperationException e) {
            System.out.println("AAAAA" + e);
            COMPACT_ENTITY_CLASS = null;
            MACHINE_BLOCK_ENTITY_CLASS = null;
            MACHINE_COLOR_CLASS = null;
            MACHINE_COLOR_SYNC_PACKET = null;
            SHRINK_CONFIG = null;
            INVALID = null;
            MACHINE_COLOR = null;
            ROOM_OWNER = null;
            SHRINKING_TAGS = null;
            COMPACT_DIM = null;
            ENTERING_ROOM = null;
            FROM_DYE_COLOR = null;
            NEW_ROOM = null;
            SHRINKING_CONFIG = null;
            SHRINKING_DEVICE_SUCCESS = null;
            TELEPORT_TO_ROOM = null;
            TEMPLATE_ID_METHOD = null;
            TEMPLATE_METHOD = null;
        }
    }

    @NotNull
    @Unique
    private static ItemInteractionResult droneCompat$tryDyingMachine(ServerLevel level, @NotNull BlockPos pos, Player player, DyeItem dye, ItemStack mainItem) {

        try {
            DyeColor color = dye.getDyeColor();
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (COMPACT_ENTITY_CLASS.isInstance(blockEntity)) {
                Object newColor = FROM_DYE_COLOR.invoke(null, color);
                blockEntity.setData((AttachmentType) ((Supplier<?>) MACHINE_COLOR.get(null)).get(), newColor);
                Constructor<?> ctor = MACHINE_COLOR_SYNC_PACKET.getDeclaredConstructor(GlobalPos.class, MACHINE_COLOR_CLASS);
                PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), (CustomPacketPayload) ctor.newInstance(GlobalPos.of(level.dimension(), pos), MACHINE_COLOR_CLASS.cast(newColor)));

                if (!player.isCreative()) {
                    mainItem.shrink(1);
                }

                return ItemInteractionResult.CONSUME;
            }
        } catch (InvocationTargetException |NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            return ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.FAIL;
    }

    /**
     * @author Daniel Hagemeier
     * @reason pain
     */
    @Overwrite
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand p_316595_, @NotNull BlockHitResult p_316140_) {
        droneCompat$init();

        if (stack.getItem() instanceof DyeItem dye && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            return droneCompat$tryDyingMachine(serverLevel, pos, player, dye, stack);
        }

        MinecraftServer server = level.getServer();
        try {
            if ((stack.is((TagKey<Item>) SHRINKING_TAGS.get(null)) || stack.has((DataComponentType<?>) ((Supplier<?>) SHRINK_CONFIG.get(null)).get())) && player instanceof ServerPlayer sp) {
                Field unboundField = MACHINE_BLOCK_ENTITY_CLASS.getDeclaredField("UNBOUND_MACHINE");
                Field boundField = MACHINE_BLOCK_ENTITY_CLASS.getDeclaredField("MACHINE");
                unboundField.setAccessible(true);
                boundField.setAccessible(true);
                level.getBlockEntity(pos, ((BlockEntityType<?>)(((Supplier<?>) unboundField.get(null)).get()))).ifPresent(unboundEntity -> {
                    try {
                        Object template = TEMPLATE_METHOD.invoke(null, level, TEMPLATE_ID_METHOD.invoke(unboundEntity));
                        if (!template.equals(INVALID)) {
                            var color = unboundEntity.getData((AttachmentType) ((Supplier<?>) MACHINE_COLOR.get(null)).get());
                            try {
                                // Generate a new machine room
                                Object newRoom = NEW_ROOM.invoke(null, server, template, sp.getUUID());
                                ((IAttachmentHolder) newRoom).setData((AttachmentType) ((Supplier<?>) ROOM_OWNER.get(null)).get(), player.getUUID());

                                // Change into a bound machine block
                                level.setBlock(pos, ((Block) (((Supplier<?>) (Class.forName("dev.compactmods.machines.machine.Machines$Blocks").getField("BOUND_MACHINE").get(null))).get())).defaultBlockState(), Block.UPDATE_ALL);

                                // Set up binding and enter
                                level.getBlockEntity(pos, ((BlockEntityType<?>) (((Supplier<?>) boundField.get(null)).get()))).ifPresent(ent -> {
                                    try {
                                        Method setOwner = Class.forName("dev.compactmods.machines.machine.block.BoundCompactMachineBlockEntity").getMethod("setOwner", UUID.class);
                                        Method setConnectedRoom = Class.forName("dev.compactmods.machines.machine.block.BoundCompactMachineBlockEntity").getMethod("setConnectedRoom", String.class);
                                        Field code = Class.forName("dev.compactmods.machines.api.room.RoomInstance").getDeclaredField("code");
                                        code.setAccessible(true);
                                        setConnectedRoom.invoke(ent, code.get(newRoom));
                                        setOwner.invoke(ent, sp.getUUID());
                                        ent.setData((AttachmentType) ((Supplier<?>) MACHINE_COLOR.get(null)).get(), color);
                                    } catch (IllegalAccessException | ClassNotFoundException |
                                             InvocationTargetException | NoSuchMethodException |
                                             NoSuchFieldException e) {
                                        throw new RuntimeException(e);
                                    }

                                    try {
                                        ResourceKey<Level> currentDimension = sp.level().dimension();
                                        ResourceKey<Level> compactDimension = ((ServerLevel) COMPACT_DIM.invoke(null, sp.server)).dimension();
                                        Scoreboard scoreboard = sp.getScoreboard();
                                        Objective objective = scoreboard.getObjective("preventDimensionChange");
                                        if (objective != null) {
                                            ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(sp, objective);
                                            int score = scoreAccess.get();
                                            if (score == 0 && (!currentDimension.equals(compactDimension)) && !(sp instanceof FakePlayer)) {
                                                System.out.println("Cancelled dimension change");
                                            } else {
                                                ((CompletableFuture<?>) TELEPORT_TO_ROOM.invoke(null, server, sp, newRoom, ENTERING_ROOM.invoke(null, player))).thenAccept(res -> {
                                                    try {
                                                        SHRINKING_DEVICE_SUCCESS.invoke(null, stack, sp, Class.forName("dev.compactmods.machines.api.shrinking.component.ShrinkingDeviceConfiguration").cast(SHRINKING_CONFIG.invoke(null, stack)));
                                                    } catch (IllegalAccessException | InvocationTargetException |
                                                             ClassNotFoundException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                });
                                            }
                                        }
                                    } catch (InvocationTargetException | IllegalAccessException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                });
                            } catch (Exception e) {
                                LogUtils.getLogger().error("Error occurred while generating new room and machine info for first player entry.", e);
                            }
                        } else {
                            LogUtils.getLogger().error("Tried to create and enter an invalidly-registered room. Something went very wrong!");
                        }
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return super.useItemOn(stack, state, level, pos, player, p_316595_, p_316140_);
    }
}
