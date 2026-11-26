package net.arcana.friendsforlife.petdatabase;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.Unpooled;
import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.packets.OcarinaUpdatePacket;
import net.arcana.friendsforlife.util.CompatNbt;
import net.arcana.friendsforlife.util.SavedData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
//? if >=1.21.10 {
/*import net.minecraft.world.level.saveddata.SavedDataType;
*///?}

import java.util.*;

public class OcarinaRegistry extends SavedData {

    public static final UUID EMPTY_UUID = new UUID(0L, 0L);
    public static final String KEY = "friendsforlife_ocarina_registry";

    // ocarinaUuid -> petUuid
    private final Map<UUID, UUID> ocarinaToPet = new HashMap<>();

    public static OcarinaRegistry get(MinecraftServer server) {
        //? if >=1.21.10 {

        /*return SavedData.get(server, TYPE);
        *///?} else {
    

        return SavedData.get(server, KEY,
                OcarinaRegistry::fromNbt, OcarinaRegistry::new);
     
        //?}
    }

    public OcarinaRegistry() {
    }

    public Map<UUID, UUID> getMap() {
        return ocarinaToPet;
    }


    //? if >=1.21.10 {
    /*private static final SavedDataType<OcarinaRegistry> TYPE = SavedData.nbtType(KEY, OcarinaRegistry::new, OcarinaRegistry::fromNbt);
    *///?}

    // ----------- LOADING ----------
    public static OcarinaRegistry fromNbt(CompoundTag tag) {
        OcarinaRegistry reg = new OcarinaRegistry();

        ListTag list = CompatNbt.getCompoundList(tag, "entries");
        for (Tag e : list) {
            CompoundTag c = (CompoundTag) e;
            UUID ocarina = CompatNbt.getUUID(c, "ocarina");
            UUID pet = CompatNbt.getUUID(c, "pet");
            if (ocarina != null && pet != null) {
                reg.ocarinaToPet.put(ocarina, pet);
            }
        }

        return reg;
    }

    // ----------- SAVING ----------
    @Override
    protected void write(CompoundTag tag) {
        ListTag list = new ListTag();

        for (var entry : ocarinaToPet.entrySet()) {
            CompoundTag c = new CompoundTag();
            CompatNbt.putUUID(c, "ocarina", entry.getKey());
            CompatNbt.putUUID(c, "pet", entry.getValue());
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
