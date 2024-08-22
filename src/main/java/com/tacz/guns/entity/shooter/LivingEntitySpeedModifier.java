package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.ExtraMovementModifier;
import com.tacz.guns.resource.modifier.custom.WeightModifier;
import com.tacz.guns.resource.pojo.data.gun.MoveSpeed;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class LivingEntitySpeedModifier {
    private static final UUID EXTRA_SPEED_MODIFIER_UUID = UUID.fromString("4D5696AE-A7C5-C59C-80E9-2A2DC8373C46");
    private static final UUID WEIGHT_SPEED_MODIFIER_UUID = UUID.fromString("2CB6F5AD-C6D2-9D29-4E84-0856ACD47CDB");
    private final LivingEntity shooter;
    private final ShooterDataHolder dataHolder;
    public LivingEntitySpeedModifier(LivingEntity shooter, ShooterDataHolder dataHolder) {
        this.shooter = shooter;
        this.dataHolder = dataHolder;
    }

    public void updateSpeedModifier() {
        if (!shooter.isAlive()) return;

        ItemStack stack = shooter.getMainHandItem();
        var speedModifier = shooter.getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
        if (speedModifier == null) return;

        if (stack.getItem() instanceof AbstractGunItem) {
            // 处理重量带来的修正
            AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
            if (cacheProperty != null) {
                double weightFactor = SyncConfig.WEIGHT_SPEED_MULTIPLIER.get();
                if(weightFactor > 0){
                    float targetSpeed = cacheProperty.getCache(WeightModifier.ID);
                    targetSpeed *= (float) -weightFactor;
                    AttributeModifier currentModifier = speedModifier.getModifier(WEIGHT_SPEED_MODIFIER_UUID);
                    if (currentModifier == null || currentModifier.getAmount() != targetSpeed) {
                        speedModifier.removeModifier(WEIGHT_SPEED_MODIFIER_UUID);
                        speedModifier.addTransientModifier(new AttributeModifier(WEIGHT_SPEED_MODIFIER_UUID, "Gun Speed Modifier",
                                targetSpeed, AttributeModifier.Operation.MULTIPLY_BASE));
                    }
                }

                MoveSpeed speed = cacheProperty.getCache(ExtraMovementModifier.ID);
                if (speed != null) {
                    double targetSpeed = getTargetSpeed(speed);
                    AttributeModifier currentModifier = speedModifier.getModifier(EXTRA_SPEED_MODIFIER_UUID);
                    if (currentModifier == null || currentModifier.getAmount() != targetSpeed) {
                        speedModifier.removeModifier(EXTRA_SPEED_MODIFIER_UUID);
                        speedModifier.addTransientModifier(new AttributeModifier(EXTRA_SPEED_MODIFIER_UUID, "Extra Gun Speed Modifier",
                                targetSpeed, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                }
            }
        } else {
            speedModifier.removeModifier(WEIGHT_SPEED_MODIFIER_UUID);
            speedModifier.removeModifier(EXTRA_SPEED_MODIFIER_UUID);
        }
    }

    private double getTargetSpeed(MoveSpeed moveSpeed) {
        if (dataHolder.reloadStateType.isReloading()) {
            return moveSpeed.getReloadMultiplier();
        }
        if (dataHolder.isAiming) {
            return moveSpeed.getAimMultiplier();
        }
        return moveSpeed.getBaseMultiplier();
    }

}
