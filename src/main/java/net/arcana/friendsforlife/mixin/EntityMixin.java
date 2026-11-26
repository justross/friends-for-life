package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.EntityAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAccessor {
    // Entity#getServer was removed in 1.21.9; Level#getServer exists on every supported version.
    @Override
    public MinecraftServer friendsforlife_getServer() {
        Entity self = (Entity) (Object) this;
        return self.level().getServer();
    }
}
