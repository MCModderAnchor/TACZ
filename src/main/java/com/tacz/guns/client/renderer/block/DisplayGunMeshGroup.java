package com.tacz.guns.client.renderer.block;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.world.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.entity.DisplayGunSlot;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.SlotAdapterHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

import java.util.*;

/** 每个悬挂物独立登记；稳定身份由所属方块实体创建和持有。 */
public final class DisplayGunMeshGroup {
    private static final int GRID_SIZE = 4;
    private static final double ITEM_DEPTH = 0.25D;
    private static WorldMeshGroup<DisplayGunSlot> group;
    private static boolean configWasEnabled;
    private static final Map<DisplayGunSlot, CachedAppearance> APPEARANCES = new WeakHashMap<>();

    private DisplayGunMeshGroup() {}

    private record AttachmentKey(AttachmentType type, ResourceLocation id) {}

    /** 只包含实际改变捕获顶点和材质的状态；世界位置与采样光照另行提供。 */
    private record GeometryKey(long resourceRevision, ResourceLocation displayId,
                               List<AttachmentKey> attachments,
                               boolean bullet, boolean ammo, boolean useLod) {}

    /** 内容或资源变化时解析一次；近距和远距模型共享同一份物品副本。 */
    private record Snapshot(GeometryKey key, ItemStack stack, GunDisplayInstance display,
                            ExternalGunModel model, ResourceLocation texture, boolean useLod) {}

    private record CachedAppearance(long appearanceRevision, long resourceRevision,
                                    Snapshot near, Snapshot far) {}

    private static final MeshRenderableAdapter<DisplayGunSlot> ADAPTER = new MeshRenderableAdapter<>() {
        @Override public boolean isValid(DisplayGunSlot slot) {
            BlockEntity owner = slot.owner();
            ItemStack stack = stack(owner, slot.index());
            boolean valid = !owner.isRemoved() && owner.getLevel() == Minecraft.getInstance().level
                    && !stack.isEmpty() && IGun.getIGunOrNull(stack) != null;
            if (!valid) APPEARANCES.remove(slot);
            return valid;
        }

        @Override public boolean isVisible(DisplayGunSlot slot) {
            return snapshot(slot) != null;
        }

        @Override
        public Vec3 origin(DisplayGunSlot slot) {
            return Vec3.atLowerCornerOf(slot.owner().getBlockPos());
        }

        @Override
        public Matrix4f localTransform(DisplayGunSlot slot) {
            BlockEntity owner = slot.owner();
            int index = slot.index();
            double x = ((index % GRID_SIZE) + 0.5D) / GRID_SIZE - 0.5D;
            double y = ((index / GRID_SIZE) + 0.5D) / GRID_SIZE - 0.5D;
            PoseStack pose = new PoseStack();
            DisplayItemTransforms.apply(pose, facing(owner), x, y, ITEM_DEPTH, rotation(owner, index));
            return new Matrix4f(pose.last().pose());
        }

        @Override
        public int packedLight(DisplayGunSlot slot) {
            BlockEntity owner = slot.owner();
            if (owner instanceof GunDisplayCaseBlockEntity) return LightTexture.pack(15, 15);
            if (owner.getBlockState().getBlock() instanceof GunPegboardBlock board && board.isGlowing())
                return LightTexture.pack(15, 15);
            return LevelRenderer.getLightColor(owner.getLevel(), owner.getBlockPos());
        }

        @Override
        public Object geometryKey(DisplayGunSlot slot) {
            Snapshot current = snapshot(slot);
            return current == null ? slot : current.key();
        }

        @Override
        public boolean collectGeometry(DisplayGunSlot slot, GeometryCollector collector) {
            Snapshot current = snapshot(slot);
            return current != null && DisplayGunGeometry.capture(current.stack(), current.display(), current.model(),
                    current.texture(), current.useLod(), collector);
        }
    };

    /** BER 对当前槽位调用；返回 true 时本帧由 SBM 绘制该悬挂物。 */
    public static boolean tryRender(BlockEntity owner, int index, int overlay) {
        if (!RenderConfig.DISPLAY_GUN_VBO.get()) {
            if (configWasEnabled) {
                if (group != null) group.clear();
                APPEARANCES.clear();
            }
            configWasEnabled = false;
            return false;
        }
        configWasEnabled = true;
        if (overlay != OverlayTexture.NO_OVERLAY || owner.getLevel() != Minecraft.getInstance().level) return false;

        ItemStack stack = stack(owner, index);
        if (stack.isEmpty() || IGun.getIGunOrNull(stack) == null) return false;
        DisplayGunSlot slot = slot(owner, index);
        Snapshot current = snapshot(slot);
        if (current == null) return false;
        if (group == null) group = WorldMeshRenderer.createGroup("tacz:display_guns",
                WorldMeshStrategy.INSTANCE, Set.of(WorldMeshStrategy.INSTANCE));
        group.track(slot, ADAPTER);
        if (slot.consumeChange()) group.markDirty(slot);
        group.claimForWorldDraw(slot, current.key());
        return true;
    }

    private static DisplayGunSlot slot(BlockEntity owner, int index) {
        if (owner instanceof GunPegboardBlockEntity board) return board.getDisplaySlot(index);
        if (owner instanceof GunDisplayCaseBlockEntity showcase) return showcase.getDisplaySlot(index);
        throw new IllegalArgumentException("Unsupported display block entity: " + owner.getClass());
    }

    private static ItemStack stack(BlockEntity owner, int index) {
        if (owner instanceof GunPegboardBlockEntity board) return board.getItem(index);
        if (owner instanceof GunDisplayCaseBlockEntity showcase) return showcase.getItem(index);
        return ItemStack.EMPTY;
    }

    private static Direction facing(BlockEntity owner) {
        if (owner instanceof GunPegboardBlockEntity board) return board.getBlockState().getValue(GunPegboardBlock.FACING);
        if (owner instanceof GunDisplayCaseBlockEntity showcase)
            return showcase.getBlockState().getValue(GunDisplayCaseBlock.FACING);
        return Direction.NORTH;
    }

    private static int rotation(BlockEntity owner, int index) {
        if (owner instanceof GunPegboardBlockEntity board) return board.getRotation(index);
        if (owner instanceof GunDisplayCaseBlockEntity showcase) return showcase.getRotation(index);
        return 0;
    }

    private static Snapshot snapshot(DisplayGunSlot slot) {
        BlockEntity owner = slot.owner();
        if (owner.isRemoved()) return null;
        long appearanceRevision = slot.appearanceRevision();
        long resourceRevision = ClientIndexManager.resourceRevision();
        CachedAppearance cached = APPEARANCES.get(slot);
        if (cached == null || cached.appearanceRevision() != appearanceRevision
                || cached.resourceRevision() != resourceRevision) {
            cached = resolveAppearance(slot, appearanceRevision, resourceRevision);
            APPEARANCES.put(slot, cached);
        }
        if (cached.near() == cached.far()) return cached.near();
        int range = RenderConfig.GUN_LOD_RENDER_DISTANCE.get();
        boolean useLod = range == 0 || owner.getBlockPos().distToCenterSqr(
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) >= (double) range * range;
        return useLod ? cached.far() : cached.near();
    }

    private static CachedAppearance unavailable(long appearanceRevision, long resourceRevision) {
        return new CachedAppearance(appearanceRevision, resourceRevision, null, null);
    }

    private static CachedAppearance resolveAppearance(DisplayGunSlot slot, long appearanceRevision,
                                                      long resourceRevision) {
        ItemStack stored = stack(slot.owner(), slot.index());
        if (stored.isEmpty()) return unavailable(appearanceRevision, resourceRevision);
        ItemStack stack = stored.copy();
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return unavailable(appearanceRevision, resourceRevision);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (display == null) return unavailable(appearanceRevision, resourceRevision);
        ExternalGunModel model = display.getExternalGunModel();
        if (model == null) return unavailable(appearanceRevision, resourceRevision);
        ResourceLocation texture = display.getModelTexture();
        Pair<ExternalGunModel, ResourceLocation> lod = display.getExternalLodModel();
        if (display.getLodModel() != null && lod == null || texture == null)
            return unavailable(appearanceRevision, resourceRevision);

        List<AttachmentKey> attachments = new ArrayList<>();
        boolean lodAffectsGeometry = lod != null;
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) continue;
            ItemStack attached = gun.getAttachment(stack, type);
            if (attached.isEmpty()) attached = gun.getBuiltinAttachment(stack, type);
            IAttachment item = IAttachment.getIAttachmentOrNull(attached);
            if (item == null) continue;
            ResourceLocation id = item.getAttachmentId(attached);
            if (TimelessAPI.getClientAttachmentVariantIndex(attached).isPresent()
                    || SlotAdapterHelper.getEffectiveSlotAdapter(stack, type, id) != null)
                return unavailable(appearanceRevision, resourceRevision);
            ClientAttachmentIndex index = TimelessAPI.getClientAttachmentIndex(id).orElse(null);
            if (index == null) return unavailable(appearanceRevision, resourceRevision);
            lodAffectsGeometry |= index.getLodModel() != null;
            attachments.add(new AttachmentKey(type, id));
        }
        List<AttachmentKey> attachmentKeys = List.copyOf(attachments);
        boolean bullet = gun.hasBulletInBarrel(stack);
        boolean ammo = gun.getCurrentAmmoCount(stack) > 0;
        GeometryKey nearKey = new GeometryKey(resourceRevision, display.getDisplayId(),
                attachmentKeys, bullet, ammo, false);
        Snapshot near = new Snapshot(nearKey, stack, display, model, texture, false);
        Snapshot far = near;
        if (lodAffectsGeometry) {
            ExternalGunModel farModel = lod == null ? model : lod.getLeft();
            ResourceLocation farTexture = lod == null ? texture : lod.getRight();
            if (farModel == null || farTexture == null) return unavailable(appearanceRevision, resourceRevision);
            GeometryKey farKey = new GeometryKey(resourceRevision, display.getDisplayId(),
                    attachmentKeys, bullet, ammo, true);
            far = new Snapshot(farKey, stack, display, farModel, farTexture, true);
        }
        return new CachedAppearance(appearanceRevision, resourceRevision, near, far);
    }
}
