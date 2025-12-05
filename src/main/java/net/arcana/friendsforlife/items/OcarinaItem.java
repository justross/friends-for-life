package net.arcana.friendsforlife.items;

import net.arcana.friendsforlife.RaycastHelper;
import net.arcana.friendsforlife.Sounds;
import net.arcana.friendsforlife.TameableWanderHelper;
import net.arcana.friendsforlife.TaskScheduler;
import net.arcana.friendsforlife.client.ClientOcarinaRegistry;
import net.arcana.friendsforlife.client.ClientPetData;
import net.arcana.friendsforlife.client.ClientPetList;
//? if >=1.21 {
import net.arcana.friendsforlife.components.Components;
 //?}
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.homeblock.HomeBlock.HomeBlockHandle;
import net.arcana.friendsforlife.petdatabase.OcarinaRegistry;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.arcana.friendsforlife.util.CompatItemStack;
import net.arcana.friendsforlife.util.Item;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class OcarinaItem extends Item {
    public OcarinaItem(Properties settings) {
        super(settings);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean isFoil(ItemStack stack) {
        //? if >=1.21 {
        String ocarinaUuidString = stack.get(Components.OCARINA_UUID);
        if (ocarinaUuidString == null) return false;
        UUID ocarinaUuid = UUID.fromString(ocarinaUuidString);
        return ClientOcarinaRegistry.hasPet(ocarinaUuid);

        //?} else {
        /*if (CompatItemStack.getTag(stack) == null || !CompatItemStack.getTag(stack).contains("ocarinaUuid"))
            return false;
        UUID ocarinaUuid = UUID.fromString(CompatItemStack.getTag(stack).getString("ocarinaUuid"));
        return ClientOcarinaRegistry.hasPet(ocarinaUuid);
        *///?}
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        Component baseName = super.getName(stack);
        String name = getPetName(stack);
        if (!name.isEmpty()) {
            return Component.literal(baseName.getString() + " - " + name);
        }
        return baseName;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // --- Check cooldown ---
        if (!player.getCooldowns().isOnCooldown(this)) {

            if (!world.isClientSide) {
                //? if >=1.21 {

                String uuidString = stack.getOrDefault(Components.OCARINA_UUID, "");
                UUID ocarinaUuid = null;
                if (!uuidString.isEmpty()) {
                    ocarinaUuid = UUID.fromString(uuidString);
                }
                //?} else {
                /*CompoundTag nbt = CompatItemStack.getOrCreateTag(stack);
                UUID ocarinaUuid = nbt.contains("ocarinaUuid")
                        ? UUID.fromString(nbt.getString("ocarinaUuid"))
                        : null;
                *///?}
                OcarinaRegistry registry = OcarinaRegistry.get(world.getServer());
                UUID petUuid = registry.getPetForOcarina(ocarinaUuid);
                PetTracker tracker = PetTracker.get(world.getServer());
                PetData petData = tracker.get(petUuid);

                // Summon bound pet
                if (!player.isShiftKeyDown() && petUuid != null) {
                    if (petData != null) {
                        BlockPos pos = RaycastHelper.getSafeBlock(player);
                        TaskScheduler.schedule(() -> {
                            var tame = petData.recreateEntity(
                                    world.getServer(),
                                    world.dimension(),
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ());
                            if (tame != null) {
                                TameableWanderHelper.stopWandering(tame);
                            }
                        }, 30);
                    }

                }

                // Send pet home if using ocarina on assigned pet
                if (petData != null && player.isShiftKeyDown() && registry.getPetForOcarina(ocarinaUuid) != null &&
                        registry.getPetForOcarina(ocarinaUuid).equals(petData.uuid)) {

                    // Get homeblock from pet data, or fall back to default homeblock
                    HomeBlockHandle hbh = petData.getHomeBlock();
                    if (hbh == null) {
                        hbh = HomeBlock.get(world.getServer()).getHome(world.getServer(), player.getUUID());
                    }
                    if (hbh != null) {
                        var tame = petData.recreateEntity(
                                world.getServer(),
                                world.dimension(),
                                hbh.pos().getX(),
                                hbh.pos().getY(),
                                hbh.pos().getZ());
                        if (tame != null) {
                            TameableWanderHelper.makeTameableWander(tame);
                        }
                    }
                }
                if (player.isShiftKeyDown()) {
                    world.playSound(
                            null,
                            player.blockPosition(),
                            Sounds.OCARINA_TWO,
                            SoundSource.PLAYERS,
                            0.7f, // volume
                            1.0f // pitch
                    );

                } else {
                    world.playSound(
                            null,
                            player.blockPosition(),
                            Sounds.OCARINA,
                            SoundSource.PLAYERS,
                            0.7f, // volume
                            1.0f // pitch
                    );

                }
                player.getCooldowns().addCooldown(this, 45);
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));

    }

    /**
     * Handles interactions from bound pet entity callback
     */
    public InteractionResult useOcarinaFromPet(Level world, Player player, ItemStack stack, PetData petData,
                                               TamableAnimal tame) {
        if (!player.getCooldowns().isOnCooldown(this)) {
            CompoundTag nbt = CompatItemStack.getOrCreateTag(stack);
            //? if >=1.21 {
            String ocarinaUuidString = stack.get(Components.OCARINA_UUID);
            UUID ocarinaUuid = ocarinaUuidString != null ? UUID.fromString(ocarinaUuidString) : null;
            //?} else {
            /*UUID ocarinaUuid = nbt.contains("ocarinaUuid")
                    ? UUID.fromString(nbt.getString("ocarinaUuid"))
                    : null;
            *///?}
            OcarinaRegistry registry = OcarinaRegistry.get(world.getServer());
            UUID petUuid = registry.getPetForOcarina(ocarinaUuid);

            if (player.isShiftKeyDown()) {

                // Bind the ocarina to pet if not already assigned to it
                if ((petUuid == null || !petUuid.equals(petData.uuid))) {

                    // Assign uuid to ocarina if it doesn't have one

                    //? if >=1.21 {
                    if (ocarinaUuidString == null) {
                        ocarinaUuid = UUID.randomUUID();
                        stack.set(Components.OCARINA_UUID, ocarinaUuid.toString());
                    }
                    registry.register(ocarinaUuid, petData.uuid, world.getServer());
                    player.getInventory().setChanged();

                    player.displayClientMessage(Component.translatable("message.friendsforlife.ocarina_bound"), true);
                    //?} else {
                    /*if (!nbt.contains("ocarinaUuid")) {
                        ocarinaUuid = UUID.randomUUID();
                        nbt.putString("ocarinaUuid", ocarinaUuid.toString());
                    }
                    registry.register(ocarinaUuid, petData.uuid, world.getServer());

                    player.displayClientMessage(Component.translatable("message.friendsforlife.ocarina_bound"), true);
                    *///?}

                    return InteractionResult.SUCCESS;
                } else {
                    return use(world, player, InteractionHand.MAIN_HAND).getResult();
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onDestroyed(@NotNull ItemEntity entity) {

    }

    @Override
    @Environment(EnvType.CLIENT)
    protected void compatAppendHoverText(ItemStack stack, List<Component> tooltip, TooltipFlag context) {
        String name = getPetName(stack);
        if (!name.isEmpty()) {
            tooltip.add(Component.literal(name).withStyle(ChatFormatting.AQUA));
        }

        if (Screen.hasShiftDown()) {
            List<FormattedCharSequence> lines = Tooltip.splitTooltip(
                    Minecraft.getInstance(),
                    Component.translatable("tooltip.friendsforlife.ocarina.info"));
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

    private String getPetName(ItemStack stack) {
        //? if >=1.21 {
        String uuidString = stack.getOrDefault(Components.OCARINA_UUID, "");
        if (!uuidString.isEmpty()) {
            var ocarinaUuid = UUID.fromString(uuidString);
            UUID petUuid = ClientOcarinaRegistry.getPet(ocarinaUuid);
            if (petUuid != null) {
                ClientPetData pd = ClientPetList.getPet(petUuid);
                if (pd != null) {
                    return pd.getName();
                }
            }
        }
        //?} else {
        /*CompoundTag nbt = CompatItemStack.getTag(stack);
        if (nbt != null && nbt.contains("ocarinaUuid")) {
            UUID ocarinaUuid = UUID.fromString(nbt.getString("ocarinaUuid"));
            UUID petUuid = ClientOcarinaRegistry.getPet(ocarinaUuid);
            if (petUuid != null) {
                ClientPetData pd = ClientPetList.getPet(petUuid);
                if (pd != null) {
                    return pd.getName();
                }
            }
        }
        *///?}

        return "";
    }
}
