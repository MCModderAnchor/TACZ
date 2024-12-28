package com.tacz.guns.resource.index;

import com.google.common.base.Preconditions;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CommonAttachmentIndex {
    private AttachmentData data;
    private AttachmentType type;
    private AttachmentIndexPOJO pojo;
    private int sort;

    private CommonAttachmentIndex() {
    }

    public static CommonAttachmentIndex getInstance(AttachmentIndexPOJO attachmentIndexPOJO) throws IllegalArgumentException {
        CommonAttachmentIndex index = new CommonAttachmentIndex();
        index.pojo = attachmentIndexPOJO;
        checkIndex(attachmentIndexPOJO, index);
        checkData(attachmentIndexPOJO, index);
        return index;
    }

    private static void checkIndex(AttachmentIndexPOJO attachmentIndexPOJO, CommonAttachmentIndex index) {
        Preconditions.checkArgument(attachmentIndexPOJO != null, "index object file is empty");
        Preconditions.checkArgument(attachmentIndexPOJO.getType() != null, "attachment type must be nonnull.");
        index.type = attachmentIndexPOJO.getType();
        index.sort = Mth.clamp(attachmentIndexPOJO.getSort(), 0, 65536);
    }

    private static void checkData(AttachmentIndexPOJO attachmentIndexPOJO, CommonAttachmentIndex index) {
        ResourceLocation pojoData = attachmentIndexPOJO.getData();
        Preconditions.checkArgument(pojoData != null, "index object missing pojoData field");
        AttachmentData data = CommonAssetsManager.get().getAttachmentData(pojoData);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        index.data = data;
    }

    public AttachmentData getData() {
        return data;
    }

    public AttachmentType getType() {
        return type;
    }

    public AttachmentIndexPOJO getPojo() {
        return pojo;
    }

    public int getSort() {
        return sort;
    }
}
