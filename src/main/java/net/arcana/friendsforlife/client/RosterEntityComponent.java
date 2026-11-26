//? if >=1.21.10 {
/*package net.arcana.friendsforlife.client;

import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/^*
 * Entity preview for the roster that doesn't jitter on 1.21.9+.
 * <p>
 * With lookAtCursor, owo's EntityComponent writes the cursor angles only into the entity's
 * previous-tick rotation fields (yHeadRotO, yRotO, xRotO), then extracts the render state with the
 * frame's partial tick. Since 1.21.9 that extraction blends previous to current rotation by the
 * partial tick, and the current fields still hold the pet's saved rotation, so the pose wobbles
 * between the two every frame. Older owo builds rendered with a partial tick of 0; doing the same
 * here makes the extracted pose exactly the cursor-facing one again. The preview entity is never
 * ticked, so nothing else depends on the partial tick.
 ^/
public class RosterEntityComponent extends EntityComponent<Entity> {
    public RosterEntityComponent(Sizing sizing, EntityType<Entity> type, CompoundTag nbt) {
        super(sizing, type, nbt);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        super.draw(context, mouseX, mouseY, 0f, delta);
    }
}
*///?}
