package net.arcana.friendsforlife.client.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyHelper {

    public static boolean isAltDown() {
        Minecraft client = Minecraft.getInstance();
        long window = client.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }
}
