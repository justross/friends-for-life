//? if neoforge {
/*package net.arcana.friendsforlife.loaders.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.client.*;
import net.arcana.friendsforlife.homeblock.HomeBlockUpdateEvents;
import net.arcana.friendsforlife.packets.*;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@EventBusSubscriber(value = Dist.CLIENT, modid = Init.MOD_ID)
public class NeoforgeClientEntrypoint {
    private static KeyMapping openBookKey;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        openBookKey = new KeyMapping(
                "key.friendsforlife.open_book",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.friendsforlife.keys");


    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openBookKey != null && openBookKey.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                mc.setScreen(new RosterScreen());
            }
        }
    }

    static {
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
        Net.CHANNEL.registerClientbound(PetUpdatePacket.class, (message, access) ->

        {
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
        Net.CHANNEL.registerClientbound(HomeUpdatePacket.class, (packet, content) ->

        {
            Minecraft.getInstance().execute(() -> {
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, packet.dimId());
                assert Minecraft.getInstance().player != null;
                HomeBlockUpdateEvents.fire(Minecraft.getInstance().player.getUUID(), packet.pos(), dim);
            });
        });

        // Ocarina list update
        Net.CHANNEL.registerClientbound(OcarinaListPacket.class, (message, access) ->

        {
            access.runtime().execute(() -> {
                message.ocarinaToPet().forEach((otp) -> ClientOcarinaRegistry.set(otp.uuid(), otp.petUuid()));
            });
        });

        // Listen to ocarina bind update
        Net.CHANNEL.registerClientbound(
                OcarinaUpdatePacket.class,
                (packet, context) ->

                {
                    Minecraft.getInstance().execute(() -> {
                        UUID ocarinaId = packet.ocarinaUuid();
                        UUID petId = packet.petUuid(); // may be null if unbound

                        ClientOcarinaRegistry.set(ocarinaId, petId);

                    });
                });
    }

    @SubscribeEvent
    public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        Net.CHANNEL.clientHandle().send(new PetListRequestPacket());

        ClientHomeBlock.initialize();

        ClientOcarinaRegistry.initialize();
        Net.CHANNEL.clientHandle().send(new OcarinaListRequestPacket());
    }

    @SubscribeEvent
    public static void onClientLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHomeBlock.reset();
        ClientPetList.reset();
        ClientOcarinaRegistry.clear();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        PoseStack matrices = event.getPoseStack();
        Camera camera = event.getCamera();
        HomeBlockRenderer.render(matrices, camera);
    }

}
*///?}
