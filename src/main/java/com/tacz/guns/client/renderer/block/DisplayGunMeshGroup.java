package com.tacz.guns.client.renderer.block;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.cache.MeshCachePolicy;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.capture.GeometryCollector;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.world.MeshRenderableAdapter;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.world.WorldMeshGroup;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.world.WorldMeshRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.world.WorldMeshStrategy;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.entity.DisplayGunSlot;
import com.tacz.guns.block.entity.GunDisplayBlockEntity;
import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** 每个悬挂物独立登记；稳定身份由所属方块实体创建和持有。 */
public class DisplayGunMeshGroup {
    private static final double ITEM_DEPTH = 0.25D;
    private static WorldMeshGroup<DisplayGunSlot> group;
    private static boolean configWasEnabled;
    private static final Map<DisplayGunSlot, CachedAppearance> APPEARANCES = new WeakHashMap<>();
    public static final MeshCachePolicy TTL_300 = new MeshCachePolicy(Duration.ofSeconds(300), 128,
            512L * 1024L * 1024L);

    private DisplayGunMeshGroup() {
    }

    private record CachedAppearance(long appearanceRevision, long resourceRevision,
                                    ExternalGunAppearance.Resolution resolution) {
    }

    private static final MeshRenderableAdapter<DisplayGunSlot> ADAPTER = new MeshRenderableAdapter<>() {
        @Override
        public boolean isValid(DisplayGunSlot slot) {
            GunDisplayBlockEntity owner = slot.owner();
            ItemStack stack = owner.getItem(slot.index());
            boolean valid = !owner.isRemoved() && owner.getLevel() == Minecraft.getInstance().level
                    && !stack.isEmpty() && IGun.getIGunOrNull(stack) != null;
            if (!valid) {
                APPEARANCES.remove(slot);
            }
            return valid;
        }

        @Override
        public boolean isVisible(DisplayGunSlot slot) {
            return snapshot(slot) != null;
        }

        @Override
        public Vec3 origin(DisplayGunSlot slot) {
            return Vec3.atLowerCornerOf(slot.owner().getBlockPos());
        }

        @Override
        public Matrix4f localTransform(DisplayGunSlot slot) {
            GunDisplayBlockEntity owner = slot.owner();
            int index = slot.index();
            int gridSize = owner.getGridSize();
            int localIndex = index % (gridSize * gridSize);
            double x = ((localIndex % gridSize) + 0.5D) / gridSize - 0.5D;
            double y = ((localIndex / gridSize) + 0.5D) / gridSize - 0.5D;
            PoseStack pose = new PoseStack();
            DisplayItemTransforms.apply(pose, owner.getSlotFacing(index), x, y, ITEM_DEPTH, owner.getRotation(index));
            return new Matrix4f(pose.last().pose());
        }

        @Override
        public int packedLight(DisplayGunSlot slot) {
            GunDisplayBlockEntity owner = slot.owner();
            if (owner.isDisplayGlowing()) {
                return LightTexture.pack(15, 15);
            }
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

    /** 方块实体渲染器对当前槽位调用；返回 true 表示由 SBM 接管，包括等待模型或 VBO 就绪。 */
    public static boolean tryRender(GunDisplayBlockEntity owner, int index, int overlay) {
        if (!RenderConfig.DISPLAY_GUN_VBO.get()) {
            if (configWasEnabled) {
                if (group != null) {
                    group.clear();
                }
                APPEARANCES.clear();
            }
            configWasEnabled = false;
            return false;
        }
        configWasEnabled = true;
        if (overlay != OverlayTexture.NO_OVERLAY || owner.getLevel() != Minecraft.getInstance().level) {
            return false;
        }

        ItemStack stack = owner.getItem(index);
        if (stack.isEmpty() || IGun.getIGunOrNull(stack) == null) {
            return false;
        }

        DisplayGunSlot slot = owner.getDisplaySlot(index);
        ExternalGunAppearance.Snapshot current = snapshot(slot);
        CachedAppearance cached = APPEARANCES.get(slot);
        if (current == null) {
            return cached != null && cached.resolution().pending();
        }

        if (group == null) {
            group = WorldMeshRenderer.createGroup(
                    "tacz:display_guns",
                    WorldMeshStrategy.INSTANCE,
                    Set.of(WorldMeshStrategy.INSTANCE),
                    TTL_300
            );
        }
        group.track(slot, ADAPTER);
        if (slot.consumeChange()) {
            group.markDirty(slot);
        }

        ExternalGunAppearance.Snapshot previous = cached.resolution().variants().select(!current.useLod());
        group.claimForWorldDraw(slot, current.key(), previous.key());
        // 捕获或上传尚未完成时继续由 VBO 路径接管，避免回退到开销较大的普通渲染。
        return true;
    }

    public static ExternalGunAppearance.Snapshot snapshot(DisplayGunSlot slot) {
        GunDisplayBlockEntity owner = slot.owner();
        if (owner.isRemoved()) {
            return null;
        }

        long appearanceRevision = slot.appearanceRevision();
        long resourceRevision = ClientIndexManager.resourceRevision();
        CachedAppearance cached = APPEARANCES.get(slot);
        if (cached == null || cached.appearanceRevision() != appearanceRevision
                || cached.resourceRevision() != resourceRevision || cached.resolution().pending()) {
            cached = new CachedAppearance(appearanceRevision, resourceRevision,
                    ExternalGunAppearance.resolve(owner.getItem(slot.index())));
            APPEARANCES.put(slot, cached);
        }
        ExternalGunAppearance.Variants variants = cached.resolution().variants();
        if (variants == null) {
            return null;
        }
        if (variants.near() == variants.far()) {
            return variants.near();
        }

        int range = RenderConfig.GUN_LOD_RENDER_DISTANCE.get();
        boolean useLod = range == 0 || owner.getBlockPos().distToCenterSqr(
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()) >= (double) range * range;
        return variants.select(useLod);
    }
}
