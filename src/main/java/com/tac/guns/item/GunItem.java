package com.tac.guns.item;

import com.tac.guns.api.entity.IShooter;
import com.tac.guns.api.item.IGun;
import com.tac.guns.api.item.ShootResult;
import com.tac.guns.client.renderer.tileentity.TileEntityItemStackGunRenderer;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.entity.EntityBullet;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.nbt.GunItemData;
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
    @OnlyIn(Dist.CLIENT)
    public Component getName(ItemStack stack) {
        ResourceLocation gunId = getData(stack).getGunId();
        Optional<ClientGunIndex> gunIndex = ClientGunLoader.getGunIndex(gunId);
        if (gunIndex.isPresent()) {
            return new TranslatableComponent(gunIndex.get().getName());
        }
        return super.getName(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillItemCategory(CreativeModeTab modeTab, NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(modeTab)) {
            ClientGunLoader.getAllGuns().forEach(id -> {
                GunItemData data = new GunItemData();
                data.setGunId(id);
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
    public ShootResult shoot(LivingEntity entity, ItemStack gun) {
        if (entity instanceof IShooter shooter) {
            // TODO 服务端应该检测间隔
            Level world = entity.level;
            EntityBullet bullet = new EntityBullet(world, entity);
            bullet.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, 10, 0);
            world.addFreshEntity(bullet);
            shooter.recordShootTime();
            return ShootResult.SUCCESS;
        }
        return ShootResult.FAIL;
    }
}
