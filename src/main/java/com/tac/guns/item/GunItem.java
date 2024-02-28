package com.tac.guns.item;

import com.tac.guns.client.animation.ObjectAnimation;
import com.tac.guns.client.input.CheckGunKey;
import com.tac.guns.client.renderer.tileentity.TileEntityItemStackGunRenderer;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import com.tac.guns.entity.EntityBullet;
import com.tac.guns.init.ModSounds;
import com.tac.guns.item.nbt.GunItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.IItemRenderProperties;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class GunItem extends Item {
    public GunItem() {
        super(new Properties().stacksTo(1));
    }

    public static @Nonnull GunItemData getData(@Nonnull ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem) {
            return GunItemData.deserialization(itemStack.getOrCreateTag());
        }
        return new GunItemData();
    }

    public static ItemStack setData(@Nonnull ItemStack stack, @Nonnull GunItemData data) {
        if (stack.getItem() instanceof GunItem) {
            GunItemData.serialization(stack.getOrCreateTag(), data);
        }
        return stack;
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
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            if (!world.isClientSide) {
                EntityBullet bullet = new EntityBullet(world, player);
                bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 10, 0);
                world.addFreshEntity(bullet);
            } else {
                if (CheckGunKey.AK47_FIRE == null) {
                    CheckGunKey.AK47_FIRE = ClientAssetManager.INSTANCE.getBedrockAnimatedAsset(new ResourceLocation("tac", "ak47")).defaultController();
                }
                CheckGunKey.AK47_FIRE.runAnimation(0, "shoot", ObjectAnimation.PlayType.PLAY_ONCE_HOLD, 0.02f);
                player.setXRot(player.getXRot() - 1);
            }
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FIRE.get(), player.getSoundSource(), 1.0f, 0.8f);
        return super.use(world, player, hand);
    }
}
