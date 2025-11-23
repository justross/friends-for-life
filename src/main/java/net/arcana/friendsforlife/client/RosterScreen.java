package net.arcana.friendsforlife.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.arcana.friendsforlife.ModInit;
import net.arcana.friendsforlife.Packets;
import net.arcana.friendsforlife.homeblock.HomeBlockUpdateEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RosterScreen extends BaseUIModelScreen<StackLayout> {
    private int currentPetIndex;

    private EntityComponent<Entity> entityBox;

    private @Nullable StackLayout entityBoxContainer;

    private ClientPetData currentPet;

    private @Nullable ButtonComponent homeButton; // Component for the send pet to home button
    private @Nullable LabelComponent homeLabel; // Component for the pet's home coordinates
    private @Nullable LabelComponent defaultHomeLabel; // Component for the default home coordinates
    private HomeBlockUpdateEvents.IListener homeUpdateListener;

    private @Nullable ButtonComponent summonButton;

    private List<ClientPetData> pets;

    public RosterScreen() {
        super(StackLayout.class, DataSource.asset(new Identifier("friendsforlife", "roster")));
    }

    // @Override
    protected void init() {
        super.init();

        // Request pets from server
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ClientPlayNetworking.send(Packets.REQUEST_PET_TRACKER, buf);

        this.homeLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class,
                "home-position-label");

        this.homeButton = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "home-button");
        this.defaultHomeLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class,
                "default-home-label");

        // Listen for default home block update
        homeUpdateListener = (uuid, newHomePos, newDim) -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (uuid.equals(player.getUuid())) {
                if (currentPet != null && currentPet.homePosition == null) {
                    homeLabel.text(Text.literal(newHomePos.toShortString()));
                    homeButton.visible = true;
                }
                defaultHomeLabel.text(Text.literal(newHomePos.toShortString()));
            }
        };

        HomeBlockUpdateEvents.subscribe(homeUpdateListener);

        // Get home position
        BlockPos homePos = null;
        if (currentPet != null && currentPet.homePosition != null && currentPet.homeDimension != null) {
            homePos = currentPet.homePosition;
        } else {
            homePos = ClientHomeBlock.get();
        }

        if (currentPet != null) {
            homeLabel.text(Text.literal(homePos.toShortString()));
            homeButton.visible = true;
        } else {
            homeLabel.text(Text.literal(""));
        }

        BlockPos defaultHomePos = ClientHomeBlock.get();
        defaultHomeLabel.text(Text.literal(defaultHomePos.toShortString()));

        // Subscribe for live updates
        ClientPetList.addListener(this::fetchPetList);

        this.entityBoxContainer = this.uiAdapter.rootComponent.childById(StackLayout.class, "entity-box-container");

        client.execute(() -> {
            fetchPetList();
        });
    }

    @Override
    protected void build(StackLayout rootComponent) {

        // Summon button
        this.summonButton = rootComponent.childById(ButtonComponent.class, "summon-button");

        summonButton.onPress(button -> {
            if (currentPet == null) {
                ModInit.LOGGER.error(ModInit.MOD_ID, "Unable to find current pet.");
                return;
            }

            // Get player position
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null)
                return;
            BlockPos pos = mc.player.getBlockPos();
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            boolean shouldSit = false;
            Identifier dim = mc.player.getWorld().getRegistryKey().getValue();

            // Send teleport packet
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeUuid(currentPet.uuid);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeIdentifier(dim);
            buf.writeBoolean(shouldSit);

            ClientPlayNetworking.send(Packets.REQUEST_PET_TELEPORT, buf);
        });

        summonButton.visible = currentPet != null;

        // Home button
        this.homeButton = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "home-button");
        this.homeLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class, "home-position-label");
        homeButton.visible = currentPet != null;
        if (currentPet == null) {
            homeLabel.text(Text.literal(""));
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            this.homeButton.onPress(button -> {
                BlockPos homePos = null;
                Identifier dimId = null;
                if (currentPet.homePosition != null && currentPet.homeDimension != null) {
                    homePos = currentPet.homePosition;
                    dimId = currentPet.homeDimension;
                } else {
                    homePos = ClientHomeBlock.get();
                    dimId = ClientHomeBlock.getDimKey().getValue();
                }
                if (homePos == null) {
                    return;
                }

                assert currentPet != null;

                int x = homePos.getX();
                int y = homePos.getY();
                int z = homePos.getZ();
                boolean shouldWander = true;

                // Send teleport packet
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeUuid(currentPet.uuid);
                buf.writeInt(x);
                buf.writeInt(y);
                buf.writeInt(z);
                buf.writeIdentifier(dimId);
                buf.writeBoolean(shouldWander);

                ClientPlayNetworking.send(Packets.REQUEST_PET_TELEPORT, buf);
            });
        }

        StackLayout container = rootComponent.childById(StackLayout.class, "entity-box-container");

        // Remove old entity component if present
        if (!container.children().isEmpty()) {
            container.removeChild(container.children().get(0));
        }

        // Insert the new one
        if (entityBox != null) {
            entityBox.lookAtCursor(true);
            entityBox.scaleToFit(true);
            container.child(entityBox);
        }

        // Close button
        rootComponent.childById(ButtonComponent.class, "close-button").onPress(button -> {
            close();
        });

    }

    // Update the current pet
    @SuppressWarnings("unchecked")
    @Nullable
    protected void updateCurrentPet(ClientPetData newPet) {
        if (newPet == null) {
            this.currentPet = null;
            this.currentPetIndex = -1;
            this.summonButton.visible = false;
            this.homeButton.visible = false;
            return;
        }

        assert pets != null;

        this.currentPetIndex = pets.indexOf(newPet);
        this.currentPet = newPet;

        // Update command buttons
        this.summonButton.visible = true;
        this.homeButton.visible = true;

        // Update entity box
        entityBoxContainer.removeChild(entityBox);
        var newEntityBox = Components.entity(Sizing.fixed(50), (EntityType<Entity>) newPet.type,
                newPet.getRenderingEntity(MinecraftClient.getInstance().world).writeNbt(new NbtCompound()));
        entityBox = newEntityBox;
        entityBoxContainer.child(entityBox);
        newEntityBox.lookAtCursor(true);
        newEntityBox.scaleToFit(true);

        // Update name
        LabelComponent nameLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class, "pet-name-label");
        String clipped = MinecraftClient.getInstance().textRenderer.trimToWidth(
                Text.literal(newPet.name).getString(),
                nameLabel.maxWidth());
        nameLabel.text(Text.literal(clipped));
        nameLabel.tooltip(Text.literal(newPet.name));

        // Update positions
        LabelComponent positionLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class,
                "pet-position-label");
        positionLabel.text(Text.literal(newPet.position));
        this.homeLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class,
                "home-position-label");

        BlockPos homePos = null;
        if (currentPet.homePosition != null) {
            homePos = currentPet.homePosition;
        } else {
            homePos = ClientHomeBlock.get();
        }
        this.homeLabel.text(Text.literal(homePos.toShortString()));
    }

    public void setPets(List<ClientPetData> pets) {
        if (pets.isEmpty()) {
            updateCurrentPet(null);
            return;
        }

        this.pets = pets;
        updateCurrentPet(pets.get(0));

        // Hide the empty pet list message
        LabelComponent noPetsMessage = uiAdapter.rootComponent.childById(LabelComponent.class, "no-pets-message");
        if (noPetsMessage != null)
            noPetsMessage.remove();

        // Find the container
        FlowLayout petListContainer = uiAdapter.rootComponent.childById(FlowLayout.class, "pet-list");

        // Clear old buttons if necessary
        petListContainer.clearChildren();

        // Generate buttons for all pets
        for (ClientPetData pet : pets) {

            int normalBg = (0x00000000); // transparent
            int hoverBg = (0xFFFFFFFF); // white

            Color normalText = Color.ofDye(DyeColor.WHITE);
            Color hoverText = Color.ofDye(DyeColor.BROWN);

            StackLayout wrapper = Containers.stack(Sizing.fill(95), Sizing.content());
            wrapper.surface(Surface.flat(normalBg));

            LabelComponent label = (LabelComponent) Components.label(Text.literal(" " + pet.name))
                    .cursorStyle(CursorStyle.HAND)
                    .horizontalSizing(Sizing.fill(100))
                    .margins(Insets.vertical(2));

            label.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    updateCurrentPet(pet);
                    MinecraftClient.getInstance().getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
                return false;
            });

            label.mouseEnter().subscribe(() -> {
                label.color(hoverText);
                wrapper.surface(Surface.flat(hoverBg));
            });

            label.mouseLeave().subscribe(() -> {
                label.color(normalText);
                wrapper.surface(Surface.flat(normalBg));
            });

            wrapper.child(label);

            petListContainer.child(wrapper);
        }

    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        super.close();
        ClientPetList.removeListener(this::fetchPetList);
        HomeBlockUpdateEvents.unsubscribe(homeUpdateListener);
    }

    private void fetchPetList() {
        List<ClientPetData> pets = ClientPetList.getAllForPlayer(MinecraftClient.getInstance().player);
        setPets(pets);
    }

}
