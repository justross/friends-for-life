package net.arcana.friendsforlife.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import net.minecraft.network.chat.MutableComponent;

//? if >=1.21.10 {
/*import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
*///?} else {
import net.minecraft.nbt.Tag;
//?}

/**
 * Version-independent access to raw {@link CompoundTag}s and NBT-based entity save/load.
 * <p>
 * 1.21.5 made CompoundTag getters return Optionals and removed the UUID helpers; 1.21.6 moved
 * entity save/load to ValueOutput/ValueInput. Getters here return the pre-1.21.5 defaults
 * (empty string, 0, false, empty compound) when a key is missing.
 */
public final class CompatNbt {
    private CompatNbt() {
    }

    public static java.util.Set<String> keys(CompoundTag tag) {
        //? if >=1.21.10 {
        /*return tag.keySet();
        *///?} else {
        return tag.getAllKeys();
        //?}
    }

    public static String getString(CompoundTag tag, String key) {
        //? if >=1.21.10 {
        /*return tag.getStringOr(key, "");
        *///?} else {
        return tag.getString(key);
        //?}
    }

    public static int getInt(CompoundTag tag, String key) {
        //? if >=1.21.10 {
        /*return tag.getInt(key).orElse(0);
        *///?} else {
        return tag.getInt(key);
        //?}
    }

    public static long getLong(CompoundTag tag, String key) {
        //? if >=1.21.10 {
        /*return tag.getLong(key).orElse(0L);
        *///?} else {
        return tag.getLong(key);
        //?}
    }

    public static float getFloat(CompoundTag tag, String key) {
        //? if >=1.21.10 {
        /*return tag.getFloat(key).orElse(0f);
        *///?} else {
        return tag.getFloat(key);
        //?}
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        //? if >=1.21.10 {
        /*return tag.getBoolean(key).orElse(false);
        *///?} else {
        return tag.getBoolean(key);
        //?}
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        //? if >=1.21.10 {
        /*return tag.getCompoundOrEmpty(key);
        *///?} else {
        return tag.getCompound(key);
        //?}
    }

    /**
     * A list of compound tags, or an empty list if missing.
     */
    public static ListTag getCompoundList(CompoundTag tag, String key) {
        //? if >=1.21.10 {
        /*return tag.getListOrEmpty(key);
        *///?} else {
        return tag.getList(key, Tag.TAG_COMPOUND);
        //?}
    }

    /**
     * Stored as a 4-int array on every version, so existing saves stay readable.
     */
    public static void putUUID(CompoundTag tag, String key, UUID uuid) {
        //? if >=1.21.10 {
        /*tag.putIntArray(key, UUIDUtil.uuidToIntArray(uuid));
        *///?} else {
        tag.putUUID(key, uuid);
        //?}
    }

    @Nullable
    public static UUID getUUID(CompoundTag tag, String key) {
        //? if >=1.21.10 {
        /*return tag.getIntArray(key)
                .filter(ints -> ints.length == 4)
                .map(UUIDUtil::uuidFromIntArray)
                .orElse(null);
        *///?} else {
        return tag.hasUUID(key) ? tag.getUUID(key) : null;
        //?}
    }

    public static boolean hasUUID(CompoundTag tag, String key) {
        return getUUID(tag, key) != null;
    }

    /**
     * Looks up an entity type by id. Unknown ids resolve to the registry default (pig), as before.
     */
    public static EntityType<?> entityType(String id) {
        //? if >=1.21.10 {
        /*return BuiltInRegistries.ENTITY_TYPE.getValue(new CompatResourceLocation(id).unwrap());
        *///?} else {
        return BuiltInRegistries.ENTITY_TYPE.get(new CompatResourceLocation(id).unwrap());
        //?}
    }

    @Nullable
    public static Entity createEntity(EntityType<?> type, Level level) {
        //? if >=1.21.10 {
        /*return type.create(level, EntitySpawnReason.LOAD);
        *///?} else {
        return type.create(level);
        //?}
    }

    /**
     * Saves an entity, including its type id, to a new tag. Unlike Entity#save this also works
     * for entities that are riding something.
     */
    public static CompoundTag saveEntity(Entity entity) {
        CompoundTag tag = saveEntityWithoutId(entity);
        tag.putString("id", EntityType.getKey(entity.getType()).toString());
        return tag;
    }

    public static CompoundTag saveEntityWithoutId(Entity entity) {
        //? if >=1.21.10 {
        /*TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.level().registryAccess());
        entity.saveWithoutId(out);
        return out.buildResult();
        *///?} else {
        CompoundTag tag = new CompoundTag();
        entity.saveWithoutId(tag);
        return tag;
        //?}
    }

    /**
     * The entity's custom name as plain text, or null if it has none. Handles both the JSON-string
     * form (pre-1.21.5 saves, client-side updates) and the component-tag form (1.21.5+).
     */
    @Nullable
    public static String getCustomName(CompoundTag tag) {
        if (!tag.contains("CustomName")) {
            return null;
        }
        //? if >=1.21.10 {
        /*Tag nameTag = tag.get("CustomName");
        if (nameTag instanceof StringTag stringTag) {
            try {
                MutableComponent parsed = CompatComponent.fromJson(stringTag.value());
                if (parsed != null) {
                    return parsed.getString();
                }
            } catch (RuntimeException ignored) {
                // Not JSON; fall through and let the codec read it as plain text
            }
        }
        return ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, nameTag).result()
                .map(Component::getString)
                .orElse(null);
        *///?} else {
        MutableComponent parsed = CompatComponent.fromJson(tag.getString("CustomName"));
        return parsed != null ? parsed.getString() : null;
        //?}
    }

    public static void loadEntity(Entity entity, CompoundTag tag) {
        //? if >=1.21.10 {
        /*entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.level().registryAccess(), tag));
        *///?} else {
        entity.load(tag);
        //?}
    }

    public static void moveTo(Entity entity, double x, double y, double z, float yRot, float xRot) {
        //? if >=1.21.10 {
        /*entity.snapTo(x, y, z, yRot, xRot);
        *///?} else {
        entity.moveTo(x, y, z, yRot, xRot);
        //?}
    }
}
