package com.tacz.guns.resource.index;

import com.google.common.base.Preconditions;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.SlotAdapterIndexPOJO;
import com.tacz.guns.resource.pojo.data.attachment.SlotAdapterData;
import com.tacz.guns.util.AllowAttachmentTagMatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

public class CommonSlotAdapterIndex {
    private SlotAdapterData data;
    private SlotAdapterIndexPOJO pojo;
    private int sort;
    private AttachmentType slot;
    private List<String> allowedAttachments;

    private CommonSlotAdapterIndex() {
    }

    public static CommonSlotAdapterIndex getInstance(SlotAdapterIndexPOJO pojo) throws IllegalArgumentException {
        CommonSlotAdapterIndex index = new CommonSlotAdapterIndex();
        index.pojo = pojo;
        checkIndex(pojo, index);
        checkData(pojo, index);
        return index;
    }

    private static void checkIndex(SlotAdapterIndexPOJO pojo, CommonSlotAdapterIndex index) {
        Preconditions.checkArgument(pojo != null, "index object file is empty");
        Preconditions.checkArgument(pojo.getDisplay() != null, "slot adapter display must be nonnull.");
        Preconditions.checkArgument(pojo.getSlot() != null && pojo.getSlot() != AttachmentType.NONE, "slot adapter slot must be a valid attachment type.");
        index.sort = Mth.clamp(pojo.getSort(), 0, 65536);
        index.slot = pojo.getSlot();
        index.allowedAttachments = List.copyOf(pojo.getAllowedAttachments());
    }

    private static void checkData(SlotAdapterIndexPOJO pojo, CommonSlotAdapterIndex index) {
        ResourceLocation dataId = pojo.getData();
        Preconditions.checkArgument(dataId != null, "index object missing pojoData field");
        SlotAdapterData data = CommonAssetsManager.get().getSlotAdapterData(dataId);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        index.data = data;
    }

    public SlotAdapterData getData() {
        return data;
    }

    public SlotAdapterIndexPOJO getPojo() {
        return pojo;
    }

    public int getSort() {
        return sort;
    }

    public AttachmentType getSlot() {
        return slot;
    }

    public boolean allowsAttachment(ResourceLocation attachmentId) {
        if (attachmentId == null) {
            return false;
        }
        for (String value : allowedAttachments) {
            if (value == null) {
                continue;
            }
            if (value.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(value.substring(1));
                if (tagId != null && AllowAttachmentTagMatcher.matchTag(tagId, attachmentId)) {
                    return true;
                }
                continue;
            }
            if (attachmentId.equals(ResourceLocation.tryParse(value))) {
                return true;
            }
        }
        return false;
    }
}
