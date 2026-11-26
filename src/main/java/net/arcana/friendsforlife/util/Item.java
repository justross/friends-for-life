package net.arcana.friendsforlife.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <1.21.10 {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.10 {
/*import net.minecraft.world.item.component.TooltipDisplay;
*///?}
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Item extends net.minecraft.world.item.Item {
    public Item(Properties properties) {
        super(properties);
    }

    protected void compatAppendHoverText(ItemStack stack, List<Component> tooltip, TooltipFlag context) {

    }


    //? if >=1.21.10 {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext tooltip, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag tooltipFlag) {
        List<Component> lines = new ArrayList<>();
        compatAppendHoverText(stack, lines, tooltipFlag);
        lines.forEach(consumer);
    }
    *///?} else if >=1.21 {
    /*@Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext tooltipContext, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        compatAppendHoverText(stack, tooltip, tooltipFlag);
    }*///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        compatAppendHoverText(stack, tooltip, context);
    }
    //?}


    // Version-independent logic
    protected boolean compatUse(Level world, Player player, InteractionHand hand) {
        return false;
    }

    //? if >=1.21.10 {
    /*@Override
    public @NotNull InteractionResult use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        return compatUse(world, player, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    *///?} else {
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return compatUse(world, player, hand)
                ? InteractionResultHolder.success(stack)
                : InteractionResultHolder.pass(stack);
    }
    //?}
}
