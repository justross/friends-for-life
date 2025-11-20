package net.arcana.friendsforlife.items;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.arcana.friendsforlife.Sounds;
import net.arcana.friendsforlife.RaycastHelper;
import net.arcana.friendsforlife.TameableWanderHelper;
import net.arcana.friendsforlife.TaskScheduler;
import net.arcana.friendsforlife.client.ClientOcarinaRegistry;
import net.arcana.friendsforlife.client.ClientPetData;
import net.arcana.friendsforlife.client.ClientPetList;
import net.arcana.friendsforlife.homeblock.HomeBlock;
import net.arcana.friendsforlife.homeblock.HomeBlock.HomeBlockHandle;
import net.arcana.friendsforlife.petdatabase.OcarinaRegistry;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.petdatabase.PetTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class OcarinaItem extends Item {
    public OcarinaItem(Settings settings) {
        super(settings);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean hasGlint(ItemStack stack) {
        if (stack.getNbt() == null || !stack.getNbt().contains("ocarinaUuid"))
            return false;

        UUID ocarinaUuid = UUID.fromString(stack.getNbt().getString("ocarinaUuid"));

        return ClientOcarinaRegistry.hasPet(ocarinaUuid);
    }

    @Override
    public Text getName(ItemStack stack) {
        Text baseName = super.getName(stack);
        String name = getPetName(stack);
        if (!name.isEmpty()) {
            return Text.literal(baseName.getString() + " - " + name);
        }
        return baseName;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        // --- Check cooldown ---
        if (!player.getItemCooldownManager().isCoolingDown(this)) {

            if (!world.isClient) {
                NbtCompound nbt = stack.getOrCreateNbt();
                UUID ocarinaUuid = nbt.contains("ocarinaUuid")
                        ? UUID.fromString(nbt.getString("ocarinaUuid"))
                        : null;
                OcarinaRegistry registry = OcarinaRegistry.get(world.getServer());
                UUID petUuid = registry.getPetForOcarina(ocarinaUuid);
                PetTracker tracker = PetTracker.get(world.getServer());
                PetData petData = tracker.get(petUuid);

                // Summon bound pet
                if (!player.isSneaking() && petUuid != null) {
                    if (petData != null) {
                        BlockPos pos = RaycastHelper.getSafeBlock(player);
                        TaskScheduler.schedule(() -> {
                            var tame = petData.recreateEntity(
                                    world.getServer(),
                                    world.getRegistryKey(),
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ());
                            if (tame != null) {
                                TameableWanderHelper.stopWandering(tame);
                            }
                        }, 30);
                    }

                }

                // Send pet home if using using ocarina on assigned pet
                if (player.isSneaking() && registry.getPetForOcarina(ocarinaUuid) != null &&
                        registry.getPetForOcarina(ocarinaUuid).equals(petData.uuid)) {

                    // Get homeblock from pet data, or fall back to default homeblock
                    HomeBlockHandle hbh = petData.getHomeBlock();
                    if (hbh == null) {
                        hbh = HomeBlock.get(world.getServer()).getHome(world.getServer(), player.getUuid());
                    }
                    if (hbh != null) {
                        var tame = petData.recreateEntity(
                                world.getServer(),
                                world.getRegistryKey(),
                                hbh.pos().getX(),
                                hbh.pos().getY(),
                                hbh.pos().getZ());
                        if (tame != null) {
                            TameableWanderHelper.makeTameableWander(tame);
                        }
                    }
                }
                if (player.isSneaking()) {
                    world.playSound(
                            null,
                            player.getBlockPos(),
                            Sounds.OCARINA_TWO,
                            SoundCategory.PLAYERS,
                            0.7f, // volume
                            1.0f // pitch
                    );

                } else {
                    world.playSound(
                            null,
                            player.getBlockPos(),
                            Sounds.OCARINA,
                            SoundCategory.PLAYERS,
                            0.7f, // volume
                            1.0f // pitch
                    );

                }
                player.getItemCooldownManager().set(this, 45);
            }
        }
        return TypedActionResult.success(player.getStackInHand(hand));

    }

    /** Handles interactions from bound pet entity callback */
    public ActionResult useOcarinaFromPet(World world, PlayerEntity player, ItemStack stack, PetData petData,
            TameableEntity tame) {
        if (!player.getItemCooldownManager().isCoolingDown(this)) {
            NbtCompound nbt = stack.getOrCreateNbt();
            UUID ocarinaUuid = nbt.contains("ocarinaUuid")
                    ? UUID.fromString(nbt.getString("ocarinaUuid"))
                    : null;
            OcarinaRegistry registry = OcarinaRegistry.get(world.getServer());
            UUID petUuid = registry.getPetForOcarina(ocarinaUuid);

            if (player.isSneaking()) {

                // Bind the ocarina to pet if not already assigned to it
                if ((petUuid == null || !petUuid.equals(petData.uuid))) {

                    UUID ocarinaId = nbt.getString("ocarinaUuid") != ""
                            ? UUID.fromString(nbt.getString("ocarinaUuid"))
                            : null;
                    // Assign uuid to ocarina if it doesn't have one
                    if (!nbt.contains("ocarinaUuid")) {
                        ocarinaId = UUID.randomUUID();
                        nbt.putString("ocarinaUuid", ocarinaId.toString());
                    }

                    registry.register(ocarinaId, petData.uuid, world.getServer());

                    player.sendMessage(Text.translatable("message.friendsforlife.ocarina_bound"), true);
                    return ActionResult.SUCCESS;
                } else {
                    return use(world, player, Hand.MAIN_HAND).getResult();
                }
            }
        }

        return ActionResult.PASS;
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {

    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // NbtCompound nbt = stack.getNbt();
        // if (nbt != null && nbt.contains("ocarinaUuid")) {
        // UUID ocarinaUuid = UUID.fromString(nbt.getString("ocarinaUuid"));
        // UUID petUuid = ClientOcarinaRegistry.getPet(ocarinaUuid);
        // if (petUuid != null) {
        // ClientPetData pd = ClientPetList.getPet(petUuid);
        // if (pd != null) {
        // tooltip.add(Text.literal(pd.name).formatted(Formatting.AQUA));
        // }
        // }
        // }

        String name = getPetName(stack);
        if (!name.isEmpty()) {
            tooltip.add(Text.literal(name).formatted(Formatting.AQUA));
        }

        if (Screen.hasShiftDown()) {
            List<OrderedText> lines = Tooltip.wrapLines(
                    MinecraftClient.getInstance(),
                    Text.translatable("tooltip.friendsforlife.ocarina.info"));
            for (OrderedText line : lines) {
                StringBuilder sb = new StringBuilder();
                line.accept((index, style, codePoint) -> {
                    sb.appendCodePoint(codePoint);
                    return true;
                });
                tooltip.add(Text.literal(sb.toString()).formatted(Formatting.GRAY));
            }
        } else {
            tooltip.add(Text.translatable("tooltip.friendsforlife.more_info").formatted(Formatting.GRAY));
        }
    }

    private String getPetName(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("ocarinaUuid")) {
            UUID ocarinaUuid = UUID.fromString(nbt.getString("ocarinaUuid"));
            UUID petUuid = ClientOcarinaRegistry.getPet(ocarinaUuid);
            if (petUuid != null) {
                ClientPetData pd = ClientPetList.getPet(petUuid);
                if (pd != null) {
                    return pd.name;
                }
            }
        }

        return "";
    }
}
