package net.arcana.friendsforlife.client;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;

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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClientInit implements ClientModInitializer {
    private static KeyBinding openBookKey;

    // Helper record to store the update data safely
    private record PetUpdate(UUID petUuid, String name, BlockPos pos) {
    }

    @Override
    public void onInitializeClient() {
        HomeBlockRenderer.register();

        openBookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.friendsforlife.open_book",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.friendsforlife.keys"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openBookKey.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
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
                        UUID uuid = buf.readUuid();
                        String entityTypeId = buf.readString(32767);
                        NbtCompound nbt = buf.readNbt();
                        String name = buf.readString(32767);
                        String position = buf.readString(32767);
                        long homePosLong = buf.readLong();
                        BlockPos homePosition = null;
                        if (homePosLong != 0L) {
                            homePosition = BlockPos.fromLong(homePosLong);
                        }
                        Identifier homeDimension = null;
                        String homeDimStr = buf.readString(32767);
                        if (!homeDimStr.isEmpty()) {
                            homeDimension = new Identifier(homeDimStr);
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
                        UUID petUuid = buf.readUuid();
                        String name = buf.readString();
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
            MinecraftClient.getInstance().execute(() -> {
                RegistryKey<World> dim = RegistryKey.of(RegistryKeys.WORLD, packet.dimId());
                HomeBlockUpdateEvents.fire(MinecraftClient.getInstance().player.getUuid(), packet.pos(), dim);
            });
        }));

        // Listen to ocarina bind update
        Net.CHANNEL.registerClientbound(
                OcarinaUpdatePacket.class,
                (packet, context) -> {
                    MinecraftClient.getInstance().execute(() -> {
                        UUID ocarinaId = packet.ocarinaUuid();
                        UUID petId = packet.petUuid(); // may be null if unbound

                        ClientOcarinaRegistry.set(ocarinaId, petId);

                    });
                });

        // Register a listener for when the player joins a world/server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PacketByteBuf ptBuf = new PacketByteBuf(Unpooled.buffer());
            ClientPlayNetworking.send(Packets.REQUEST_PET_TRACKER, ptBuf);

            ClientHomeBlock.initialize();
            ClientOcarinaRegistry.initialize();

            PacketByteBuf ocRegBuf = new PacketByteBuf(Unpooled.buffer());
            ClientPlayNetworking.send(Packets.REQUEST_OCARINA_REGISTRY, ocRegBuf);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientHomeBlock.reset();
            ClientPetList.reset();
            ClientOcarinaRegistry.clear();
        });
    }

}