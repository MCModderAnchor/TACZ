package com.tac.guns.item.level;

import com.tac.guns.api.event.GunLevelEvent;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

public class GunLevelManager {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 10;
    public static final IntList DAMAGE_UP_LEVELS = IntList.of(5, 8);
    public static final String GUN_LEVEL_TAG = "GunLevel";
    public static final String GUN_LEVEL_EXP_TAG = "GunLevelExp";
    public static final String GUN_LEVEL_LOCK_TAG = "GunLevelLock";
    public static final String GUN_LEVEL_OWNER_TAG = "GunLevelOwner";
    public static final String GUN_LEVEL_OWNER_UUID_TAG = "GunLevelOwnerUUID";

    public static void init(ItemStack gun) {
        CompoundTag nbt = gun.getOrCreateTag();
        // 初始化等级
        if (!nbt.contains(GUN_LEVEL_TAG, Tag.TAG_INT)) {
            nbt.putInt(GUN_LEVEL_TAG, MIN_LEVEL);
        }
        // 初始化经验值
        if (!nbt.contains(GUN_LEVEL_EXP_TAG, Tag.TAG_FLOAT)) {
            nbt.putFloat(GUN_LEVEL_EXP_TAG, 0);
        }
        // 初始化玩家锁
        if (!nbt.contains(GUN_LEVEL_LOCK_TAG, Tag.TAG_BYTE)) {
            nbt.putBoolean(GUN_LEVEL_LOCK_TAG, false);
        }
    }

    public static void checkUser(ItemStack gun, Player user) {
        CompoundTag nbt = gun.getOrCreateTag();
        // 初始化拥有者的 ID，用于在玩家离线时显示名称
        if (!nbt.contains(GUN_LEVEL_OWNER_TAG, Tag.TAG_STRING)) {
            nbt.putString(GUN_LEVEL_OWNER_TAG, user.getScoreboardName());
        }
        // 初始化拥有者的 UUID
        if (!nbt.contains(GUN_LEVEL_OWNER_UUID_TAG, Tag.TAG_INT_ARRAY)) {
            nbt.putUUID(GUN_LEVEL_OWNER_UUID_TAG, user.getUUID());
        }
        // 判断使用者是否是拥有者，如果不是，激活等级锁
        nbt.putBoolean(GUN_LEVEL_LOCK_TAG, !nbt.getUUID(GUN_LEVEL_OWNER_UUID_TAG).equals(user.getUUID()));
    }

    public static void levelUp(ItemStack gun, float damage, Player shooter) {
        CompoundTag nbt = gun.getOrCreateTag();
        // 如果武器处于锁定状态或者达到最大等级，则不升级
        if (nbt.getBoolean(GUN_LEVEL_LOCK_TAG) || nbt.getInt(GUN_LEVEL_TAG) >= MAX_LEVEL) {
            return;
        }
        int level = nbt.getInt(GUN_LEVEL_TAG);
        float exp = nbt.getFloat(GUN_LEVEL_EXP_TAG);
        float expNeeded = getExpNeeded(gun, level);
        // 触发升级
        if (exp + damage >= expNeeded) {
            int currentLevel = level + 1;
            nbt.putInt(GUN_LEVEL_TAG, currentLevel);
            nbt.putFloat(GUN_LEVEL_EXP_TAG, 0);
            MinecraftForge.EVENT_BUS.post(new GunLevelEvent(shooter, gun, currentLevel));
        } else {
            nbt.putFloat(GUN_LEVEL_EXP_TAG, exp + damage);
        }
    }

    // TODO 计算升级所需经验，当前设置仅测试用
    public static float getExpNeeded(ItemStack gun, float level) {
        return (level + 1) * 500;
    }
}
