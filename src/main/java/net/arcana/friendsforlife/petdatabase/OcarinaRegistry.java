package net.arcana.friendsforlife.petdatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.packets.OcarinaUpdatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class OcarinaRegistry extends SavedData {

    public static final UUID EMPTY_UUID = new UUID(0L, 0L);

    // ocarinaUuid -> petUuid
    private final Map<UUID, UUID> ocarinaToPet = new HashMap<>();

    public static OcarinaRegistry get(MinecraftServer server) {
        DimensionDataStorage mgr = server.getLevel(Level.OVERWORLD).getDataStorage();

        return mgr.computeIfAbsent(
                OcarinaRegistry::fromNbt,
                OcarinaRegistry::new,
                "friendsforlife_ocarina_registry");
    }

    public OcarinaRegistry() {
    }

    // ----------- LOADING ----------
    public static OcarinaRegistry fromNbt(CompoundTag tag) {
        OcarinaRegistry reg = new OcarinaRegistry();

        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (Tag e : list) {
            CompoundTag c = (CompoundTag) e;
            UUID ocarina = c.getUUID("ocarina");
            UUID pet = c.getUUID("pet");
            reg.ocarinaToPet.put(ocarina, pet);
        }

        return reg;
    }

    // ----------- SAVING ----------
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (var entry : ocarinaToPet.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("ocarina", entry.getKey());
            c.putUUID("pet", entry.getValue());
            list.add(c);
        }

        tag.put("entries", list);
        return tag;
    }

    /** Writes the full registry into a PacketByteBuf */
    public FriendlyByteBuf serializeList() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(ocarinaToPet.size());
        for (Map.Entry<UUID, UUID> entry : ocarinaToPet.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUUID(entry.getValue());
        }
        return buf;
    }

    // ----------- OPERATIONS ----------

    public void register(UUID ocarinaUuid, UUID petUuid, MinecraftServer server) {
        ocarinaToPet.put(ocarinaUuid, petUuid);
        setDirty();

        // Send update to all connected clients
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(ocarinaUuid);
        buf.writeUUID(petUuid);

        Net.CHANNEL.serverHandle(server).send(new OcarinaUpdatePacket(ocarinaUuid, petUuid));
    }

    public void unregisterOcarina(UUID ocarinaUuid, MinecraftServer server) {
        ocarinaToPet.remove(ocarinaUuid);
        Net.CHANNEL.serverHandle(server).send(new OcarinaUpdatePacket(ocarinaUuid, null));
        setDirty();
    }

    public List<UUID> getOcarinasForPet(UUID petUuid) {
        List<UUID> result = new ArrayList<>();

        for (var entry : ocarinaToPet.entrySet()) {
            if (entry.getValue().equals(petUuid)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public UUID getPetForOcarina(UUID ocarinaUuid) {
        return ocarinaToPet.get(ocarinaUuid);
    }

    public void unregisterAllForPet(UUID petUuid, MinecraftServer server) {
        List<UUID> ocarinasToClear = new ArrayList<>();

        for (var entry : ocarinaToPet.entrySet()) {
            if (entry.getValue().equals(petUuid)) {
                ocarinasToClear.add(entry.getKey());
            }
        }

        // Now update map + send packets
        for (UUID ocarinaUuid : ocarinasToClear) {
            ocarinaToPet.remove(ocarinaUuid);
            Net.CHANNEL.serverHandle(server).send(
                    new OcarinaUpdatePacket(ocarinaUuid, EMPTY_UUID));
        }

        setDirty();
    }

}
