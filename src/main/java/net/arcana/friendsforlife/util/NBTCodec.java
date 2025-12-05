package net.arcana.friendsforlife.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTCodec {
    public static byte[] serialize(CompoundTag tag) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            NbtIo.writeCompressed(tag, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize NBT", e);
        }
    }

    public static CompoundTag deserialize(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            //? if >=1.21 {
            return NbtIo.readCompressed(bais, NbtAccounter.create(2 * 1024 * 1024));
            //?} else {
            /*return NbtIo.readCompressed(bais);
            *///?}
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize NBT", e);
        }
    }
}
