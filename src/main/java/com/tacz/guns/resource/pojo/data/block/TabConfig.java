package com.tacz.guns.resource.pojo.data.block;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;

public record TabConfig(ResourceLocation id, String name, ItemStack icon) {
    public static final ResourceLocation TAB_AMMO = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "ammo");

    public static final ResourceLocation TAB_PISTOL = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "pistol");
    public static final ResourceLocation TAB_SNIPER = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "sniper");
    public static final ResourceLocation TAB_DMR = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "dmr");
    public static final ResourceLocation TAB_RIFLE = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "rifle");
    public static final ResourceLocation TAB_SHOTGUN = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "shotgun");
    public static final ResourceLocation TAB_SMG = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "smg");
    public static final ResourceLocation TAB_RPG = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "rpg");
    public static final ResourceLocation TAB_MG = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "mg");

    public static final ResourceLocation TAB_SCOPE = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "scope");
    public static final ResourceLocation TAB_MUZZLE = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "muzzle");
    public static final ResourceLocation TAB_STOCK = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "stock");
    public static final ResourceLocation TAB_GRIP = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "grip");
    public static final ResourceLocation TAB_EXTENDED_MAG = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "extended_mag");
    public static final ResourceLocation TAB_LASER = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "laser");

    public static final ResourceLocation TAB_MISC = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "misc");
    public static final ResourceLocation TAB_EMPTY = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "empty");

    public static final List<TabConfig> DEFAULT_TABS = List.of(
            new TabConfig(TabConfig.TAB_AMMO, "tacz.type.ammo.name", AmmoItemBuilder.create().setId(DefaultAssets.DEFAULT_AMMO_ID).build()),
            new TabConfig(TabConfig.TAB_PISTOL, "tacz.type.pistol.name", GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "glock_17")).forceBuild(null)),
            new TabConfig(TabConfig.TAB_SNIPER, "tacz.type.sniper.name", GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "ai_awp")).forceBuild(null)),
            new TabConfig(TabConfig.TAB_DMR, "tacz.type.dmr.name", GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "mk14")).forceBuild(null)),
            new TabConfig(TabConfig.TAB_RIFLE, "tacz.type.rifle.name", GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "ak47")).forceBuild(null)),
            new TabConfig(TabConfig.TAB_SHOTGUN, "tacz.type.shotgun.name", GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "db_short")).forceBuild(null)),
            new TabConfig(TabConfig.TAB_SMG, "tacz.type.smg.name", GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "hk_mp5a5")).forceBuild(null)),
            new TabConfig(TabConfig.TAB_RPG, "tacz.type.rpg.name", GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "rpg7")).forceBuild(null)),
            new TabConfig(TabConfig.TAB_MG, "tacz.type.mg.name", GunItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "m249")).forceBuild(null)),
            new TabConfig(TabConfig.TAB_SCOPE, "tacz.type.scope.name",  AttachmentItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "scope_acog_ta31")).build()),
            new TabConfig(TabConfig.TAB_MUZZLE, "tacz.type.muzzle.name", AttachmentItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "muzzle_compensator_trident")).build()),
            new TabConfig(TabConfig.TAB_STOCK, "tacz.type.stock.name", AttachmentItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "stock_militech_b5")).build()),
            new TabConfig(TabConfig.TAB_GRIP, "tacz.type.grip.name", AttachmentItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "grip_magpul_afg_2")).build()),
            new TabConfig(TabConfig.TAB_EXTENDED_MAG, "tacz.type.extended_mag.name", AttachmentItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "extended_mag_3")).build()),
            new TabConfig(TabConfig.TAB_LASER, "tacz.type.laser.name", AttachmentItemBuilder.create().setId(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "laser_compact")).build()),
            new TabConfig(TabConfig.TAB_MISC, "tacz.type.misc.name", ModItems.GUN_SMITH_TABLE.get().getDefaultInstance())
    );

    public static class Deserializer implements JsonDeserializer<TabConfig> {
        @Override
        public TabConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) {
                throw new JsonParseException("TabConfig must be a JSON object");
            }
            JsonObject object = json.getAsJsonObject();
            if (!object.has("id") || !object.get("id").isJsonPrimitive()) {
                throw new JsonParseException("TabConfig must have an id");
            }
            ResourceLocation id = context.deserialize(object.get("id"), ResourceLocation.class);
            ItemStack icon = ItemStack.CODEC.parse(JsonOps.INSTANCE, GsonHelper.getAsJsonObject(object, "icon")).getOrThrow();
            String name = GsonHelper.getAsString(object, "name", "tacz.type.unknown.name");
            return new TabConfig(id, name, icon);
        }
    }

    @NotNull
    public Component getName() {
        return Component.translatable(name==null ? "tacz.type.unknown.name" : name);
    }
}
