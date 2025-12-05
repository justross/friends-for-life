package net.arcana.friendsforlife.petdatabase;

import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.packets.PetUpdatePacket;
import net.arcana.friendsforlife.util.CompatComponent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

import java.util.*;

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

        List<PetUpdatePacket.Entry> updatedPets = tracker.getTrackedMap().values().stream()
                .filter((petData) -> {
                    UUID petUuid = petData.uuid;

                    boolean petChanged = false;

                    Entity entity = petData.getEntity(server).join().entity();


                    if (entity == null)
                        return false; // If there is no entity then there is no chance this could be updated.

                    // --- Position check ---
                    BlockPos newPos = entity.blockPosition();
                    if (!newPos.equals(petData.position)) {
                        petData.position = newPos;
                        petChanged = true;
                    }

                    // --- Name check ---
                    // compare the CustomName stored in pet data NBT with the entity's current custom customName
                    CompoundTag nbtSnapshot = petData.getEntityData();
                    String oldName = nbtSnapshot.getString("CustomName"); // JSON stored in NBT
                    String newName = entity.getCustomName() != null ? entity.getCustomName().toString() : "";

                    if (!newName.equals(oldName)) {
                        petData.updateEntityData(entity::saveWithoutId); // overwrite snapshot with current NBT
                        petChanged = true;
                    }

                    return petChanged;
                })
                .map(pd -> {
                    Component customName = Objects.requireNonNull(pd.getEntity(server).join().entity()).getCustomName();
                    String name = customName != null ? CompatComponent.toJson(customName) : "";

                    return new PetUpdatePacket.Entry(pd.uuid, name, pd.position);
                })
                .toList();


        if (!updatedPets.isEmpty()) {
            Net.CHANNEL.serverHandle(server).send(new PetUpdatePacket(updatedPets));
        }
        tracker.setDirty();
    }
}
