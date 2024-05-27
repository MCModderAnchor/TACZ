package com.tacz.guns.compat.jei.entry;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.compat.jei.category.AttachmentQueryCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

public class AttachmentQueryEntry {
    /**
     * 显示的配件
     */
    private final ItemStack attachmentStack;
    /**
     * 用于能显示的枪械
     */
    private List<ItemStack> allowGunStacks;
    /**
     * 如果是显示不下的，那么全放这里
     */
    private List<ItemStack> extraAllowGunStacks;

    public AttachmentQueryEntry(ResourceLocation attachmentId) {
        this.attachmentStack = AttachmentItemBuilder.create().setId(attachmentId).build();
        this.allowGunStacks = Lists.newArrayList();
        this.extraAllowGunStacks = Lists.newArrayList();
        this.addAllAllowGuns();
        this.dividedGuns();
    }

    public static List<AttachmentQueryEntry> getAllAttachmentQueryEntries() {
        List<AttachmentQueryEntry> entries = Lists.newArrayList();
        TimelessAPI.getAllCommonAttachmentIndex().forEach(entry -> entries.add(new AttachmentQueryEntry(entry.getKey())));
        return entries;
    }

    public ItemStack getAttachmentStack() {
        return attachmentStack;
    }

    public List<ItemStack> getAllowGunStacks() {
        return allowGunStacks;
    }

    public List<ItemStack> getExtraAllowGunStacks() {
        return extraAllowGunStacks;
    }

    private void addAllAllowGuns() {
        TimelessAPI.getAllCommonGunIndex().forEach(entry -> {
            ResourceLocation gunId = entry.getKey();
            ItemStack gun = GunItemBuilder.create().setId(gunId).build();
            if (!(gun.getItem() instanceof IGun iGun)) {
                return;
            }
            if (iGun.allowAttachment(gun, this.attachmentStack)) {
                this.allowGunStacks.add(gun);
            }
        });
    }

    private void dividedGuns() {
        int size = this.allowGunStacks.size();
        if (size >= AttachmentQueryCategory.MAX_GUN_SHOW_COUNT) {
            this.extraAllowGunStacks = this.allowGunStacks.subList(AttachmentQueryCategory.MAX_GUN_SHOW_COUNT, size);
            this.allowGunStacks = this.allowGunStacks.subList(0, AttachmentQueryCategory.MAX_GUN_SHOW_COUNT);
        }
    }
}
