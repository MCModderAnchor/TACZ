package com.tacz.guns.compat.kubejs.util;

import com.google.gson.JsonObject;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.util.JsonIO;
import dev.latvian.mods.kubejs.util.UtilsJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.CraftingHelper;

public class GunSmithTableResultInfo {
    private static final String TYPE_KEY = "type";
    private static final String ID_KEY = "id";
    private static final String COUNT_KEY = "count";
    private static final String NBT_KEY = "nbt";
    private static final String CUSTOM_ITEM_KEY = "item";
    private static final String OUTPUT_GROUP_KEY = "group";
    private final JsonObject json;

    private GunSmithTableResultInfo() {
        this(new JsonObject());
    }

    private GunSmithTableResultInfo(JsonObject jsonObject) {
        this.json = (jsonObject != null) ? jsonObject : new JsonObject();
    }

    public static GunSmithTableResultInfo create() {
        return new GunSmithTableResultInfo();
    }

    public static GunSmithTableResultInfo createFromJson(JsonObject jsonObject) {
        return new GunSmithTableResultInfo(jsonObject);
    }

    public static GunSmithTableResultInfo createFromItemStack(ItemStack stack) {
        GunSmithTableResultInfo info = create().setType(GunSmithTableResult.CUSTOM);
        JsonObject itemJson = new JsonObject();
        itemJson.addProperty("item", RegistryInfo.ITEM.getId(stack.getItem()).toString());
        itemJson.addProperty("count", stack.getCount());
        if (JsonIO.of(stack.getTag()) != null) {
            itemJson.addProperty("nbt", stack.getOrCreateTag().toString());
        }
        info.setCustomItem(itemJson);
        return info;
    }

    /**
     * {@link GunSmithTableResultInfo}的TypeWrapper, 将其他类型转为{@link GunSmithTableResultInfo}
     * object为其他类型时优先解析{@link JsonObject}，其次{@link OutputItem}，再其次{@link ItemStack}
     * 之后尝试转化为{@link String}解析为{@link ResourceLocation}
     * 以上均不成功时最终{@link JsonIO#of(Object)}解析
     * @param object 输入待转化对象
     * @return {@link GunSmithTableResultInfo}
     */
    public static GunSmithTableResultInfo of(Object object) {
        if (object instanceof GunSmithTableResultInfo info) {
            return info;
        } else if (object instanceof JsonObject jsonObject) {
            return createFromJson(jsonObject);
        }else if (object instanceof OutputItem outputItem) {
            return createFromItemStack(outputItem.item);
        } else if (object instanceof ItemStack stack) {
            return createFromItemStack(stack);
        }
        String idString = object.toString();
        if (ResourceLocation.isValidResourceLocation(idString)) {
            ResourceLocation rl = UtilsJS.getMCID(null, idString);
            TimelessItemWrapper.ItemIndexInfo indexInfo = TimelessItemWrapper.ItemIndexInfo.createFromResourceLocation(rl);
            if (indexInfo.isValidForRecipe()) {
                return create().setType(indexInfo.getParent()).setId(indexInfo.getIndexId());
            }
        }
        ItemStack stack = ItemStackJS.of(object);
        if (!stack.isEmpty()) {
            return createFromItemStack(stack);
        }
        //以上都不匹配，默认按JsonObject处理
        return createFromJson(JsonIO.of(object).getAsJsonObject());
    }

    public String getType() {
        return GsonHelper.getAsString(this.json, TYPE_KEY);
    }

    public GunSmithTableResultInfo setType(String typeName) {
        this.json.addProperty(TYPE_KEY, typeName);
        return this;
    }

    public ResourceLocation getId() {
        return ResourceLocation.tryParse(GsonHelper.getAsString(this.json, ID_KEY));
    }

    public GunSmithTableResultInfo setId(ResourceLocation id) {
        this.json.addProperty(ID_KEY, id.toString());
        return this;
    }

    public CompoundTag getNbt() {
        return CraftingHelper.getNBT(this.json.get(NBT_KEY));
    }

    public GunSmithTableResultInfo setNbt(CompoundTag nbt) {
        this.json.addProperty(NBT_KEY, nbt.toString());
        return this;
    }

    public int getCount() {
        return Math.max(GsonHelper.getAsInt(this.json, "count"), 1);
    }

    public GunSmithTableResultInfo setCount(int count) {
        this.json.addProperty(COUNT_KEY, count);
        return this;
    }

    public JsonObject getCustomItem() {
        return GsonHelper.getAsJsonObject(this.json, CUSTOM_ITEM_KEY);
    }

    public GunSmithTableResultInfo setCustomItem(JsonObject itemJson) {
        this.json.add(CUSTOM_ITEM_KEY, itemJson);
        return this;
    }

    public GunSmithTableResultInfo setGroupName(String groupName) {
        this.json.addProperty(OUTPUT_GROUP_KEY, groupName);
        return this;
    }

    public GunSmithTableResultInfo setGroup(OutputGroupName group) {
        this.setGroupName(group.getName());
        return this;
    }

    public JsonObject toJson() {
        return json;
    }

    public enum OutputGroupName {
        AMMO("ammo"),
        EXTENDED_MAG("extended_mag"),
        GRIP("grip"),
        MG("mg"),
        MUZZLE("muzzle"),
        PISTOL("pistol"),
        RIFLE("rifle"),
        DMR("dmr"),
        RPG("rpg"),
        SCOPE("scope"),
        SHOTGUN("shotgun"),
        SMG("smg"),
        SNIPER("sniper"),
        STOCK("stock");

        private final String name;

        OutputGroupName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
