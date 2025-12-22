package net.arcana.friendsforlife.items;

import net.arcana.friendsforlife.RaycastHelper;
import net.arcana.friendsforlife.util.CompatItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//?}

//? if >= 1.21
/*import net.arcana.friendsforlife.components.Components;*/
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.arcana.friendsforlife.util.Item;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CollarItem extends Item {
    public CollarItem(Properties settings) {
        super(settings);
    }

    @Override
    /*? if fabric {*/ @Environment(EnvType.CLIENT) /*?}*/
    /*? if neoforge {*/ /*@OnlyIn(Dist.CLIENT) *//*?}*/
    public boolean isFoil(ItemStack stack) {
        //? if >=1.21 {
        /*return stack.getOrDefault(Components.HOME_POS, 0L) != 0;
        *///?} else {
        CompoundTag nbt = CompatItemStack.getTag(stack, HolderLookup.Provider.create(Stream.empty()));
        if (nbt == null) return false;
        return nbt.contains("HomePos");
        //?}
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            if (player.isShiftKeyDown()) {
                ResourceKey<Level> dim = player.level().dimension();

                BlockPos pos = RaycastHelper.getRaycastBlock(player);
                //? if >=1.21 {
                /*boolean shouldRemove = stack.getOrDefault(Components.HOME_POS, 0L) != 0 && pos == null;
                *///?} else {
                CompoundTag nbt = CompatItemStack.getOrCreateTag(stack);
                boolean shouldRemove = pos == null && nbt.getLong("HomePos") != 0;
                //?}
                if (shouldRemove) {
                    //? if >=1.21 {
                    /*stack.remove(Components.HOME_DIM);
                    stack.remove(Components.HOME_POS);
                    *///?} else {
                    nbt.remove("HomePos");
                    nbt.remove("HomeDim");
                    //?}

                    player.displayClientMessage(Component.translatable("message.friendsforlife.home_unset"), true);
                } else {
                    if (pos == null) {
                        pos = player.blockPosition();
                    }

                    //? if >=1.21 {
                    /*stack.set(Components.HOME_POS, pos.asLong());
                    stack.set(Components.HOME_DIM, dim.location().toString());
                    *///?} else {

                    nbt.putLong("HomePos", pos.asLong());
                    nbt.putString("HomeDim", dim.location().toString());

                    stack = CompatItemStack.setTag(stack, nbt);
                    //?}


                    player.getInventory().setChanged();

                    player.displayClientMessage(Component.translatable("message.friendsforlife.home_set"), true);
                }

            }
        }

        return InteractionResultHolder.success(stack);
    }

    public InteractionResult applyCollar(ItemStack item, ServerPlayer user, TamableAnimal tame) {
        var tracker = PetTracker.get(user.getServer());
        // tell server to track the pet
        PetData pd = tracker.addPet(tame, user, item, Instant.now().toEpochMilli());
        if (pd == null) {
            return InteractionResult.SUCCESS;
        }

        //? if >=1.21 {
        /*var posLong = item.get(Components.HOME_POS);
        var pos = posLong != null
                ? BlockPos.of(posLong)
                : null;
        var dimString = item.get(Components.HOME_DIM);
        var dim = dimString != null ? new CompatResourceLocation(dimString).unwrap() : null;
        *///?} else {

        var tag = CompatItemStack.hasTag(item) ? Objects.requireNonNull(CompatItemStack.getTag(item)) : null;
        var pos = tag != null && tag.contains("HomePos")
                ? BlockPos.of(tag.getLong("HomePos"))
                : null;
        var dim = tag != null && tag.contains("HomeDim")
                ? new CompatResourceLocation(tag.getString("HomeDim")).unwrap()
                : null;
        //?}

        if (pos != null && dim != null) {
            pd.homePosition = pos;
            pd.homeDimension = ResourceKey.create(Registries.DIMENSION, dim);
        }

        user.level().playSound(
                null, // broadcast to nearby players
                tame.getX(), tame.getY(), tame.getZ(),
                SoundEvents.ARMOR_EQUIP_LEATHER,
                SoundSource.PLAYERS,
                1.0F, 1.0F);

        if (!user.isCreative())
            item.shrink(1);

        return InteractionResult.SUCCESS;
    }

    public static ItemStack getCollar(Player user, TamableAnimal tame, PetData petData) {
        MinecraftServer server = tame.level().getServer();
        if (server == null)
            return null;
        if (petData == null)
            return null;

        user.level().playSound(null, tame.getX(), tame.getY(), tame.getZ(),
                SoundEvents.ARMOR_EQUIP_LEATHER,
                SoundSource.PLAYERS,
                1.0f, 1.0f
        );
        //? if >=1.21 {
        /*return CompatItemStack.of(petData.collar);
        *///?} else {
        return CompatItemStack.of(petData.collar);
         //?}
    }

    public static void removePet(TamableAnimal tame, PetData petData) {
        MinecraftServer server = tame.level().getServer();
        if (server == null)
            return;
        PetTracker.get(server).removePet(server, petData.uuid);
        tame.setInvulnerable(false);
    }

    @Override
    /*? if fabric {*/ @Environment(EnvType.CLIENT) /*?}*/
    /*? if neoforge {*/ /*@OnlyIn(Dist.CLIENT) *//*?}*/
    protected void compatAppendHoverText(ItemStack stack, List<Component> tooltip, TooltipFlag tooltipFlag) {

        //? if >=1.21 {
        /*long poslong = stack.getOrDefault(Components.HOME_POS, 0L);
        BlockPos pos = poslong != 0L ? BlockPos.of(poslong) : null;
        *///?} else {
        CompoundTag nbt = CompatItemStack.getTag(stack);
        BlockPos pos = null;
        if (nbt != null && nbt.contains("HomePos")) {
            pos = BlockPos.of(nbt.getLong("HomePos"));
        }
        //?}

        if (pos != null) {
            tooltip.add(Component.literal("Home Position: ")
                    .append(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                            .withStyle(ChatFormatting.AQUA)));
        }
        if (!Screen.hasShiftDown()) {
            tooltip.add(
                    Component.translatable("tooltip.friendsforlife.more_info")
                            .withStyle(ChatFormatting.GRAY));
        } else {
            List<FormattedCharSequence> lines = Tooltip.splitTooltip(
                    Minecraft.getInstance(),
                    Component.translatable("tooltip.friendsforlife.collar.info"));
            for (FormattedCharSequence line : lines) {
                StringBuilder sb = new StringBuilder();
                line.accept((index, style, codePoint) -> {
                    sb.appendCodePoint(codePoint);
                    return true;
                });
                tooltip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}