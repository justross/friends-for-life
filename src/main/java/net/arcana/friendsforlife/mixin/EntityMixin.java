package net.arcana.friendsforlife.mixin;

import net.arcana.friendsforlife.accessors.EntityAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAccessor {
    @Shadow
    public abstract @Nullable MinecraftServer getServer();

    @Override
    public MinecraftServer friendsforlife_getServer() {
        Entity self = (Entity) (Object) this;
        return self.getServer();
    }
}
