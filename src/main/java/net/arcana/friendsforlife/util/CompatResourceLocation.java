package net.arcana.friendsforlife.util;

import net.minecraft.resources.ResourceLocation;

public final class CompatResourceLocation {
    private final ResourceLocation value;

    public CompatResourceLocation(String namespaceAndPath) {
        //? if <1.21 {
        /*this.value = new ResourceLocation(namespaceAndPath);
         *///?} else
        this.value = ResourceLocation.tryParse(namespaceAndPath);
    }
    public CompatResourceLocation(String namespace, String path) {
        //? if <1.21 {
        /*this.value = new ResourceLocation(namespace, path);
        *///?} else
        this.value = ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public ResourceLocation unwrap() {
        return this.value;
    }
}
