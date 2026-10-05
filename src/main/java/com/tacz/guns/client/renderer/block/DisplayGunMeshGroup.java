package com.tacz.guns.client.renderer.block;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.world.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.GunPegboardSlotHelper;
import com.tacz.guns.block.entity.DisplayGunSlot;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** 每个悬挂物独立登记；稳定身份由所属方块实体创建和持有。 */
public class DisplayGunMeshGroup {
    private static final int GRID_SIZE = 4;
    private static final double ITEM_DEPTH = 0.25D;
    private static WorldMeshGroup<DisplayGunSlot> group;
    private static boolean configWasEnabled;
    private static final Map<DisplayGunSlot, CachedAppearance> APPEARANCES = new WeakHashMap<>();

    private DisplayGunMeshGroup() {}

    private record CachedAppearance(long appearanceRevision, long resourceRevision,
                                    ExternalGunAppearance.Variants variants) {}

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
            int localIndex = owner instanceof GunPegboardBlockEntity ? index % GunPegboardBlockEntity.SLOTS_PER_FACE : index;
            double y = ((localIndex / GRID_SIZE) + 0.5D) / GRID_SIZE - 0.5D;
            PoseStack pose = new PoseStack();
            Direction facing = owner instanceof GunPegboardBlockEntity
                    ? GunPegboardSlotHelper.getSlotFacing(owner.getBlockState(), index) : facing(owner);
            DisplayItemTransforms.apply(pose, facing, x, y, ITEM_DEPTH, rotation(owner, index));
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
            ExternalGunAppearance.Snapshot current = snapshot(slot);
            return current == null ? slot : current.key();
        }

        @Override
        public boolean collectGeometry(DisplayGunSlot slot, GeometryCollector collector) {
            ExternalGunAppearance.Snapshot current = snapshot(slot);
            return current != null && DisplayGunGeometry.capture(current, ItemDisplayContext.FIXED, collector);
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
        ExternalGunAppearance.Snapshot current = snapshot(slot);
        if (current == null) return false;
        if (group == null) group = WorldMeshRenderer.createGroup("tacz:display_guns",
                WorldMeshStrategy.INSTANCE, Set.of(WorldMeshStrategy.INSTANCE), MeshCachePolicy.DEFAULT);
        group.track(slot, ADAPTER);
        if (slot.consumeChange()) group.markDirty(slot);
        CachedAppearance cached = APPEARANCES.get(slot);
        ExternalGunAppearance.Snapshot previous = cached.variants().select(!current.useLod());
        group.claimForWorldDraw(slot, current.key(), previous.key());
        // Pending capture/upload stays owned by the VBO path; ordinary rendering is too expensive.
        return true;
    }

    public static DisplayGunSlot slot(BlockEntity owner, int index) {
        if (owner instanceof GunPegboardBlockEntity board) return board.getDisplaySlot(index);
        if (owner instanceof GunDisplayCaseBlockEntity showcase) return showcase.getDisplaySlot(index);
        throw new IllegalArgumentException("Unsupported display block entity: " + owner.getClass());
    }

    public static ItemStack stack(BlockEntity owner, int index) {
        if (owner instanceof GunPegboardBlockEntity board) return board.getItem(index);
        if (owner instanceof GunDisplayCaseBlockEntity showcase) return showcase.getItem(index);
        return ItemStack.EMPTY;
    }

    public static Direction facing(BlockEntity owner) {
        if (owner instanceof GunPegboardBlockEntity board) return board.getBlockState().getValue(GunPegboardBlock.FACING);
        if (owner instanceof GunDisplayCaseBlockEntity showcase)
            return showcase.getBlockState().getValue(GunDisplayCaseBlock.FACING);
        return Direction.NORTH;
    }

    public static int rotation(BlockEntity owner, int index) {
        if (owner instanceof GunPegboardBlockEntity board) return board.getRotation(index);
        if (owner instanceof GunDisplayCaseBlockEntity showcase) return showcase.getRotation(index);
        return 0;
    }

    public static ExternalGunAppearance.Snapshot snapshot(DisplayGunSlot slot) {
        BlockEntity owner = slot.owner();
        if (owner.isRemoved()) return null;
        long appearanceRevision = slot.appearanceRevision();
        long resourceRevision = ClientIndexManager.resourceRevision();
        CachedAppearance cached = APPEARANCES.get(slot);
        if (cached == null || cached.appearanceRevision() != appearanceRevision
                || cached.resourceRevision() != resourceRevision) {
            cached = new CachedAppearance(appearanceRevision, resourceRevision,
                    ExternalGunAppearance.resolve(stack(owner, slot.index())));
            APPEARANCES.put(slot, cached);
        }
        if (cached.variants() == null) return null;
        if (cached.variants().near() == cached.variants().far()) return cached.variants().near();
        int range = RenderConfig.GUN_LOD_RENDER_DISTANCE.get();
        boolean useLod = range == 0 || owner.getBlockPos().distToCenterSqr(
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) >= (double) range * range;
        return cached.variants().select(useLod);
    }
}
