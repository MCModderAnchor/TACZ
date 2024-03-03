package com.tac.guns.item;

import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.renderer.tileentity.TileEntityItemStackGunRenderer;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.entity.EntityBullet;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.nbt.GunItemData;
import com.tac.guns.resource.CommonGunPackLoader;
import com.tac.guns.resource.index.CommonGunIndex;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IItemRenderProperties;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Consumer;

public class GunItem extends Item implements IGun {
    public GunItem() {
        super(new Properties().stacksTo(1).tab(ModItems.GUN_TAB));
    }

    public static @Nonnull GunItemData getData(@Nonnull ItemStack itemStack) {
        if (IGun.isGun(itemStack)) {
            return GunItemData.deserialization(itemStack.getOrCreateTag());
        }
        return new GunItemData();
    }

    public static ItemStack setData(@Nonnull ItemStack stack, @Nonnull GunItemData data) {
        if (IGun.isGun(stack)) {
            GunItemData.serialization(stack.getOrCreateTag(), data);
        }
        return stack;
    }

    @Override
    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        ResourceLocation gunId = getData(stack).getGunId();
        Optional<ClientGunIndex> gunIndex = ClientGunPackLoader.getGunIndex(gunId);
        if (gunIndex.isPresent()) {
            return new TranslatableComponent(gunIndex.get().getName());
        }
        return super.getName(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillItemCategory(@Nonnull CreativeModeTab modeTab, @Nonnull NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(modeTab)) {
            ClientGunPackLoader.getAllGuns().forEach(entry -> {
                GunItemData data = new GunItemData();
                data.setGunId(entry.getKey());
                stacks.add(setData(this.getDefaultInstance(), data));
            });
        }
    }

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        consumer.accept(new IItemRenderProperties() {
            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                return new TileEntityItemStackGunRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            }
        });
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean isAmmo(ItemStack gun, ItemStack ammo) {
        return false;
    }

    @Override
    public void reload(ItemStack gun) {
    }

    @Override
    public void shoot(LivingEntity shooter, ItemStack gun, float pitch, float yaw) {
        ResourceLocation gunId = GunItem.getData(gun).getGunId();
        Optional<CommonGunIndex> gunIndexOptional = CommonGunPackLoader.getGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            return;
        }
        // todo 获取 GunData 并根据其中的弹道参数创建 EntityBullet
        Level world = shooter.getLevel();
        EntityBullet bullet = new EntityBullet(world, shooter);
        bullet.shootFromRotation(bullet, pitch, yaw, 0.0F, 10, 0);
        world.addFreshEntity(bullet);
    }

    @Override
    public FireMode getFireMode(ItemStack gun) {
        // FIXME 应该还需要检查是否允许这种射击类型
        return GunItem.getData(gun).getFireMode();
    }
}
