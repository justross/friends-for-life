package net.arcana.friendsforlife.homeblock;

import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.loaders.fabric.FabricEntrypoint;
import net.arcana.friendsforlife.packets.HomeUpdatePacket;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.arcana.friendsforlife.util.SavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeBlock extends SavedData {
    public record HomeBlockHandle(BlockPos pos, ResourceKey<Level> dim) {
    };

    private final Map<UUID, HomeBlockHandle> playerHomes = new HashMap<>();
    public static final String KEY = "friendsforlife_home_blocks";

    // --- Access ---
    public static HomeBlock get(MinecraftServer server) {
        return SavedData.get(server, KEY, HomeBlock::fromNbt, HomeBlock::new);
    }

    public static HomeBlock fromNbt(CompoundTag nbt) {
        HomeBlock state = new HomeBlock();
        CompoundTag homes = nbt.getCompound("Homes");
        for (String key : homes.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag posTag = homes.getCompound(key);
                BlockPos pos = new BlockPos(
                        posTag.getInt("x"),
                        posTag.getInt("y"),
                        posTag.getInt("z"));
                String dimStr = posTag.getString("dimension");
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, new CompatResourceLocation(dimStr).unwrap());
                state.playerHomes.put(uuid, new HomeBlockHandle(pos, dim));
            } catch (Exception e) {
                FabricEntrypoint.LOGGER.error(FabricEntrypoint.MOD_ID, "Skipped invalid home entry: " + key);
            }
        }
        return state;
    }

    @Override
    public void write(CompoundTag nbt) {
        CompoundTag homes = new CompoundTag();
        for (Map.Entry<UUID, HomeBlockHandle> e : playerHomes.entrySet()) {
            BlockPos pos = e.getValue().pos();
            ResourceKey<Level> dim = e.getValue().dim();
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            tag.putString("dimension", dim.location().toString());
            homes.put(e.getKey().toString(), tag);
        }
        nbt.put("Homes", homes);
    }

    // --- API ---
    public void setHome(ServerPlayer player, BlockPos pos, ResourceKey<Level> dim) {
        playerHomes.put(player.getUUID(), new HomeBlockHandle(pos, dim));

        Net.CHANNEL.serverHandle(player).send(new HomeUpdatePacket(pos, dim.location()));

        setDirty(); // tells Minecraft to save this state
    }

    @Nullable
    public HomeBlockHandle getHome(MinecraftServer server, UUID playerUuid) {
        var home = playerHomes.get(playerUuid);
        if (home == null) {
            var player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null) {
                home = new HomeBlockHandle(player.getRespawnPosition(), player.getRespawnDimension());
            }
        }
        if (home != null && home.pos() == null) {
            home = new HomeBlockHandle(server.overworld().getSharedSpawnPos(), server.overworld().dimension());
        }
        return home;
    }

    public boolean hasHome(UUID playerUuid) {
        return playerHomes.containsKey(playerUuid);
    }

    public void removeHome(ServerPlayer player) {
        if (playerHomes.remove(player.getUUID()) != null) {
            Net.CHANNEL.serverHandle(player).send(new HomeUpdatePacket(null, null));
            setDirty();
        }
    }
}
