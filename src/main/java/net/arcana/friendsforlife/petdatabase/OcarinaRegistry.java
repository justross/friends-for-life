package net.arcana.friendsforlife.petdatabase;

import io.netty.buffer.Unpooled;
import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.packets.OcarinaUpdatePacket;
import net.arcana.friendsforlife.util.SavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;

import java.util.*;

public class OcarinaRegistry extends SavedData {

    public static final UUID EMPTY_UUID = new UUID(0L, 0L);

    // ocarinaUuid -> petUuid
    private final Map<UUID, UUID> ocarinaToPet = new HashMap<>();

    public static OcarinaRegistry get(MinecraftServer server) {
        return SavedData.get(server, "friendsforlife_ocarina_registry",
                OcarinaRegistry::fromNbt, OcarinaRegistry::new);
    }

    public OcarinaRegistry() {
    }

    public Map<UUID, UUID> getMap() {
        return ocarinaToPet;
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
    protected void write(CompoundTag tag) {
        ListTag list = new ListTag();

        for (var entry : ocarinaToPet.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("ocarina", entry.getKey());
            c.putUUID("pet", entry.getValue());
            list.add(c);
        }

        tag.put("entries", list);
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
