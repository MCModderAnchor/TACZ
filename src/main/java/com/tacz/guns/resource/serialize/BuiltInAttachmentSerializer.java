package com.tacz.guns.resource.serialize;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.tacz.guns.resource.pojo.data.gun.BuiltInAttachment;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;
import java.util.List;

public class BuiltInAttachmentSerializer implements JsonDeserializer<BuiltInAttachment> {
    @Override
    public BuiltInAttachment deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<ResourceLocation> attachments = Lists.newArrayList();
        ResourceLocation defaultAttachment = null;
        ResourceLocation displayAttachment = null;
        boolean showIcon = true;
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            displayAttachment = context.deserialize(json, ResourceLocation.class);
            attachments.add(displayAttachment);
            showIcon = false;
        } else if (json.isJsonArray()) {
            attachments.addAll(parseAttachmentList(json.getAsJsonArray(), context));
        } else if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            JsonElement attachmentsElement = object.get("attachments");
            if (attachmentsElement != null) {
                if (attachmentsElement.isJsonArray()) {
                    attachments.addAll(parseAttachmentList(attachmentsElement.getAsJsonArray(), context));
                } else if (attachmentsElement.isJsonPrimitive() && attachmentsElement.getAsJsonPrimitive().isString()) {
                    attachments.add(context.deserialize(attachmentsElement, ResourceLocation.class));
                } else {
                    throw new JsonParseException("builtin attachments must be a string or string array");
                }
            }
            JsonElement defaultElement = object.has("default_attached") ? object.get("default_attached") : object.get("default");
            if (defaultElement != null) {
                defaultAttachment = context.deserialize(defaultElement, ResourceLocation.class);
                if (!attachments.contains(defaultAttachment)) {
                    attachments.add(defaultAttachment);
                }
            }
            // 保留未安装配件时的原有显示，供可选内置配件共用。
            JsonElement displayElement = object.get("display_attachment");
            if (displayElement != null) {
                displayAttachment = context.deserialize(displayElement, ResourceLocation.class);
            }
            JsonElement showIconElement = object.get("show_icon");
            if (showIconElement != null) {
                showIcon = showIconElement.getAsBoolean();
            }
        } else {
            throw new JsonParseException("builtin attachment must be a string, array, or object");
        }
        return new BuiltInAttachment(attachments, defaultAttachment, displayAttachment, showIcon);
    }

    private static List<ResourceLocation> parseAttachmentList(JsonArray array, JsonDeserializationContext context) {
        List<ResourceLocation> result = Lists.newArrayList();
        array.forEach(element -> result.add(context.deserialize(element, ResourceLocation.class)));
        return result;
    }
}
