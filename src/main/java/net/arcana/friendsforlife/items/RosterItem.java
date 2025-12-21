package net.arcana.friendsforlife.items;

import net.arcana.friendsforlife.RaycastHelper;
import net.arcana.friendsforlife.client.RosterScreen;
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.util.Item;
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//?}
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RosterItem extends Item {
    public RosterItem(Properties settings) {
        super(settings);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        // Only open screen on the client side
        if (world.isClientSide) {
            if (!player.isShiftKeyDown()) {
                openRosterScreen();
                player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            }
        }

        if (!world.isClientSide) {
            // If sneak is held down set home block position
            if (player.isShiftKeyDown()) {
                ResourceKey<Level> dim = player.level().dimension();
                BlockPos pos = RaycastHelper.getSafeBlock(player);

                HomeBlock.get(world.getServer()).setHome((ServerPlayer) player, pos, dim);

                player.displayClientMessage(Component.translatable("message.friendsforlife.default_home_set"), true);
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    /*? if fabric {*/ @Environment(EnvType.CLIENT) /*?}*/
    /*? if neoforge {*/ /*@OnlyIn(Dist.CLIENT) *//*?}*/
    private void openRosterScreen() {
        Minecraft.getInstance().setScreen(new RosterScreen());
    }

    @Override
    /*? if fabric {*/ @Environment(EnvType.CLIENT) /*?}*/
    /*? if neoforge {*/ /*@OnlyIn(Dist.CLIENT) *//*?}*/
    protected void compatAppendHoverText(ItemStack stack, List<Component> tooltip, TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            List<FormattedCharSequence> lines = Tooltip.splitTooltip(
                    Minecraft.getInstance(),
                    Component.translatable("tooltip.friendsforlife.roster.info"));
            for (FormattedCharSequence line : lines) {
                StringBuilder sb = new StringBuilder();
                line.accept((index, style, codePoint) -> {
                    sb.appendCodePoint(codePoint);
                    return true;
                });
                tooltip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.friendsforlife.more_info").withStyle(ChatFormatting.GRAY));
        }
    }
}
