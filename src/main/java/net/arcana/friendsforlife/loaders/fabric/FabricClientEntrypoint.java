//? if fabric {
package net.arcana.friendsforlife.loaders.fabric;


import com.mojang.blaze3d.platform.InputConstants;
import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.client.*;
import net.arcana.friendsforlife.packets.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
//? if >=1.21.10 {
/*import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
 //?}
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class FabricClientEntrypoint implements ClientModInitializer {
    private static KeyMapping openBookKey;

    @Override
    public void onInitializeClient() {
        ClientNet.initialize();

        //? if >=1.21.10 {
        /*openBookKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.friendsforlife.open_book",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                Init.FRIENDS_FOR_LIFE
        ));
        *///?} else {
                    openBookKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.friendsforlife.open_book",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.friendsforlife.keys"));
        //?}


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openBookKey.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new RosterScreen());
            }
        });

        // Register a listener for when the player joins a world/server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientEventHandler.handleClientJoin();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientEventHandler.handleClientDisconnect();
        });

        // Home renderer block
        WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
            //? if >=1.21.10 {
            /*if (context.gameRenderer() != null) {
                HomeBlockRenderer.render(
                        context.matrices(),
                        context.gameRenderer().getMainCamera()
                );
            }
            *///?} else {
            HomeBlockRenderer.render(context.matrixStack(), context.camera());
             //?}
        });

    }

}
//?}
