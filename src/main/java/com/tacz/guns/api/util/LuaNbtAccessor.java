package com.tacz.guns.api.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * 一个简单的NBT包装，用于在Lua中访问NBT数据。<br/>
 * 暂时只支持基本数据类型的读写，不支持数组等复杂数据类型。
 */
@SuppressWarnings("unused")
public record LuaNbtAccessor(CompoundTag nbt) {

    public static LuaNbtAccessor from(ItemStack stack) {
        return new LuaNbtAccessor(stack.getTag());
    }

    public static LuaNbtAccessor from(CompoundTag nbt) {
        return new LuaNbtAccessor(nbt);
    }

    public boolean contains(String key) {
        return nbt.contains(key);
    }

    public boolean contains(String key, int type) {
        return nbt.contains(key, type);
    }

    public LuaNbtAccessor newCompoundTag() {
        return new LuaNbtAccessor(new CompoundTag());
    }

    public int getInt(String key) {
        return nbt.getInt(key);
    }

    public double getDouble(String key) {
        return nbt.getDouble(key);
    }

    public float getFloat(String key) {
        return nbt.getFloat(key);
    }

    public long getLong(String key) {
        return nbt.getLong(key);
    }

    public String getString(String key) {
        return nbt.getString(key);
    }

    public boolean getBoolean(CompoundTag nbt, String key) {
        return nbt.getBoolean(key);
    }

    public LuaNbtAccessor getCompound(String key) {
        if (!nbt.contains(key, Tag.TAG_COMPOUND)) {
            return null;
        }
        return from(nbt.getCompound(key));
    }

    public void putInt(String key, int value) {
        nbt.putInt(key, value);
    }

    public void putDouble(String key, double value) {
        nbt.putDouble(key, value);
    }

    public void putFloat(String key, float value) {
        nbt.putFloat(key, value);
    }

    public void putLong(String key, long value) {
        nbt.putLong(key, value);
    }

    public void putString(String key, String value) {
        nbt.putString(key, value);
    }

    public void putBoolean(String key, boolean value) {
        nbt.putBoolean(key, value);
    }

    /**
     * 向当前的NbtCompound中添加一个新的Compound
     *
     * @param key   键
     * @param value 在脚本中请使用{@link LuaNbtAccessor#newCompoundTag()}创建一个新的LuaNbtAccessor对象
     */
    public void putCompound(String key, LuaNbtAccessor value) {
        if (value != null) {
            nbt.put(key, value.nbt());
        }
    }

    @Override
    @ApiStatus.Internal
    public CompoundTag nbt() {
        return nbt;
    }
}
