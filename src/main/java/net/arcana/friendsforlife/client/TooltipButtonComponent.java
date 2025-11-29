package net.arcana.friendsforlife.client;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.util.NinePatchTexture;

public class TooltipButtonComponent extends ButtonComponent {
    public boolean looksDisabled = false;

    public TooltipButtonComponent(Component message, Consumer<ButtonComponent> onPress) {
        super(message, onPress);
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        ResourceLocation texture;
        if (looksDisabled) {
            texture = DISABLED_TEXTURE;
        } else if (this.isHovered) {
            texture = HOVERED_TEXTURE;
        } else {
            texture = ACTIVE_TEXTURE;
        }

        NinePatchTexture.draw(texture, (OwoUIDrawContext) context, getX(), getY(), width, height);
        super.renderWidget(context, mouseX, mouseY, delta); // optional: draws text + tooltip
    }
}