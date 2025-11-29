package net.arcana.friendsforlife.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.arcana.friendsforlife.items.CollarItem;
import net.arcana.friendsforlife.items.RosterItem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HomeBlockRenderer {

        public static void register() {
                WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
                        Minecraft client = Minecraft.getInstance();
                        ResourceKey<Level> dimKey = ClientHomeBlock.getDimKey();
                        if (client.player != null) {
                                BlockPos homeBlock = null;
                                ItemStack item = client.player.getMainHandItem();

                                // If holding roster, show default home block
                                if (item.getItem() instanceof RosterItem
                                                && client.level.dimension().equals(dimKey)
                                                && KeyHelper.isAltDown()) {
                                        homeBlock = ClientHomeBlock.get();
                                }

                                // If holding collar, show that pet's home block
                                else if (item.getItem() instanceof CollarItem && KeyHelper.isAltDown()) {
                                        var dim = item.hasTag() && item.getTag().contains("HomeDim")
                                                        ? new ResourceLocation(item.getTag().getString("HomeDim"))
                                                        : null;
                                        if (dim == null || !client.level.dimension().location().equals(dim))
                                                return;
                                        homeBlock = item.hasTag() && item.getTag().contains("HomePos")
                                                        ? BlockPos.of(item.getTag().getLong("HomePos"))
                                                        : null;
                                }

                                if (homeBlock == null)
                                        return;

                                var matrices = context.matrixStack();
                                var camera = context.camera();
                                matrices.pushPose();

                                RenderSystem.enablePolygonOffset();
                                RenderSystem.polygonOffset(1f, -10f);

                                matrices.translate(
                                                homeBlock.getX() - camera.getPosition().x,
                                                homeBlock.getY() - camera.getPosition().y,
                                                homeBlock.getZ() - camera.getPosition().z);

                                VertexConsumer buffer = client.renderBuffers()
                                                .bufferSource()
                                                .getBuffer(RenderType.lines());

                                // Rainbow color pulse
                                float time = (System.currentTimeMillis() % 5000) / 5000f;
                                float r = (float) Math.sin(time * Math.PI * 2) * 0.5f + 0.5f;
                                float g = (float) Math.sin(time * Math.PI * 2 + 2) * 0.5f + 0.5f;
                                float b = (float) Math.sin(time * Math.PI * 2 + 4) * 0.5f + 0.5f;

                                VoxelShape shape = Shapes.block();

                                LevelRenderer.renderVoxelShape(
                                                matrices,
                                                buffer,
                                                shape,
                                                0, 0, 0, // x, y, z offset inside matrix
                                                r, g, b, // color
                                                1.0f,
                                                false
                                );
                                RenderSystem.polygonOffset(0f, 0f);
                                RenderSystem.disablePolygonOffset();

                                matrices.popPose();
                                client.renderBuffers().bufferSource().endBatch();

                        }
                });
        }

}