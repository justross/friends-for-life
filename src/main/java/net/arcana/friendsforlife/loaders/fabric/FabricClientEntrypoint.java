package net.arcana.friendsforlife.loaders.fabric;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.arcana.friendsforlife.client.*;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import io.netty.buffer.Unpooled;
import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.Packets;
import net.arcana.friendsforlife.homeblock.HomeBlockUpdateEvents;
import net.arcana.friendsforlife.packets.HomeUpdatePacket;
import net.arcana.friendsforlife.packets.OcarinaUpdatePacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class FabricClientEntrypoint implements ClientModInitializer {
    private static KeyMapping openBookKey;

    // Helper record to store the update data safely
    private record PetUpdate(UUID petUuid, String name, BlockPos pos) {
    }

    @Override
    public void onInitializeClient() {
        HomeBlockRenderer.register();

        openBookKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.friendsforlife.open_book",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.friendsforlife.keys"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openBookKey.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.setScreen(new RosterScreen());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(Packets.RESPONSE_PET_TRACKER,
                (client, handler, buf, responseSender) -> {
                    int size = buf.readInt();
                    List<ClientPetData> pets = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        UUID uuid = buf.readUUID();
                        String entityTypeId = buf.readUtf(32767);
                        CompoundTag nbt = buf.readNbt();
                        String name = buf.readUtf(32767);
                        String position = buf.readUtf(32767);
                        long homePosLong = buf.readLong();
                        BlockPos homePosition = null;
                        if (homePosLong != 0L) {
                            homePosition = BlockPos.of(homePosLong);
                        }
                        ResourceLocation homeDimension = null;
                        String homeDimStr = buf.readUtf(32767);
                        if (!homeDimStr.isEmpty()) {
                            homeDimension = new ResourceLocation(homeDimStr);
                        }

                        pets.add(new ClientPetData(uuid, entityTypeId, nbt, name, position, homePosition,
                                homeDimension));
                    }

                    // update GUI on the main thread
                    client.execute(() -> {
                        ClientPetList.setPets(pets);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(Packets.RESPONSE_PET_UPDATE,
                (client, handler, buf, responseSender) -> {
                    List<PetUpdate> updates = new ArrayList<>();

                    while (buf.isReadable()) {
                        UUID petUuid = buf.readUUID();
                        String name = buf.readUtf();
                        BlockPos pos = buf.readBlockPos();

                        updates.add(new PetUpdate(petUuid, name, pos));
                    }

                    // Now safe to use in the main thread
                    client.execute(() -> {
                        for (PetUpdate update : updates) {
                            ClientPetData pet = ClientPetList.getPet(update.petUuid());
                            if (pet != null) {
                                if (!pet.name.isEmpty()) {
                                    pet.name = update.name();
                                }
                                pet.position = update.pos().toShortString();
                            }
                        }
                    });
                });

        // Listen to home block update packet
        Net.CHANNEL.registerClientbound(HomeUpdatePacket.class, ((packet, content) -> {
            Minecraft.getInstance().execute(() -> {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, packet.dimId());
                HomeBlockUpdateEvents.fire(Minecraft.getInstance().player.getUUID(), packet.pos(), dim);
            });
        }));

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
            FriendlyByteBuf ptBuf = new FriendlyByteBuf(Unpooled.buffer());
            ClientPlayNetworking.send(Packets.REQUEST_PET_TRACKER, ptBuf);

            ClientHomeBlock.initialize();
            ClientOcarinaRegistry.initialize();

            FriendlyByteBuf ocRegBuf = new FriendlyByteBuf(Unpooled.buffer());
            ClientPlayNetworking.send(Packets.REQUEST_OCARINA_REGISTRY, ocRegBuf);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientHomeBlock.reset();
            ClientPetList.reset();
            ClientOcarinaRegistry.clear();
        });
    }

}