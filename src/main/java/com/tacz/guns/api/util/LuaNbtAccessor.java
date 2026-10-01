package com.tacz.guns.api.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Basic NBT access for Lua, with copy-on-write updates for item components. */
@SuppressWarnings("unused")
public final class LuaNbtAccessor {
    private final Supplier<CompoundTag> reader;
    private final Consumer<Consumer<CompoundTag>> writer;

    public LuaNbtAccessor(CompoundTag nbt) {
        this(() -> nbt, change -> change.accept(nbt));
    }

    private LuaNbtAccessor(Supplier<CompoundTag> reader, Consumer<Consumer<CompoundTag>> writer) {
        this.reader = reader;
        this.writer = writer;
    }

    /** Read the latest component on every access; never retain a stale snapshot. */
    public static LuaNbtAccessor from(ItemStack stack) {
        return new LuaNbtAccessor(
                () -> stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(),
                change -> stack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY,
                        data -> data.update(change)));
    }

    public static LuaNbtAccessor from(CompoundTag nbt) { return new LuaNbtAccessor(nbt); }
    public boolean contains(String key) { return reader.get().contains(key); }
    public boolean contains(String key, int type) { return reader.get().contains(key, type); }
    public LuaNbtAccessor newCompoundTag() { return from(new CompoundTag()); }
    public int getInt(String key) { return reader.get().getInt(key); }
    public double getDouble(String key) { return reader.get().getDouble(key); }
    public float getFloat(String key) { return reader.get().getFloat(key); }
    public long getLong(String key) { return reader.get().getLong(key); }
    public String getString(String key) { return reader.get().getString(key); }
    public boolean getBoolean(String key) { return reader.get().getBoolean(key); }
    /** Retained for scripts using the old two-argument API. */
    public boolean getBoolean(CompoundTag nbt, String key) { return nbt.getBoolean(key); }

    public LuaNbtAccessor getCompound(String key) {
        if (!reader.get().contains(key, Tag.TAG_COMPOUND)) return null;
        return new LuaNbtAccessor(() -> reader.get().getCompound(key), change -> writer.accept(root -> {
            if (!root.contains(key, Tag.TAG_COMPOUND)) root.put(key, new CompoundTag());
            change.accept(root.getCompound(key));
        }));
    }

    public void putInt(String key, int value) { writer.accept(tag -> tag.putInt(key, value)); }
    public void putDouble(String key, double value) { writer.accept(tag -> tag.putDouble(key, value)); }
    public void putFloat(String key, float value) { writer.accept(tag -> tag.putFloat(key, value)); }
    public void putLong(String key, long value) { writer.accept(tag -> tag.putLong(key, value)); }
    public void putString(String key, String value) { writer.accept(tag -> tag.putString(key, value)); }
    public void putBoolean(String key, boolean value) { writer.accept(tag -> tag.putBoolean(key, value)); }
    public void putCompound(String key, LuaNbtAccessor value) {
        if (value != null) writer.accept(tag -> tag.put(key, value.nbt()));
    }

    /** A detached value: changes must go through the accessor's put methods. */
    @ApiStatus.Internal
    public CompoundTag nbt() { return reader.get().copy(); }
}
