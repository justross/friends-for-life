package net.arcana.friendsforlife.items;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.arcana.friendsforlife.RaycastHelper;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class CollarItem extends Item {
    private static final String UUIDtag = "animalUUID";

    public CollarItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains("HomePos");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide) {
            if (player.isShiftKeyDown()) {
                ResourceKey<Level> dim = player.level().dimension();
                CompoundTag nbt = stack.getOrCreateTag();

                BlockPos pos = RaycastHelper.getRaycastBlock(player);
                if (pos == null && nbt != null && nbt.getLong("HomePos") != 0) {
                    nbt.remove("HomePos");
                    nbt.remove("HomeDim");
                    player.displayClientMessage(Component.translatable("message.friendsforlife.home_unset"), true);
                } else {
                    if (pos == null) {
                        pos = player.blockPosition();
                    }
                    nbt.putLong("HomePos", pos.asLong());
                    nbt.putString("HomeDim", dim.location().toString());
                    stack.setTag(nbt);
                    player.getInventory().setChanged();

                    player.displayClientMessage(Component.translatable("message.friendsforlife.home_set"), true);
                }

            }
        }

        return InteractionResultHolder.success(stack);
    }

    public InteractionResult applyCollar(ItemStack item, ServerPlayer user, TamableAnimal tame) {
        // maybe a config setting here or something
        tame.setInvulnerable(true);

        var tracker = PetTracker.get(user.getServer());
        // tell server to track the pet
        tracker.addPet(tame, user, item, Instant.now().toEpochMilli());
        if (!user.isCreative())
            item.shrink(1);

        PetData pd = tracker.getByEntityId(tame.getUUID());
        var pos = item.hasTag() && item.getTag().contains("HomePos")
                ? BlockPos.of(item.getTag().getLong("HomePos"))
                : null;
        var dim = item.hasTag() && item.getTag().contains("HomeDim")
                ? new ResourceLocation(item.getTag().getString("HomeDim"))
                : null;

        if (pos != null && dim != null) {
            pd.homePosition = pos;
            pd.homeDimension = ResourceKey.create(Registries.DIMENSION, dim);
        }

        user.level().playSound(
                null, // null = broadcast to nearby players
                tame.blockPosition(), // sound position
                SoundEvents.ARMOR_EQUIP_LEATHER, // pick any sound here
                SoundSource.PLAYERS,
                1.0F, 1.0F);

        return InteractionResult.SUCCESS;
    }

    public static ItemStack getCollar(Player user, TamableAnimal tame, PetData petData) {
        MinecraftServer server = tame.level().getServer();
        if (server == null)
            return null;
        if (petData == null)
            return null;

        user.level().playSound(
                null, // null = broadcast to nearby players
                tame.blockPosition(), // sound position
                SoundEvents.ARMOR_EQUIP_LEATHER, // pick any sound here
                SoundSource.PLAYERS,
                1.0F, 1.0F);
        return ItemStack.of(petData.collar);
    }

    public static void removePet(TamableAnimal tame, PetData petData) {
        MinecraftServer server = tame.level().getServer();
        if (server == null)
            return;
        PetTracker.get(server).removePet(server, petData.uuid);
        tame.setInvulnerable(false);
    }

    public PetData getBoundPetData(Level world, ItemStack item) {
        if (!item.hasTag() || !item.getTag().hasUUID(UUIDtag))
            return null;
        UUID uuid = item.getTag().getUUID(UUIDtag);

        MinecraftServer server = world.getServer();
        if (server == null)
            return null;
        return PetTracker.get(server).get(uuid);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains("HomePos")) {
            BlockPos pos = BlockPos.of(nbt.getLong("HomePos"));

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
