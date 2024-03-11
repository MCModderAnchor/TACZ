package com.tac.guns.crafting;

import com.google.gson.*;
import com.tac.guns.item.builder.GunItemBuilder;
import com.tac.guns.resource.CommonGunPackLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

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

    public ItemStack getResult() {
        return result;
    }

    public String getGroup() {
        return group;
    }

    public static void toNetwork(FriendlyByteBuf buffer, GunSmithTableResult result) {
        buffer.writeItem(result.result);
        buffer.writeUtf(result.group);
    }

    public static GunSmithTableResult fromNetwork(FriendlyByteBuf buffer) {
        return new GunSmithTableResult(buffer.readItem(), buffer.readUtf());
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
                    final int countIn = count;
                    return CommonGunPackLoader.getGunIndex(id).map(gunIndex -> {
                        ItemStack itemStack = GunItemBuilder.create().setCount(countIn).setId(id).setAmmoCount(0)
                                .setFireMode(gunIndex.getGunData().getFireModeSet().get(0)).build();
                        String group = gunIndex.getType();
                        return new GunSmithTableResult(itemStack, group);
                    }).orElse(new GunSmithTableResult(ItemStack.EMPTY, ""));
                }
            }
            return new GunSmithTableResult(ItemStack.EMPTY, "");
        }
    }
}
