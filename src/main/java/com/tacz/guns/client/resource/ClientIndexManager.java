package com.tacz.guns.client.resource;

import com.google.common.collect.Maps;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.*;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.AmmoIndexPOJO;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import com.tacz.guns.resource.pojo.BlockIndexPOJO;
import com.tacz.guns.resource.pojo.GunIndexPOJO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@OnlyIn(Dist.CLIENT)
public class ClientIndexManager {
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final AtomicLong RESOURCE_REVISION = new AtomicLong();
    private static final Set<ResourceLocation> WARNED_MISSING_GUN_DISPLAYS = new HashSet<>();

    public static final Map<ResourceLocation, GunDisplayInstance> GUN_DISPLAY = Maps.newHashMap();
    public static final Map<ResourceLocation, ClientGunIndex> GUN_INDEX = Maps.newHashMap();
    public static final Map<ResourceLocation, ClientAmmoIndex> AMMO_INDEX = Maps.newHashMap();
    public static final Map<ResourceLocation, ClientAttachmentIndex> ATTACHMENT_INDEX = Maps.newHashMap();
    public static final Map<ResourceLocation, ClientAttachmentVariantIndex> ATTACHMENT_VARIANT_INDEX = Maps.newHashMap();
    public static final Map<ResourceLocation, ClientSlotAdapterIndex> SLOT_ADAPTER_INDEX = Maps.newHashMap();
    public static final Map<ResourceLocation, ClientBlockIndex> BLOCK_INDEX = Maps.newHashMap();

    public static void clear() {
        ClientAssetLoadDispatcher.invalidate();
        RESOURCE_REVISION.incrementAndGet();
        GUN_DISPLAY.clear();
        WARNED_MISSING_GUN_DISPLAYS.clear();
        GUN_INDEX.clear();
        AMMO_INDEX.clear();
        ATTACHMENT_INDEX.clear();
        ATTACHMENT_VARIANT_INDEX.clear();
        SLOT_ADAPTER_INDEX.clear();
        BLOCK_INDEX.clear();
    }

    public static long resourceRevision() {
        return RESOURCE_REVISION.get();
    }

    public static void reload() {
        clear();
        try {
            loadGunDisplay();
            loadGunIndex();
            loadAmmoIndex();
            loadAttachmentIndex();
            loadAttachmentVariantIndex();
            loadSlotAdapterIndex();
            loadBlockIndex();
            preloadExternalModels();
            warmUpInventoryModels();
        } finally {
            // 使重载过程中暂时解析失败的展示槽位在资源就绪后再次解析。
            RESOURCE_REVISION.incrementAndGet();
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && IGun.mainHandHoldGun(player)) {
            AttachmentPropertyManager.postChangeEvent(player, player.getMainHandItem());

            // 自动切一次枪，以便刷新状态机
            IClientPlayerGunOperator.fromLocalPlayer(player).draw(ItemStack.EMPTY);
            // FIXME: FirstPersonRenderHandler.reset();
        }
    }

    public static void loadGunIndex() {
        TimelessAPI.getAllCommonGunIndex().forEach(index -> {
            ResourceLocation id = index.getKey();
            GunIndexPOJO pojo = index.getValue().getPojo();
            try {
                GUN_INDEX.put(id, ClientGunIndex.getInstance(pojo));
            } catch (IllegalArgumentException exception) {
                GunMod.LOGGER.warn("{} index file read fail!", id, exception);
            }
        });
    }

    public static void loadGunDisplay() {
        ClientAssetsManager.INSTANCE.getGunDisplays().forEach(entry -> {
            ResourceLocation displayId = entry.getKey();
            try {
                GUN_DISPLAY.put(displayId, GunDisplayInstance.create(displayId, entry.getValue()));
            } catch (IllegalArgumentException exception) {
                GunMod.LOGGER.warn("{} display file read fail!", displayId, exception);
            }
        });
    }

    public static GunDisplayInstance getOrCreateGunDisplay(ResourceLocation displayId) {
        GunDisplayInstance instance = GUN_DISPLAY.get(displayId);
        if (instance != null) {
            return instance;
        }
        if (WARNED_MISSING_GUN_DISPLAYS.add(displayId)) {
            GunMod.LOGGER.warn("Gun display {} is unavailable (assets/{}/display/guns/{}.json);",
                    displayId, displayId.getNamespace(), displayId.getPath());
        }
        return null;
    }

    public static void loadAmmoIndex() {
        TimelessAPI.getAllCommonAmmoIndex().forEach(index -> {
            ResourceLocation id = index.getKey();
            AmmoIndexPOJO pojo = index.getValue().getPojo();
            try {
                AMMO_INDEX.put(id, ClientAmmoIndex.getInstance(pojo));
            } catch (IllegalArgumentException exception) {
                GunMod.LOGGER.warn("{} index file read fail!", id, exception);
            }
        });
    }

    public static void loadAttachmentIndex() {
        TimelessAPI.getAllCommonAttachmentIndex().forEach(index -> {
            ResourceLocation id = index.getKey();
            AttachmentIndexPOJO pojo = index.getValue().getPojo();
            try {
                ATTACHMENT_INDEX.put(id, ClientAttachmentIndex.getInstance(id, pojo));
            } catch (IllegalArgumentException exception) {
                GunMod.LOGGER.warn("{} index file read fail!", id, exception);
            }
        });
    }

    public static void loadAttachmentVariantIndex() {
        TimelessAPI.getAllCommonAttachmentVariantIndex().forEach(index -> {
            ResourceLocation id = index.getKey();
            try {
                ATTACHMENT_VARIANT_INDEX.put(id, ClientAttachmentVariantIndex.getInstance(index.getValue()));
            } catch (IllegalArgumentException exception) {
                GunMod.LOGGER.warn("{} attachment variant index file read fail!", id, exception);
            }
        });
    }

    public static void loadSlotAdapterIndex() {
        TimelessAPI.getAllCommonSlotAdapterIndex().forEach(index -> {
            ResourceLocation id = index.getKey();
            try {
                SLOT_ADAPTER_INDEX.put(id, ClientSlotAdapterIndex.getInstance(index.getValue()));
            } catch (IllegalArgumentException exception) {
                GunMod.LOGGER.warn("{} slot adapter index file read fail!", id, exception);
            }
        });
    }

    public static void loadBlockIndex() {
        TimelessAPI.getAllCommonBlockIndex().forEach(index -> {
            ResourceLocation id = index.getKey();
            BlockIndexPOJO pojo = index.getValue().getPojo();
            try {
                BLOCK_INDEX.put(id, ClientBlockIndex.getInstance(pojo));
            } catch (IllegalArgumentException exception) {
                GunMod.LOGGER.warn("{} index file read fail!", id, exception);
            }
        });
    }

    public static Set<Map.Entry<ResourceLocation, ClientGunIndex>> getAllGuns() {
        return GUN_INDEX.entrySet();
    }

    public static Set<Map.Entry<ResourceLocation, ClientAmmoIndex>> getAllAmmo() {
        return AMMO_INDEX.entrySet();
    }

    public static Set<Map.Entry<ResourceLocation, ClientAttachmentIndex>> getAllAttachments() {
        return ATTACHMENT_INDEX.entrySet();
    }

    public static Set<Map.Entry<ResourceLocation, ClientAttachmentVariantIndex>> getAllAttachmentVariants() {
        return ATTACHMENT_VARIANT_INDEX.entrySet();
    }

    public static Set<Map.Entry<ResourceLocation, ClientSlotAdapterIndex>> getAllSlotAdapters() {
        return SLOT_ADAPTER_INDEX.entrySet();
    }

    public static Set<Map.Entry<ResourceLocation, ClientBlockIndex>> getAllBlocks() {
        return BLOCK_INDEX.entrySet();
    }

    /** 重载时预加载默认命名空间的模型，其他枪包继续按需加载并由背包预热。 */
    private static void preloadExternalModels() {
        List<CompletableFuture<?>> tasks = new ArrayList<>();
        for (GunDisplayInstance display : GUN_DISPLAY.values()) {
            tasks.add(display.getExternalModels().preload());
        }
        for (ClientAttachmentIndex index : ATTACHMENT_INDEX.values()) {
            tasks.add(index.getExternalModels().preload());
        }
        for (ClientAttachmentVariantIndex index : ATTACHMENT_VARIANT_INDEX.values()) {
            tasks.add(index.getExternalModels().preload());
        }
        for (ClientSlotAdapterIndex index : SLOT_ADAPTER_INDEX.values()) {
            tasks.add(index.getExternalModels().preload());
        }

        // 只在资源重载阶段等待；外部模型的渲染入口不会等待后台任务。
        CompletableFuture.allOf(tasks.toArray(CompletableFuture<?>[]::new)).join();
    }

    public static void warmUpInventoryModels() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        warmUpItemForUse(player.getMainHandItem());
        warmUpItemForUse(player.getOffhandItem());
        warmUpHotbarModels(player);
        warmUpBackpackModels(player);
    }

    public static void warmUpEquippedAndHotbarModels() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        warmUpItemForUse(player.getMainHandItem());
        warmUpItemForUse(player.getOffhandItem());
        warmUpHotbarModels(player);
    }

    public static void warmUpBackpackModels() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        warmUpBackpackModels(player);
    }

    private static void warmUpHotbarModels(LocalPlayer player) {
        var items = player.getInventory().items;
        int hotbarSize = Math.min(HOTBAR_SLOT_COUNT, items.size());
        for (int i = 0; i < hotbarSize; i++) {
            warmUpItemModel(items.get(i));
        }
    }

    private static void warmUpBackpackModels(LocalPlayer player) {
        var items = player.getInventory().items;
        for (int i = Math.min(HOTBAR_SLOT_COUNT, items.size()); i < items.size(); i++) {
            warmUpItemModel(items.get(i));
        }
    }

    public static void warmUpItem(ItemStack stack) {
        warmUpItemForUse(stack);
    }

    public static void warmUpItemModel(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof IGun) {
            TimelessAPI.getGunDisplay(stack).ifPresent(display -> {
                display.warmUpLod();
                display.warmUpModel();
                display.warmUpRuntime();
            });
            return;
        }
        IAttachment attachment = IAttachment.getIAttachmentOrNull(stack);
        if (attachment != null) {
            ResourceLocation attachmentId = attachment.getAttachmentId(stack);
            TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresent(ClientAttachmentIndex::warmUp);
            TimelessAPI.getClientAttachmentVariantIndex(stack).ifPresent(ClientAttachmentVariantIndex::warmUp);
            return;
        }
        IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
        if (ammo != null) {
            TimelessAPI.getClientAmmoIndex(ammo.getAmmoId(stack)).ifPresent(ClientAmmoIndex::warmUp);
        }
    }

    public static void warmUpItemForUse(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getItem() instanceof IGun) {
            TimelessAPI.getGunDisplay(stack).ifPresent(display -> {
                display.warmUpLod();
                display.warmUpModel();
                display.warmUpRuntime();
            });
            return;
        }
        warmUpItemModel(stack);
    }
}
