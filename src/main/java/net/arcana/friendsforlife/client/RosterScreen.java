package net.arcana.friendsforlife.client;

import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import net.arcana.friendsforlife.Init;
import net.arcana.friendsforlife.Net;
import net.arcana.friendsforlife.client.homeblock.ClientHomeBlock;
import net.arcana.friendsforlife.client.homeblock.HomeBlockUpdateEvents;
import net.arcana.friendsforlife.client.petdatabase.ClientPetData;
import net.arcana.friendsforlife.client.petdatabase.ClientPetList;
import net.arcana.friendsforlife.client.petdatabase.PetDataUpdateEvent;
import net.arcana.friendsforlife.packets.PetListRequestPacket;
import net.arcana.friendsforlife.packets.PetTeleportRequestPacket;
import net.arcana.friendsforlife.packets.SetPetModePacket;
import net.arcana.friendsforlife.petdatabase.PetData;
import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RosterScreen extends BaseUIModelScreen<StackLayout> {
    private EntityComponent<Entity> entityBox;

    private @Nullable StackLayout entityBoxContainer;

    private ClientPetData currentPet;

    private @Nullable ButtonComponent homeButton; // Component for the send pet to home button
    private @Nullable ButtonComponent summonButton;
    private @Nullable LabelComponent homeLabel; // Component for the pet's home coordinates
    private @Nullable LabelComponent defaultHomeLabel; // Component for the default home coordinates
    private HomeBlockUpdateEvents.IListener homeUpdateListener;
    private PetDataUpdateEvent.IListener petDataUpdateListener;


    private List<ClientPetData> pets;

    public RosterScreen() {
        super(StackLayout.class, DataSource.asset(new CompatResourceLocation("friendsforlife", "roster").unwrap()));
    }

    public void renderFollowModeButton() {
        var companionButton = ((ButtonComponent) this.uiAdapter.rootComponent.childById(io.wispforest.owo.ui.core.Component.class, "follow-mode-button-companion"));
        var guardianButton = ((ButtonComponent) this.uiAdapter.rootComponent.childById(io.wispforest.owo.ui.core.Component.class, "follow-mode-button-guardian"));
        assert companionButton != null;
        assert guardianButton != null;

        guardianButton.visible = false;
        guardianButton.active = false;
        companionButton.visible = false;
        companionButton.active = false;

        // Hide mode button if there is only 1 mode available for the pet
        boolean shouldRender = currentPet != null && currentPet.modes.size() >= 2;

        if (shouldRender) {
            PetData.FollowMode mode = currentPet.mode;
            companionButton.visible = mode.equals(PetData.FollowMode.COMPANION);
            companionButton.active = mode.equals(PetData.FollowMode.COMPANION);

            guardianButton.visible = mode.equals(PetData.FollowMode.GUARDIAN);
            guardianButton.active = mode.equals(PetData.FollowMode.GUARDIAN);
            var currentButton = mode.equals(PetData.FollowMode.COMPANION) ? companionButton : guardianButton;
            currentButton.onPress(button -> {
                var currentMode = currentPet.mode;
                if (currentMode.equals(PetData.FollowMode.COMPANION)) {
                    currentMode = PetData.FollowMode.GUARDIAN;
                } else {
                    currentMode = PetData.FollowMode.COMPANION;
                }
                companionButton.visible = currentMode.equals(PetData.FollowMode.COMPANION);
                companionButton.active = currentMode.equals(PetData.FollowMode.COMPANION);

                guardianButton.visible = currentMode.equals(PetData.FollowMode.GUARDIAN);
                guardianButton.active = currentMode.equals(PetData.FollowMode.GUARDIAN);
                currentPet.mode = currentMode;

                Net.CHANNEL.clientHandle().send(new SetPetModePacket(currentPet.uuid, currentMode.id));
                renderFollowModeButton();

            });
        }
    }

    public void renderSummonButton() {
        // Summon button
        this.summonButton = (ButtonComponent) this.uiAdapter.rootComponent.childById(io.wispforest.owo.ui.core.Component.class, "summon-button");
        assert summonButton != null;

        summonButton.onPress(button -> {
            if (currentPet == null) {
                Init.LOGGER.error("Unable to find current pet.");
                return;
            }

            // Get player position
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;
            BlockPos pos = mc.player.blockPosition();
            ResourceLocation dim = mc.player.level().dimension().location();

            var packet = new PetTeleportRequestPacket(currentPet.uuid, pos, dim, false);
            Net.CHANNEL.clientHandle().send(packet);
        });

        summonButton.visible = currentPet != null;
        if (currentPet != null) {
            summonButton.active = !currentPet.isDowned;
        }
    }

    // @Override
    protected void init() {
        super.init();

        // Request pets from server
        Net.CHANNEL.clientHandle().send(new PetListRequestPacket());

        this.homeLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class, "home-position-label");
        assert homeLabel != null;

        this.homeButton = ((ButtonComponent) this.uiAdapter.rootComponent.childById(io.wispforest.owo.ui.core.Component.class, "home-button"));
        assert homeButton != null;

        this.defaultHomeLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class, "default-home-label");
        assert defaultHomeLabel != null;

        renderFollowModeButton();

        // Listen for default home block update
        homeUpdateListener = (uuid, newHomePos, newDim) -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (uuid.equals(player != null ? player.getUUID() : null)) {
                if (currentPet != null && currentPet.homePosition == null) {
                    homeLabel.text(Component.literal(newHomePos.toShortString()));
                    homeButton.visible = true;
                }
                defaultHomeLabel.text(Component.literal(newHomePos.toShortString()));
            }
        };

        HomeBlockUpdateEvents.subscribe(homeUpdateListener);

        // Listen for pet updates and render if needed
        petDataUpdateListener = (clientPetData) -> {
            if (clientPetData.uuid.equals(currentPet.uuid)) {
                updateCurrentPet(clientPetData);
            }
        };

        PetDataUpdateEvent.subscribe(petDataUpdateListener);

        // Get home position
        BlockPos homePos;
        if (currentPet != null && currentPet.homePosition != null && currentPet.homeDimension != null) {
            homePos = currentPet.homePosition;
        } else {
            homePos = ClientHomeBlock.get();
        }

        if (currentPet != null) {
            homeLabel.text(Component.literal(homePos.toShortString()));
            homeButton.visible = true;
        } else {
            homeLabel.text(Component.literal(""));
        }

        BlockPos defaultHomePos = ClientHomeBlock.get();
        assert defaultHomeLabel != null;
        defaultHomeLabel.text(Component.literal(defaultHomePos.toShortString()));

        // Subscribe for live updates
        ClientPetList.addListener(this::fetchPetList);

        this.entityBoxContainer = this.uiAdapter.rootComponent.childById(StackLayout.class, "entity-box-container");

        assert minecraft != null;
        minecraft.execute(this::fetchPetList);
    }

    @Override
    protected void build(StackLayout rootComponent) {

        renderSummonButton();

        // Home button
        this.homeButton = (ButtonComponent) this.uiAdapter.rootComponent.childById(io.wispforest.owo.ui.core.Component.class, "home-button");
        assert homeButton != null;
        homeButton.visible = currentPet != null;

        this.homeLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class, "home-position-label");
        assert homeLabel != null;

        if (currentPet == null) {
            homeLabel.text(Component.literal(""));
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.homeButton.onPress(button -> {
                BlockPos homePos;
                ResourceLocation dimId;
                if (currentPet.homePosition != null && currentPet.homeDimension != null) {
                    homePos = currentPet.homePosition;
                    dimId = currentPet.homeDimension;
                } else {
                    homePos = ClientHomeBlock.get();
                    dimId = ClientHomeBlock.getDimKey().location();
                }
                if (homePos == null) {
                    return;
                }

                assert currentPet != null;

                var packet = new PetTeleportRequestPacket(currentPet.uuid, homePos, dimId, true);
                Net.CHANNEL.clientHandle().send(packet);

            });
        }

        StackLayout container = rootComponent.childById(StackLayout.class, "entity-box-container");
        assert container != null;

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
        ButtonComponent buttonComponent = (ButtonComponent) rootComponent.childById(io.wispforest.owo.ui.core.Component.class, "close-button");
        if (buttonComponent != null) {
            buttonComponent.onPress(button -> {
                onClose();
            });
        }

        renderFollowModeButton();

    }

    // Update the current pet
    protected void updateCurrentPet(ClientPetData newPet) {
        if (newPet == null) {
            this.currentPet = null;
            if (this.summonButton != null) {
                this.summonButton.visible = false;
            }
            if (this.homeButton != null) {
                this.homeButton.visible = false;
            }
            return;
        }

        assert pets != null;

        this.currentPet = newPet;

        // Update command buttons
        if (this.summonButton != null) {
            this.summonButton.visible = true;
            this.summonButton.active = !this.currentPet.isDowned;
        }
        if (this.homeButton != null) {
            this.homeButton.visible = true;
            this.homeButton.active = !this.currentPet.isDowned;
        }

        // Update entity box
        if (entityBoxContainer != null) {
            entityBoxContainer.removeChild(entityBox);
        }
        var newEntityBox = Components.entity(Sizing.fixed(50), (EntityType<Entity>) newPet.type,
                newPet.getRenderingEntity(Minecraft.getInstance().level).saveWithoutId(new CompoundTag()));
        entityBox = newEntityBox;
        entityBoxContainer.child(entityBox);
        newEntityBox.lookAtCursor(true);
        newEntityBox.scaleToFit(true);

        // Update customName
        LabelComponent nameLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class, "pet-name-label");
        if (nameLabel != null) {
            String clipped = Minecraft.getInstance().font.plainSubstrByWidth(
                    Component.literal(newPet.getName()).getString(),
                    nameLabel.maxWidth());
            nameLabel.text(Component.literal(clipped));
            nameLabel.tooltip(Component.literal(newPet.getName()));
        }

        // Update positions
        LabelComponent positionLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class,
                "pet-position-label");
        if (positionLabel != null) {
            positionLabel.text(Component.literal(newPet.position));
        }
        this.homeLabel = this.uiAdapter.rootComponent.childById(LabelComponent.class,
                "home-position-label");

        BlockPos homePos;
        if (currentPet.homePosition != null) {
            homePos = currentPet.homePosition;
        } else {
            homePos = ClientHomeBlock.get();
        }
        if (this.homeLabel != null) {
            if (homePos != null) {
                this.homeLabel.text(Component.literal(homePos.toShortString()));
            } else {
                Init.LOGGER.warn("Expected home position to be defined.");
            }
        }

        renderFollowModeButton();
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
        if (petListContainer != null) {
            petListContainer.clearChildren();
        }

        // Generate buttons for all pets
        for (ClientPetData pet : pets) {

            int normalBg = (0x00000000); // transparent
            int hoverBg = (0xFFFFFFFF); // white

            Color normalText = Color.ofDye(DyeColor.WHITE);
            Color hoverText = Color.ofDye(DyeColor.BROWN);

            StackLayout wrapper = Containers.stack(Sizing.fill(95), Sizing.content());
            wrapper.surface(Surface.flat(normalBg));

            LabelComponent label = (LabelComponent) Components.label(Component.literal(" " + pet.getName()))
                    .cursorStyle(CursorStyle.HAND)
                    .horizontalSizing(Sizing.fill(100))
                    .margins(Insets.vertical(2));

            label.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == 0) {
                    updateCurrentPet(pet);
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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

            if (petListContainer != null) {
                petListContainer.child(wrapper);
            }
        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        ClientPetList.removeListener(this::fetchPetList);
        HomeBlockUpdateEvents.unsubscribe(homeUpdateListener);
    }

    private void fetchPetList() {
        List<ClientPetData> pets = ClientPetList.getAllForPlayer(Minecraft.getInstance().player);
        setPets(pets);
    }

}
