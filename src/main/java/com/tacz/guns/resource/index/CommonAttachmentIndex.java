package com.tacz.guns.resource.index;

import com.google.common.base.Preconditions;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.CommonAssetManager;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import net.minecraft.resources.ResourceLocation;

public class CommonAttachmentIndex {
    private AttachmentData data;
    private AttachmentType type;

    private CommonAttachmentIndex() {
    }

    public static CommonAttachmentIndex getInstance(AttachmentIndexPOJO attachmentIndexPOJO) throws IllegalArgumentException {
        CommonAttachmentIndex index = new CommonAttachmentIndex();
        checkIndex(attachmentIndexPOJO, index);
        checkData(attachmentIndexPOJO, index);
        return index;
    }

    private static void checkIndex(AttachmentIndexPOJO attachmentIndexPOJO, CommonAttachmentIndex index) {
        Preconditions.checkArgument(attachmentIndexPOJO != null, "index object file is empty");
        Preconditions.checkArgument(attachmentIndexPOJO.getType() != null, "attachment type must be nonnull.");
        index.type = attachmentIndexPOJO.getType();
    }

    private static void checkData(AttachmentIndexPOJO attachmentIndexPOJO, CommonAttachmentIndex index) {
        ResourceLocation pojoData = attachmentIndexPOJO.getData();
        Preconditions.checkArgument(pojoData != null, "index object missing pojoData field");
        AttachmentData data = CommonAssetManager.INSTANCE.getAttachmentData(pojoData);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        index.data = data;
    }

    public AttachmentData getData() {
        return data;
    }

    public AttachmentType getType() {
        return type;
    }
}
