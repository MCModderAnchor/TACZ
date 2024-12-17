package com.tacz.guns.crafting.result;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GunSmithTableResult {
    public static final String GUN = "gun";
    public static final String AMMO = "ammo";
    public static final String ATTACHMENT = "attachment";
    public static final String CUSTOM = "custom";

    private ItemStack result = ItemStack.EMPTY;
    private String group = "";

    @Nullable
    private RawGunTableResult raw = null;

    public GunSmithTableResult(@NotNull RawGunTableResult raw) {
        this.raw = raw;
    }

    public void init() {
        if (raw != null) {
            GunSmithTableResult result = RawGunTableResult.init(raw);
            this.result = result.getResult();
            this.group = result.getGroup();
            this.raw = null;
        }
    }

    public GunSmithTableResult(ItemStack result, String group) {
        this.result = result;
        this.group = group;
    }

    public ItemStack getResult() {
        return result;
    }

    public String getGroup() {
        return group;
    }
}
