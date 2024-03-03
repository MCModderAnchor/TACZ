package com.tac.guns.mixin.common;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.api.gun.ShootResult;
import com.tac.guns.api.item.IGun;
import com.tac.guns.item.GunItem;
import com.tac.guns.resource.CommonGunPackLoader;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.resource.pojo.data.GunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IGunOperator {
    @Shadow protected abstract float tickHeadTurn(float p_21260_, float p_21261_);

    @Unique
    private long tac$ShootTimestamp = -1L;

    @Unique
    private long tac$DrawTimestamp = -1L;

    @Unique
    private long tac$ReloadTimestamp = -1L;

    @Override
    public void draw(ItemStack gunItemStack) {
        // todo
    }

    @Override
    public void reload(ItemStack gunItemStack) {
        // todo
    }

    @Override
    public ShootResult shoot(ItemStack gunItemStack, float pitch, float yaw) {
        // 获取GunData
        ResourceLocation gunId = GunItem.getData(gunItemStack).getGunId();
        Optional<CommonGunIndex> gunIndexOptional = CommonGunPackLoader.getGunIndex(gunId);
        if(gunIndexOptional.isEmpty()) {
            return ShootResult.FAIL;
        }
        GunData gunData = gunIndexOptional.get().getGunData();
        // 判断射击是否正在冷却
        if ((System.currentTimeMillis() - tac$ShootTimestamp) < gunData.getShootInterval()) {
            return ShootResult.COOL_DOWN;
        }
        // todo 判断枪械是否有足够的弹药
        // 调用射击方法
        if(gunItemStack.getItem() instanceof IGun iGun){
            iGun.shoot((LivingEntity) (Object) this, gunItemStack, pitch, yaw);
            tac$ShootTimestamp = System.currentTimeMillis();
            return ShootResult.SUCCESS;
        }
        return ShootResult.FAIL;
    }
}
