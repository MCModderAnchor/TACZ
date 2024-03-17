package com.tac.guns.crafting;

import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

import java.lang.reflect.Type;

public class GunSmithTableIngredient {
    private final Ingredient ingredient;
    private final int count;

    public GunSmithTableIngredient(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;
    }

    public static void toNetwork(FriendlyByteBuf buffer, GunSmithTableIngredient ingredient) {
        ingredient.ingredient.toNetwork(buffer);
        buffer.writeVarInt(ingredient.count);
    }

    public static GunSmithTableIngredient fromNetwork(FriendlyByteBuf buffer) {
        return new GunSmithTableIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt());
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getCount() {
        return count;
    }

    public static class Serializer implements JsonDeserializer<GunSmithTableIngredient> {
        @Override
        public GunSmithTableIngredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();
                if (!jsonObject.has("item")) {
                    throw new JsonSyntaxException("Expected " + jsonObject + " must has a item member");
                }
                Ingredient ingredient = Ingredient.fromJson(jsonObject.get("item"));
                int count = 1;
                if (jsonObject.has("count")) {
                    count = Math.max(GsonHelper.getAsInt(jsonObject, "count"), 1);
                }
                return new GunSmithTableIngredient(ingredient, count);
            } else {
                throw new JsonSyntaxException("Expected " + json + " to be a Pair because it's not an object");
            }
        }
    }
}
