package com.tac.guns.entity;

import com.mojang.authlib.GameProfile;
import com.tac.guns.api.entity.ITargetEntity;
import com.tac.guns.config.client.RenderConfig;
import com.tac.guns.init.ModBlocks;
import com.tac.guns.init.ModItems;
import com.tac.guns.init.ModSounds;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.entity.vehicle.AbstractMinecart.Type.RIDEABLE;

public class TargetMinecart extends AbstractMinecart implements ITargetEntity {
    public static EntityType<TargetMinecart> TYPE = EntityType.Builder.<TargetMinecart>of(TargetMinecart::new, MobCategory.MISC)
            .sized(0.75F, 2.4F)
            .clientTrackingRange(8)
            .build("target_minecart");

    private @Nullable GameProfile gameProfile = null;

    public TargetMinecart(EntityType<TargetMinecart> type, Level world) {
        super(type, world);
    }

    public TargetMinecart(Level level, double x, double y, double z) {
        super(TYPE, level, x, y, z);
    }

    @Override
    public boolean onProjectileHit(Entity p, EntityHitResult result, DamageSource source, float damage) {
        if (!this.level.isClientSide() && !this.isRemoved()) {
            if (source instanceof IndirectEntityDamageSource) {
                Entity e = source.getEntity();
                if (e instanceof Player player) {
                    this.setHurtDir(-1);
                    this.setHurtTime(10);
                    this.markHurt();
                    this.setDamage(10);
                    double dis = this.position().distanceTo(e.position());
                    player.displayClientMessage(new TranslatableComponent("message.tac.target_minecart.hit",
                            String.format("%.1f", damage), String.format("%.2f", dis)), true);
                    level.playSound(null, this, ModSounds.TARGET_HIT.get(), SoundSource.BLOCKS, 0.8f, this.level.random.nextFloat() * 0.1F + 0.9F);
                }
            }
        }
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.isExplosion() || super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBeRidden() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize();
        if (Double.isNaN(size)) {
            size = 1.0;
        }
        size *= RenderConfig.TARGET_RENDER_DISTANCE.get() * getViewScale();
        return distance < size * size;
    }

    @Override
    public void destroy(DamageSource source) {
        this.remove(Entity.RemovalReason.KILLED);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack itemStack = new ItemStack(ModItems.TARGET_MINECART.get());
            if (this.hasCustomName()) {
                itemStack.setHoverName(this.getCustomName());
            }
            this.spawnAtLocation(itemStack);
        }
    }

    @Override
    public ItemStack getPickResult() {
        ItemStack itemStack = new ItemStack(ModItems.TARGET_MINECART.get());
        if (this.hasCustomName()) {
            itemStack.setHoverName(this.getCustomName());
        }
        return itemStack;
    }

    @Nullable
    public GameProfile getGameProfile() {
        if (this.gameProfile == null && this.getCustomName() != null) {
            this.gameProfile = new GameProfile(null, this.getCustomName().getString());
        }
        return gameProfile;
    }

    @Override
    @NotNull
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.TARGET.get().defaultBlockState();
    }

    @NotNull
    @Override
    public Type getMinecartType() {
        return RIDEABLE;
    }

    @Override
    public float getMaxCartSpeedOnRail() {
        return 0.2F;
    }
}
