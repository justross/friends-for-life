package net.arcana.friendsforlife;

import net.arcana.friendsforlife.util.CompatResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class Tags {
    public static final TagKey<EntityType<?>> PET_ALWAYS_PASSIVE = TagKey.create(Registries.ENTITY_TYPE, new CompatResourceLocation(Init.MOD_ID, "pet_always_passive").unwrap());
}
