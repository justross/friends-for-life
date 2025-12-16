package net.arcana.friendsforlife.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Item extends net.minecraft.world.item.Item {
    public Item(Properties properties) {
        super(properties);
    }

    protected void compatAppendHoverText(ItemStack stack, List<Component> tooltip, TooltipFlag context) {

    }

    //? if >=1.21 {
    /*@Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext tooltipContext, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        compatAppendHoverText(stack, tooltip, tooltipFlag);
    }
    *///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        compatAppendHoverText(stack, tooltip, context);
    }
    //?}
}
