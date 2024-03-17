package com.tac.guns.resource.index;

import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.resource.CommonAssetManager;
import com.tac.guns.resource.pojo.AttachmentIndexPOJO;
import com.tac.guns.resource.pojo.data.attachment.AttachmentData;
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
        if (attachmentIndexPOJO == null) {
            throw new IllegalArgumentException("index object file is empty");
        }
        if (attachmentIndexPOJO.getType() == null) {
            throw new IllegalArgumentException("attachment type must be nonnull.");
        }
        index.type = attachmentIndexPOJO.getType();
    }

    private static void checkData(AttachmentIndexPOJO attachmentIndexPOJO, CommonAttachmentIndex index) {
        ResourceLocation pojoData = attachmentIndexPOJO.getData();
        if (pojoData == null) {
            throw new IllegalArgumentException("index object missing pojoData field");
        }
        AttachmentData data = CommonAssetManager.INSTANCE.getAttachmentData(pojoData);
        if (data == null) {
            throw new IllegalArgumentException("there is no corresponding data file");
        }
        index.data = data;
    }

    public AttachmentData getData() {
        return data;
    }

    public AttachmentType getType() {
        return type;
    }
}
