//? if neoforge {
/*package net.arcana.friendsforlife.loaders.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.client.*;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.glfw.GLFW;


@EventBusSubscriber(value = Dist.CLIENT, modid = Init.MOD_ID)
public class NeoforgeClientEntrypoint {
    private static KeyMapping openBookKey;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        //? if >=1.21.10 {
        /^openBookKey = new KeyMapping(
                "key.friendsforlife.open_book",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                Init.FRIENDS_FOR_LIFE);
        event.registerCategory(Init.FRIENDS_FOR_LIFE);
        ^///?} else {
        openBookKey = new KeyMapping(
                "key.friendsforlife.open_book",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.friendsforlife.keys");
        //?}
        event.register(openBookKey);
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
        ClientNet.initialize();
    }

    @SubscribeEvent
    public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientEventHandler.handleClientJoin();
    }

    @SubscribeEvent
    public static void onClientLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientEventHandler.handleClientDisconnect();
    }

    //? if >=1.21.10 {
    /^@SubscribeEvent
    public static void onRenderAfterEntities(RenderLevelStageEvent.AfterEntities event) {
        HomeBlockRenderer.render(event.getPoseStack(), Minecraft.getInstance().gameRenderer.getMainCamera());
    }
    ^///?} else {
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        PoseStack matrices = event.getPoseStack();
        Camera camera = event.getCamera();
        HomeBlockRenderer.render(matrices, camera);
    }
    //?}

}
*///?}
