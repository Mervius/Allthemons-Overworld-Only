package top.yuhh.dronecompat;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(value = DroneCompat.MODID)
public class DroneCompat {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "dronecompat";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static DroneCompat INSTANCE;
    public TicketController TICKETS = new TicketController(ResourceLocation.fromNamespaceAndPath(MODID, "teleport_pad_temp"));

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public DroneCompat(IEventBus modEventBus, ModContainer modContainer) {
        INSTANCE = this;
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerTicketController);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Test) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    public void registerTicketController(RegisterTicketControllersEvent event) {
        event.register(TICKETS);
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        PendingUnforceData data = PendingUnforceData.get(level);

        long now = level.getGameTime();

        Iterator<PendingUnforce> it = data.getPending().iterator();
        while (it.hasNext()) {
            PendingUnforce pending = it.next();
            if (pending.dimensionResourceKey.equals(level.dimension()) && now >= pending.tick) {
                TICKETS.forceChunk(level, pending.blockPos, pending.pos.x, pending.pos.z, false, true);
                it.remove();
                data.setDirty();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = scoreboard.getObjective("preventDimensionChange");
        if (objective == null) {
            return;
        }
        ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(player, objective);
        int score = scoreAccess.get();
        if (!(score == 1 || score == 0)) {
            scoreAccess.set(0);
        }

    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");

        MinecraftServer server = event.getServer();
        Scoreboard scoreboard = server.getScoreboard();
        Objective obj = scoreboard.getObjective("preventDimensionChange");

        if (obj == null) {
            scoreboard.addObjective(
                    "preventDimensionChange",
                    ObjectiveCriteria.DUMMY,
                    net.minecraft.network.chat.Component.literal("prevent dimension change"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null);

            scoreboard.setDisplayObjective(DisplaySlot.LIST, scoreboard.getObjective("preventDimensionChange"));
        }

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    public record PendingUnforce(ResourceKey<Level> dimensionResourceKey, BlockPos blockPos, ChunkPos pos, long tick) {
    }

    public static class PendingUnforceData extends SavedData {

        public PendingUnforceData() {
        }

        private final List<PendingUnforce> pending = new ArrayList<>();

        public static PendingUnforceData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new Factory<>(PendingUnforceData::new, PendingUnforceData::load), "pending_unforce");
        }

        public List<PendingUnforce> getPending() {
            return pending;
        }

        public void add(PendingUnforce p) {
            pending.add(p);
            setDirty();
        }

        public void remove(PendingUnforce p) {
            pending.remove(p);
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
            ListTag list = new ListTag();

            for (PendingUnforce p : pending) {
                CompoundTag entry = new CompoundTag();

                entry.putString("dimension", p.dimensionResourceKey.location().toString());
                entry.putLong("chunkX", p.pos.x);
                entry.putLong("chunkZ", p.pos.z);
                entry.putLong("blockPos", p.blockPos.asLong());
                entry.putLong("tick", p.tick);

                list.add(entry);
            }
            compoundTag.put("pending", list);
            return compoundTag;
        }

        public static PendingUnforceData load(
                CompoundTag tag,
                HolderLookup.Provider provider
        ) {
            PendingUnforceData data = new PendingUnforceData();

            ListTag list = tag.getList("pending", Tag.TAG_COMPOUND);

            for (Tag t : list) {
                CompoundTag entry = (CompoundTag) t;

                ResourceKey<Level> dim = ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.parse(entry.getString("dimension"))
                );

                data.pending.add(
                        new PendingUnforce(
                                dim,
                                BlockPos.of(entry.getLong("blockPos")),
                                new ChunkPos(
                                        (int) entry.getLong("chunkX"),
                                        (int) entry.getLong("chunkZ")
                                ),
                                entry.getLong("tick")
                        )
                );
            }

            return data;
        }
    }

}

