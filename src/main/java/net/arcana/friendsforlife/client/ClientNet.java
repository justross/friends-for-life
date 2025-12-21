package net.arcana.friendsforlife.client;

import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.client.homeblock.HomeBlockUpdateEvents;
import net.arcana.friendsforlife.client.petdatabase.ClientOcarinaRegistry;
import net.arcana.friendsforlife.client.petdatabase.ClientPetData;
import net.arcana.friendsforlife.client.petdatabase.ClientPetList;
import net.arcana.friendsforlife.client.petdatabase.PetDataUpdateEvent;
import net.arcana.friendsforlife.packets.*;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientNet {
    public static void initialize() {
        // Pet List update packet
        Net.CHANNEL.registerClientbound(PetListPacket.class, (packet, access) -> {
            List<ClientPetData> pets = new ArrayList<>();
            for (PetData.Entry pet : packet.pets()) {
                pets.add(ClientPetData.fromEntry(pet));
            }

            // update GUI on the main thread
            access.runtime().execute(() -> {
                ClientPetList.setPets(pets);
            });
        });

        // Pet update packet
        Net.CHANNEL.registerClientbound(PetTrackerUpdatePacket.class, (message, access) -> {
            access.runtime().execute(() -> {
                for (PetTrackerUpdatePacket.Entry update : message.pets()) {
                    ClientPetData pet = ClientPetList.getPet(update.uuid());
                    if (pet != null) {
                        if (!update.customName().isEmpty()) {
                            pet.entityData.putString("CustomName", update.customName());
                        }
                        pet.position = update.pos().toShortString();
                    }
                }
            });
        });

        // Pet mode update packet
        Net.CHANNEL.registerClientbound(PetModeUpdatePacket.class, (packet, access) -> {
            access.runtime().execute(() -> {
                ClientPetData pet = ClientPetList.getPet(packet.uuid());
                if (pet != null) {
                    pet.mode = PetData.FollowMode.fromId(packet.mode());
                }
            });
        });

        // Pet downed update packet
        Net.CHANNEL.registerClientbound(PetDownedUpdatePacket.class, (packet, access) -> {
            access.runtime().execute(() -> {
                ClientPetData pet = ClientPetList.getPet(packet.uuid());
                if (pet != null) {
                    pet.isDowned = packet.isDowned();
                    PetDataUpdateEvent.fire(pet);
                }
            });
        });

        // Listen to home block update packet
        Net.CHANNEL.registerClientbound(HomeUpdatePacket.class, (packet, content) -> {
            Minecraft.getInstance().execute(() -> {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, packet.dimId());
                assert Minecraft.getInstance().player != null;
                HomeBlockUpdateEvents.fire(Minecraft.getInstance().player.getUUID(), packet.pos(), dim);
            });
        });

        // Ocarina list update
        Net.CHANNEL.registerClientbound(OcarinaListPacket.class, (message, access) -> {
            access.runtime().execute(() -> {
                message.ocarinaToPet().forEach((otp) -> ClientOcarinaRegistry.set(otp.uuid(), otp.petUuid()));
            });
        });

        // Listen to ocarina bind update
        Net.CHANNEL.registerClientbound(OcarinaUpdatePacket.class, (packet, context) -> {
            Minecraft.getInstance().execute(() -> {
                UUID ocarinaId = packet.ocarinaUuid();
                UUID petId = packet.petUuid(); // may be null if unbound

                ClientOcarinaRegistry.set(ocarinaId, petId);

            });
        });

        // Pet misssing message from server
        Net.CHANNEL.registerClientbound(PetMissingMessagePacket.class, (message, access) -> {
            access.runtime().execute(() -> {
                var pet = ClientPetList.getPet(message.uuid());
                if (pet == null) {
                    Init.LOGGER.warn("Pet {} could not be found in client registry", message.uuid());
                    return;
                }
                var petName = ClientPetList.getPet(message.uuid()).getName();
                access.player().displayClientMessage(Component.translatable("message.friendsforlife.pet_missing", petName), true);
            });
        });
    }
}
