package com.tac.guns.item;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.renderer.item.GunItemRenderer;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.tab.CustomTab;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.builder.GunItemBuilder;
import com.tac.guns.item.nbt.GunItemDataAccessor;
import com.tac.guns.resource.pojo.data.gun.AttachmentPass;
import com.tac.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IItemRenderProperties;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class GunItem extends Item implements GunItemDataAccessor {
    public GunItem() {
        super(new Properties().stacksTo(1).tab(ModItems.OTHER_TAB));
    }

    @Override
    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        ResourceLocation gunId = this.getGunId(stack);
        Optional<ClientGunIndex> gunIndex = TimelessAPI.getClientGunIndex(gunId);
        if (gunIndex.isPresent()) {
            return new TranslatableComponent(gunIndex.get().getName());
        }
        return super.getName(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillItemCategory(@Nonnull CreativeModeTab modeTab, @Nonnull NonNullList<ItemStack> stacks) {
        if (modeTab instanceof CustomTab tab) {
            String key = tab.getKey();
            TimelessAPI.getAllClientGunIndex().forEach(entry -> {
                ClientGunIndex index = entry.getValue();
                if (key.equals(index.getType())) {
                    GunData gunData = index.getGunData();
                    ItemStack itemStack = GunItemBuilder.create()
                            .setId(entry.getKey())
                            .setFireMode(gunData.getFireModeSet().get(0))
                            .setAmmoCount(gunData.getAmmoAmount())
                            .build();
                    stacks.add(itemStack);
                }
            });
        }
    }

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        consumer.accept(new IItemRenderProperties() {
            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                return new GunItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            }
        });
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean allowAttachment(ItemStack gun, ItemStack attachmentItem) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null && iAttachment != null) {
            AttachmentType type = iAttachment.getType(attachmentItem);
            ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentItem);
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).map(gunIndex -> {
                Map<AttachmentType, AttachmentPass> map = gunIndex.getGunData().getAllowAttachments();
                if (map == null) {
                    return false;
                }
                AttachmentPass pass = map.get(type);
                if (pass == null) {
                    return false;
                }
                return pass.isAllow(attachmentId);
            }).orElse(false);
        } else {
            return false;
        }
    }

    @Override
    public boolean allowAttachmentType(ItemStack gun, AttachmentType type) {
        IGun iGun = IGun.getIGunOrNull(gun);
        if (iGun != null) {
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(gun)).map(gunIndex -> {
                Map<AttachmentType, AttachmentPass> map = gunIndex.getGunData().getAllowAttachments();
                if (map == null) {
                    return false;
                }
                return map.containsKey(type);
            }).orElse(false);
        } else {
            return false;
        }
    }
}
