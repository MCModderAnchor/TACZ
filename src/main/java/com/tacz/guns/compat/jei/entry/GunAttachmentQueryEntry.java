package com.tacz.guns.compat.jei.entry;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 一把枪的某种配件类型的一页查询结果。
 */
public class GunAttachmentQueryEntry {
    public static final int ATTACHMENTS_PER_PAGE = 54;

    private final ItemStack gunStack;
    private final AttachmentType attachmentType;
    private final List<ItemStack> attachmentStacks;
    private final int totalAttachmentCount;
    private final int pageNumber;
    private final int pageCount;

    private GunAttachmentQueryEntry(ItemStack gunStack, AttachmentType attachmentType,
                                    List<ItemStack> attachmentStacks, int totalAttachmentCount,
                                    int pageNumber, int pageCount) {
        this.gunStack = gunStack;
        this.attachmentType = attachmentType;
        this.attachmentStacks = List.copyOf(attachmentStacks);
        this.totalAttachmentCount = totalAttachmentCount;
        this.pageNumber = pageNumber;
        this.pageCount = pageCount;
    }

    public static List<GunAttachmentQueryEntry> getAllGunAttachmentQueryEntries() {
        // 与原有配件查询一致，隐藏配件不参与 JEI 展示。
        Map<AttachmentType, List<ItemStack>> attachmentsByType = new EnumMap<>(AttachmentType.class);
        TimelessAPI.getAllCommonAttachmentIndex().stream()
                .filter(entry -> !entry.getValue().getPojo().isHidden())
                .filter(entry -> entry.getValue().getType() != AttachmentType.NONE)
                .sorted(Comparator.<Map.Entry<ResourceLocation, CommonAttachmentIndex>>comparingInt(entry -> entry.getValue().getSort())
                        .thenComparing(entry -> entry.getKey().toString()))
                .forEach(entry -> attachmentsByType.computeIfAbsent(entry.getValue().getType(), type -> new ArrayList<>())
                        .add(AttachmentItemBuilder.create().setId(entry.getKey()).build()));

        List<GunAttachmentQueryEntry> entries = new ArrayList<>();
        TimelessAPI.getAllCommonGunIndex().stream()
                .sorted(Comparator.<Map.Entry<ResourceLocation, CommonGunIndex>>comparingInt(entry -> entry.getValue().getSort())
                        .thenComparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    ItemStack gun = GunItemBuilder.create().setId(entry.getKey()).build();
                    if (!(gun.getItem() instanceof IGun iGun)) {
                        return;
                    }
                    for (AttachmentType type : AttachmentType.values()) {
                        List<ItemStack> candidates = attachmentsByType.get(type);
                        if (candidates == null || !iGun.allowAttachmentType(gun, type)) {
                            continue;
                        }
                        List<ItemStack> compatible = candidates.stream()
                                .filter(attachment -> iGun.allowAttachment(gun, attachment))
                                .toList();
                        int total = compatible.size();
                        int pageCount = (total + ATTACHMENTS_PER_PAGE - 1) / ATTACHMENTS_PER_PAGE;
                        for (int page = 0; page < pageCount; page++) {
                            int start = page * ATTACHMENTS_PER_PAGE;
                            int end = Math.min(start + ATTACHMENTS_PER_PAGE, total);
                            entries.add(new GunAttachmentQueryEntry(gun, type, compatible.subList(start, end),
                                    total, page + 1, pageCount));
                        }
                    }
                });
        return entries;
    }

    public ItemStack getGunStack() {
        return gunStack;
    }

    public AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public List<ItemStack> getAttachmentStacks() {
        return attachmentStacks;
    }

    public int getTotalAttachmentCount() {
        return totalAttachmentCount;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageCount() {
        return pageCount;
    }
}
