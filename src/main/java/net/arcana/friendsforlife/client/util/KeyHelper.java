package net.arcana.friendsforlife.client.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyHelper {

    public static boolean isShiftDown() {
        Minecraft client = Minecraft.getInstance();
        //? if >=1.21.10 {
        /*long windowHandle = client.getWindow().handle();
        *///?} else {
        long windowHandle = client.getWindow().getWindow();
         //?}
        return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    public static boolean isAltDown() {
        Minecraft client = Minecraft.getInstance();
        //? if >=1.21.10 {
        /*long windowHandle = client.getWindow().handle();
        *///?} else {
        long windowHandle = client.getWindow().getWindow();
         //?}
        return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }
}
