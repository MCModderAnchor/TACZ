package com.tac.guns.item;

import com.tac.guns.GunMod;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.item.IAmmo;
import com.tac.guns.api.item.IAmmoBox;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.builder.AmmoItemBuilder;
import com.tac.guns.resource.DefaultAssets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class AmmoBoxItem extends Item implements DyeableLeatherItem, IAmmoBox {
    public static final ResourceLocation PROPERTY_NAME = new ResourceLocation(GunMod.MOD_ID, "ammo_statue");
    private static final int MAX_STACK = 5;
    private static final int OPEN = 0;
    private static final int CLOSE = 1;

    private static final String AMMO_ID_TAG = "AmmoId";
    private static final String AMMO_COUNT_TAG = "AmmoCount";

    public AmmoBoxItem() {
        super(new Properties().stacksTo(1).tab(ModItems.OTHER_TAB));
    }

    @OnlyIn(Dist.CLIENT)
    public static int getColor(ItemStack stack, int tintIndex) {
        return tintIndex > 0 ? -1 : ((DyeableLeatherItem) stack.getItem()).getColor(stack);
    }

    @OnlyIn(Dist.CLIENT)
    public static float getStatue(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (stack.getItem() instanceof IAmmoBox iAmmoBox) {
            boolean idIsEmpty = iAmmoBox.getAmmoId(stack).equals(DefaultAssets.EMPTY_AMMO_ID);
            boolean countIsZero = iAmmoBox.getAmmoCount(stack) <= 0;
            if (idIsEmpty || countIsZero) {
                return OPEN;
            }
            return CLOSE;
        }
        return OPEN;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack pOther, Slot slot, ClickAction action, Player player, SlotAccess access) {
        return super.overrideOtherStackedOnMe(stack, pOther, slot, action, player, access);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack ammoBox, Slot slot, ClickAction action, Player player) {
        // 右击
        if (action == ClickAction.SECONDARY) {
            // 点击的格子
            ItemStack slotItem = slot.getItem();
            ResourceLocation boxAmmoId = this.getAmmoId(ammoBox);

            // 格子为空，那就是取出物品
            if (slotItem.isEmpty()) {
                // 啥也没有，不能取出
                if (boxAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
                    return false;
                }
                // 数量不对，不能取出
                int boxAmmoCount = this.getAmmoCount(ammoBox);
                if (boxAmmoCount <= 0) {
                    return false;
                }
                TimelessAPI.getCommonAmmoIndex(boxAmmoId).ifPresent(index -> {
                    int takeCount = Math.min(index.getStackSize(), boxAmmoCount);
                    ItemStack takeAmmo = AmmoItemBuilder.create().setId(boxAmmoId).setCount(takeCount).build();
                    slot.safeInsert(takeAmmo);

                    int remainCount = boxAmmoCount - takeCount;
                    this.setAmmoCount(ammoBox, remainCount);
                    if (remainCount <= 0) {
                        this.setAmmoId(ammoBox, DefaultAssets.EMPTY_AMMO_ID);
                    }
                    this.playRemoveOneSound(player);
                });
                return true;
            }

            // 如果是子弹
            if (slotItem.getItem() instanceof IAmmo iAmmo) {
                ResourceLocation slotAmmoId = iAmmo.getAmmoId(slotItem);
                // 格子里的子弹 ID 不对，不能放
                if (slotAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
                    return false;
                }
                // 如果盒子的子弹 ID 为空，变成当前点击的类型
                if (boxAmmoId.equals(DefaultAssets.EMPTY_AMMO_ID)) {
                    this.setAmmoId(ammoBox, slotAmmoId);
                } else if (!slotAmmoId.equals(boxAmmoId)) {
                    return false;
                }
                TimelessAPI.getCommonAmmoIndex(slotAmmoId).ifPresent(index -> {
                    int boxAmmoCount = this.getAmmoCount(ammoBox);
                    int needCount = index.getStackSize() * MAX_STACK - boxAmmoCount;
                    ItemStack takeItem = slot.safeTake(slotItem.getCount(), needCount, player);
                    this.setAmmoCount(ammoBox, boxAmmoCount + takeItem.getCount());
                    this.playInsertSound(player);
                });
                return true;
            }
        }
        return false;
    }

    private void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + entity.getLevel().getRandom().nextFloat() * 0.4F);
    }

    private void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + entity.getLevel().getRandom().nextFloat() * 0.4F);
    }

    @Override
    public ResourceLocation getAmmoId(ItemStack ammoBox) {
        CompoundTag tag = ammoBox.getOrCreateTag();
        if (tag.contains(AMMO_ID_TAG, Tag.TAG_STRING)) {
            return new ResourceLocation(tag.getString(AMMO_ID_TAG));
        }
        return DefaultAssets.EMPTY_AMMO_ID;
    }

    @Override
    public void setAmmoId(ItemStack ammoBox, ResourceLocation ammoId) {
        CompoundTag tag = ammoBox.getOrCreateTag();
        tag.putString(AMMO_ID_TAG, ammoId.toString());
    }

    @Override
    public int getAmmoCount(ItemStack ammoBox) {
        CompoundTag tag = ammoBox.getOrCreateTag();
        if (tag.contains(AMMO_COUNT_TAG, Tag.TAG_INT)) {
            return tag.getInt(AMMO_COUNT_TAG);
        }
        return 0;
    }

    @Override
    public void setAmmoCount(ItemStack ammoBox, int count) {
        CompoundTag tag = ammoBox.getOrCreateTag();
        tag.putInt(AMMO_COUNT_TAG, count);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag isAdvanced) {
        int ammoCount = this.getAmmoCount(stack);
        components.add(new TranslatableComponent("tooltip.tac.ammo_box.count", ammoCount).withStyle(ChatFormatting.GRAY));
    }
}
