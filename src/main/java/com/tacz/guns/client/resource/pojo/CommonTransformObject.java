package com.tacz.guns.client.resource.pojo;

import com.google.gson.*;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;

public class CommonTransformObject {
    @Nullable
    private Vector3f translation;
    @Nullable
    private Vector3f rotation;
    @Nullable
    private Vector3f scale;

    public CommonTransformObject() {
    }

    public CommonTransformObject(@Nullable Vector3f translation, @Nullable Vector3f rotation, @Nullable Vector3f scale) {
        this.translation = translation;
        this.rotation = rotation;
        this.scale = scale;
    }

    @Nonnull
    public Vector3f getTranslation() {
        if (translation == null) {
            translation = new Vector3f(0, 0, 0);
        }
        return translation;
    }

    @Nonnull
    public Vector3f getRotation() {
        if (rotation == null) {
            rotation = new Vector3f(0, 0, 0);
        }
        return rotation;
    }

    @Nonnull
    public Vector3f getScale() {
        if (scale == null) {
            scale = new Vector3f(1, 1, 1);
        }
        return scale;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CommonTransformObject object) {
            boolean flag1, flag2, flag3;
            if (translation == null || object.translation == null) {
                flag1 = translation == null && object.translation == null;
            } else {
                flag1 = translation.equals(object.translation);
            }
            if (rotation == null || object.rotation == null) {
                flag2 = rotation == null && object.rotation == null;
            } else {
                flag2 = rotation.equals(object.rotation);
            }
            if (scale == null || object.scale == null) {
                flag3 = scale == null && object.scale == null;
            } else {
                flag3 = scale.equals(object.scale);
            }
            return flag1 && flag2 && flag3;
        }
        return false;
    }

    public CommonTransformObject lerp(CommonTransformObject target, float alpha) {
        CommonTransformObject object = new CommonTransformObject();
        object.translation = this.translation == null ? new Vector3f(0, 0, 0) : new Vector3f(this.translation);
        object.rotation = this.rotation == null ? new Vector3f(0, 0, 0) :new Vector3f( this.rotation);
        object.scale = this.scale == null ? new Vector3f(1, 1, 1) : new Vector3f(this.scale);
        object.translation.lerp(target.getTranslation(), alpha);
        object.rotation.lerp(target.getRotation(), alpha);
        object.scale.lerp(target.getScale(), alpha);
        return object;
    }

    public static class Serializer implements JsonDeserializer<CommonTransformObject>, JsonSerializer<CommonTransformObject> {
        @Override
        public CommonTransformObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                CommonTransformObject transformObject = new CommonTransformObject();
                JsonObject jsonObject = json.getAsJsonObject();
                JsonElement translationElement = jsonObject.get("translation");
                JsonElement rotationElement = jsonObject.get("rotation");
                JsonElement scaleElement = jsonObject.get("scale");
                if (translationElement != null) {
                    transformObject.translation = context.deserialize(translationElement, Vector3f.class);
                }
                if (rotationElement != null) {
                    transformObject.rotation = context.deserialize(rotationElement, Vector3f.class);
                }
                if (scaleElement != null) {
                    transformObject.scale = context.deserialize(scaleElement, Vector3f.class);
                }
                return transformObject;
            } else {
                throw new JsonSyntaxException("Expected " + json + " to be a CommonTransformObject because it's not an JsonObject");
            }
        }

        @Override
        public JsonElement serialize(CommonTransformObject src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            if (src.translation != null) {
                jsonObject.add("translation", context.serialize(src.translation));
            }
            if (src.rotation != null) {
                jsonObject.add("rotation", context.serialize(src.rotation));
            }
            if (src.scale != null) {
                jsonObject.add("scale", context.serialize(src.scale));
            }
            return jsonObject;
        }
    }
}
