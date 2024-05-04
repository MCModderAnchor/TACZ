package com.tacz.guns.item;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.attachment.AttachmentType;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.client.renderer.item.AttachmentItemRenderer;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.attachment.RecoilModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.IItemRenderProperties;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AttachmentItem extends Item implements AttachmentItemDataAccessor {
    public AttachmentItem() {
        super(new Properties().stacksTo(1).tab(ModItems.ATTACHMENT_TAB));
    }

    @Override
    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public Component getName(@Nonnull ItemStack stack) {
        ResourceLocation attachmentId = this.getAttachmentId(stack);
        Optional<ClientAttachmentIndex> attachmentIndex = TimelessAPI.getClientAttachmentIndex(attachmentId);
        if (attachmentIndex.isPresent()) {
            return new TranslatableComponent(attachmentIndex.get().getName());
        }
        return super.getName(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillItemCategory(@Nonnull CreativeModeTab modeTab, @Nonnull NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(modeTab)) {
            TimelessAPI.getAllClientAttachmentIndex().forEach(entry -> {
                ItemStack itemStack = this.getDefaultInstance();
                this.setAttachmentId(itemStack, entry.getKey());
                stacks.add(itemStack);
            });
        }
    }

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        consumer.accept(new IItemRenderProperties() {
            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                return new AttachmentItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
            }
        });
    }

    @Override
    @Nonnull
    public AttachmentType getType(ItemStack attachmentStack) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        if (iAttachment != null) {
            ResourceLocation id = iAttachment.getAttachmentId(attachmentStack);
            return TimelessAPI.getCommonAttachmentIndex(id).map(CommonAttachmentIndex::getType).orElse(AttachmentType.NONE);
        } else {
            return AttachmentType.NONE;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag isAdvanced) {
        ResourceLocation attachmentId = getAttachmentId(stack);
        AttachmentType type = getType(stack);
        TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresent(index -> {
            AttachmentData data = index.getData();

            if (type == AttachmentType.SCOPE) {
                float[] zoom = index.getZoom();
                if (zoom != null) {
                    String[] zoomText = new String[zoom.length];
                    for (int i = 0; i < zoom.length; i++) {
                        zoomText[i] = "x" + zoom[i];
                    }
                    String zoomJoinText = StringUtils.join(zoomText, ", ");
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.zoom", zoomJoinText).withStyle(ChatFormatting.GOLD));
                }
            }

            if (type == AttachmentType.EXTENDED_MAG) {
                int magLevel = data.getExtendedMagLevel();
                if (magLevel == 1) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.extended_mag_level_1").withStyle(ChatFormatting.GRAY));
                } else if (magLevel == 2) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.extended_mag_level_2").withStyle(ChatFormatting.BLUE));
                } else if (magLevel == 3) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.extended_mag_level_3").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }

            float adsAddendTime = data.getAdsAddendTime();
            if (adsAddendTime > 0) {
                components.add(new TranslatableComponent("tooltip.tacz.attachment.ads.increase").withStyle(ChatFormatting.RED));
            } else if (adsAddendTime < 0) {
                components.add(new TranslatableComponent("tooltip.tacz.attachment.ads.decrease").withStyle(ChatFormatting.GREEN));
            }

            float inaccuracyAddend = data.getInaccuracyAddend();
            if (inaccuracyAddend > 0) {
                components.add(new TranslatableComponent("tooltip.tacz.attachment.inaccuracy.increase").withStyle(ChatFormatting.RED));
            } else if (inaccuracyAddend < 0) {
                components.add(new TranslatableComponent("tooltip.tacz.attachment.inaccuracy.decrease").withStyle(ChatFormatting.GREEN));
            }

            RecoilModifier recoilModifier = data.getRecoilModifier();
            if (recoilModifier != null) {
                float pitch = recoilModifier.getPitch();
                if (pitch > 0) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.pitch.increase").withStyle(ChatFormatting.RED));
                } else if (pitch < 0) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.pitch.decrease").withStyle(ChatFormatting.GREEN));
                }

                float yaw = recoilModifier.getYaw();
                if (yaw > 0) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.yaw.increase").withStyle(ChatFormatting.RED));
                } else if (yaw < 0) {
                    components.add(new TranslatableComponent("tooltip.tacz.attachment.yaw.decrease").withStyle(ChatFormatting.GREEN));
                }
            }
        });
    }
}
