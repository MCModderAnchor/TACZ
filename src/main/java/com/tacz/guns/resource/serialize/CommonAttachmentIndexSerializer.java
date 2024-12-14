package com.tacz.guns.resource.serialize;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;

import java.lang.reflect.Type;

public class CommonAttachmentIndexSerializer implements JsonDeserializer<CommonAttachmentIndex> {
    @Override
    public CommonAttachmentIndex deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            AttachmentIndexPOJO pojo = context.deserialize(json, AttachmentIndexPOJO.class);
            return CommonAttachmentIndex.getInstance(pojo);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(e.getMessage());
        }
    }
}
