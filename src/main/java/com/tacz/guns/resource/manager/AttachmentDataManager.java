package com.tacz.guns.resource.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;

public class AttachmentDataManager extends CommonDataManager<AttachmentData> {

    public AttachmentDataManager() {
        super(DataType.ATTACHMENT_DATA, AttachmentData.class, CommonAssetsManager.GSON, "data/attachments", "AttachmentDataLoader");
    }

    @Override
    protected AttachmentData parseJson(JsonElement element) {
        AttachmentData data = getGson().fromJson(element, getDataClass());
        if (data != null) {
            // 序列化注册的配件属性修改
            AttachmentPropertyManager.getModifiers().forEach((key, value) -> {
                String json = getGson().toJson(element);
                if (!element.isJsonObject()) {
                    return;
                }
                JsonObject jsonObject = element.getAsJsonObject();
                if (jsonObject.has(key)) {
                    JsonProperty<?> property = value.readJson(json);
                    property.initComponents();
                    data.addModifier(key, property);
                } else if (jsonObject.has(value.getOptionalFields())) {
                    // 为了兼容旧版本，读取可选字段名
                    JsonProperty<?> property = value.readJson(json);
                    property.initComponents();
                    data.addModifier(key, property);
                }
            });
        }
        return data;
    }
}
