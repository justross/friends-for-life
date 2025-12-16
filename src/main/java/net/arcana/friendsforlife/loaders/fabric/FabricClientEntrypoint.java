//? if fabric {
package net.arcana.friendsforlife.loaders.fabric;


import com.mojang.blaze3d.platform.InputConstants;
import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.client.*;
import net.arcana.friendsforlife.homeblock.HomeBlockUpdateEvents;
import net.arcana.friendsforlife.packets.*;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FabricClientEntrypoint implements ClientModInitializer {
    private static KeyMapping openBookKey;

    @Override
    public void onInitializeClient() {
        openBookKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.friendsforlife.open_book",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.friendsforlife.keys"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openBookKey.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new RosterScreen());
            }
        });

        // Pet List update
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
        Net.CHANNEL.registerClientbound(PetUpdatePacket.class, (message, access) -> {
            access.runtime().execute(() -> {
                for (PetUpdatePacket.Entry update : message.pets()) {
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
        Net.CHANNEL.registerClientbound(
                OcarinaUpdatePacket.class,
                (packet, context) -> {
                    Minecraft.getInstance().execute(() -> {
                        UUID ocarinaId = packet.ocarinaUuid();
                        UUID petId = packet.petUuid(); // may be null if unbound

                        ClientOcarinaRegistry.set(ocarinaId, petId);

                    });
                });

        // Register a listener for when the player joins a world/server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Net.CHANNEL.clientHandle().send(new PetListRequestPacket());

            ClientHomeBlock.initialize();

            ClientOcarinaRegistry.initialize();
            Net.CHANNEL.clientHandle().send(new OcarinaListRequestPacket());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientHomeBlock.reset();
            ClientPetList.reset();
            ClientOcarinaRegistry.clear();
        });

        // Home renderer block
        WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
            HomeBlockRenderer.render(context.matrixStack(), context.camera());
        });

    }

}
//?}
