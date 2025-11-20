package net.cozystudios.rainbowbridge.petdatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.cozystudios.rainbowbridge.RainbowBridgePackets;
import net.cozystudios.rainbowbridge.packets.PetUpdatePacket;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PetWatcher {
    private static int tickCounter = 0;
    List<PetUpdatePacket.Entry> changedPets = new ArrayList<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PetWatcher::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (++tickCounter % 100 != 0)
            return;

        PetTracker tracker = PetTracker.get(server);

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        boolean changed = false;

        for (Map.Entry<UUID, PetData> entry : tracker.getTrackedMap().entrySet()) {
            UUID petUuid = entry.getKey();
            PetData petdata = entry.getValue();
            boolean petChanged = false;

            Entity entity = petdata.getEntity(server).join().entity();
            if (entity == null)
                continue;

            // --- Position check ---
            BlockPos newPos = entity.getBlockPos();
            if (!newPos.equals(petdata.position)) {
                petdata.position = newPos;
                changed = true;
                petChanged = true;
            }

            // --- Name check ---
            // compare the CustomName stored in petdata NBT with the entity's current custom
            // name
            NbtCompound nbtSnapshot = petdata.getEntityData();
            String oldName = nbtSnapshot.getString("CustomName"); // JSON stored in NBT
            String newName = entity.getCustomName() != null ? entity.getCustomName().toString() : "";

            if (!newName.equals(oldName)) {
                petdata.updateEntityData(nbt -> entity.writeNbt(nbt)); // overwrite snapshot with current NBT
                changed = true;
                petChanged = true;
            }

            if (!petChanged) {
                continue;
            }

            buf.writeUuid(petUuid);
            buf.writeString(entity.getCustomName() != null ? entity.getCustomName().getString() : "");
            buf.writeBlockPos(petdata.position);
        }

        if (changed) {
            // --- Send packet ---
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

                ServerPlayNetworking.send(player, RainbowBridgePackets.RESPONSE_PET_UPDATE, new PacketByteBuf(buf.copy()));
            }
        }
        tracker.markDirty();
    }
}
