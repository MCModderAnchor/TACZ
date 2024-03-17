package com.tac.guns.crafting;

import com.google.gson.*;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.item.builder.AmmoItemBuilder;
import com.tac.guns.item.builder.AttachmentItemBuilder;
import com.tac.guns.item.builder.GunItemBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;

public class GunSmithTableResult {
    public static final String GUN = "gun";
    public static final String AMMO = "ammo";
    public static final String ATTACHMENT = "attachment";

    private final ItemStack result;
    private final String group;

    public GunSmithTableResult(ItemStack result, String group) {
        this.result = result;
        this.group = group;
    }

    public static void toNetwork(FriendlyByteBuf buffer, GunSmithTableResult result) {
        buffer.writeItem(result.result);
        buffer.writeUtf(result.group);
    }

    public static GunSmithTableResult fromNetwork(FriendlyByteBuf buffer) {
        return new GunSmithTableResult(buffer.readItem(), buffer.readUtf());
    }

    public ItemStack getResult() {
        return result;
    }

    public String getGroup() {
        return group;
    }

    public static class Serializer implements JsonDeserializer<GunSmithTableResult> {
        @Override
        public GunSmithTableResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();
                ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(jsonObject, "id"));
                String typeName = GsonHelper.getAsString(jsonObject, "type");
                int count = 1;
                if (jsonObject.has("count")) {
                    count = Math.max(GsonHelper.getAsInt(jsonObject, "count"), 1);
                }
                if (GUN.equals(typeName)) {
                    return getGunStack(id, count);
                }
                if (AMMO.equals(typeName)) {
                    return getAmmoStack(id, count);
                }
                if (ATTACHMENT.equals(typeName)) {
                    return getAttachmentStack(id, count);
                }
            }
            return new GunSmithTableResult(ItemStack.EMPTY, StringUtils.EMPTY);
        }

        private GunSmithTableResult getGunStack(ResourceLocation id, int count) {
            return TimelessAPI.getCommonGunIndex(id).map(gunIndex -> {
                ItemStack itemStack = GunItemBuilder.create().setCount(count).setId(id).setAmmoCount(0)
                        .setFireMode(gunIndex.getGunData().getFireModeSet().get(0)).build();
                String group = gunIndex.getType();
                return new GunSmithTableResult(itemStack, group);
            }).orElse(new GunSmithTableResult(ItemStack.EMPTY, StringUtils.EMPTY));
        }

        private GunSmithTableResult getAmmoStack(ResourceLocation id, int count) {
            return new GunSmithTableResult(AmmoItemBuilder.create().setCount(count).setId(id).build(), GunSmithTableResult.AMMO);
        }

        private GunSmithTableResult getAttachmentStack(ResourceLocation id, int count) {
            return new GunSmithTableResult(AttachmentItemBuilder.create().setCount(count).setId(id).build(), GunSmithTableResult.ATTACHMENT);
        }
    }
}
