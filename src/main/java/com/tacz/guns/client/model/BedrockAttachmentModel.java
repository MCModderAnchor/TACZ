package com.tacz.guns.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.model.functional.TextShowRender;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedrockAttachmentModel extends BedrockAnimatedModel {
    private static final String SCOPE_VIEW_NODE = "scope_view";
    private static final String SCOPE_BODY_NODE = "scope_body";
    private static final String OCULAR_RING_NODE = "ocular_ring";
    private static final String DIVISION_NODE = "division";
    private static final String OCULAR_NODE = "ocular";
    private static final String OCULAR_SIGHT_NODE = "ocular_sight";
    private static final String OCULAR_SCOPE_NODE = "ocular_scope";
    private static final Pattern OCULAR_PATTERN = Pattern.compile("^(" + OCULAR_NODE + "|" + OCULAR_SIGHT_NODE + "|" + OCULAR_SCOPE_NODE + ")(_(\\d+))?$");
    private static final Pattern LASER_BEAM_PATTERN = Pattern.compile("^laser_beam(_(\\d+))?$");

    protected List<List<BedrockPart>> scopeViewPaths;
    protected @Nullable List<BedrockPart> scopeBodyPath;
    protected @Nullable List<BedrockPart> ocularRingPath;
    protected List<List<BedrockPart>> ocularNodePaths;
    protected List<Boolean> isScopeOcular;
    protected List<List<BedrockPart>> divisionNodePaths;
    protected List<List<BedrockPart>> laserBeamPaths; // 在构造函数里被new了，不会为null

    private @Nullable ItemStack currentGunItem;
    private @Nullable ItemStack attachmentItem;

    private boolean isScope = false;
    private boolean isSight = false;
    private boolean scopeCompat = true;
    private float scopeViewRadiusModifier = 1;

    public BedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        scopeViewPaths = new ArrayList<>();
        ocularNodePaths = new ArrayList<>();
        isScopeOcular = new ArrayList<>();
        divisionNodePaths = new ArrayList<>();
        laserBeamPaths = new ArrayList<>();

        /**
         * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
         * 按如下顺序命名：
         * scope_view
         * scope_view_2
         * scope_view_3
         * scope_view_4
         * ...
         * scope_view_n
         */
        // 初始化 view 的 node path
        List<BedrockPart> path = getPath(modelMap.get(SCOPE_VIEW_NODE));
        int i = 2;
        while (path != null) {
            scopeViewPaths.add(path); // 第一次是无后缀（scope_view），兼容老的命名
            path = getPath(modelMap.get(SCOPE_VIEW_NODE + '_' + i++)); // 第二次开始是scope_view_2
        }

        TreeMap<Integer, OcularWrapper> map = new TreeMap<>();
        for (Map.Entry<String, ModelRendererWrapper> entry : modelMap.entrySet()) { // 遍历所有分组
            /**
             * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
             * 以 ocular 或 ocular_sight 或 ocular_scope开头，直接结尾或者以下划线+数字结尾
             * 可能的命名方式：
             * ocular
             * ocular_1
             * ocular_2
             * ocular_sight_3
             * ocular_scope_n
             * 读取完的结果自动按后缀排序，其中ocular和ocular_1都算作1，而entrySet的遍历方式不确定顺序，后加载的覆盖前加载的，所以ocular（不带后缀）和ocular_1（带后缀）只能用其一
             * ocular，ocular_sight，ocular_scope共享后缀的排序，例如ocular_sight_2和ocular_scope_2只能用其一
             * ocular和ocular_sight无区别
             * ocular_scope影响此处分支 {@link BedrockAttachmentModel#renderOcularAndDivision(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, boolean isOcularSelective)}
             */
            // 初始化 ocular 的 node path
            Matcher matcher = OCULAR_PATTERN.matcher(entry.getKey());
            if (matcher.matches()) {
                String numStr = matcher.group(3);
                int num = numStr != null ? Integer.parseInt(numStr) : 1; // 无后缀则视为后缀为1
                boolean isScope = OCULAR_SCOPE_NODE.equals(matcher.group(1));
                map.put(num, new OcularWrapper(entry.getValue(), isScope));
            }
            /**
             * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
             * 激光剑组名：以 laser_beam 开头，直接结尾或者以下划线+数字结尾
             * 读取完的结果不自动排序，且顺序不定
             * 带后缀和不带后缀的可同时存在，且可重复
             */
            // 初始化 laser 的 node path
            if (LASER_BEAM_PATTERN.matcher(entry.getKey()).find()) {
                laserBeamPaths.add(getPath(entry.getValue()));
            }
        }
        for (OcularWrapper wrapper : map.values()) {
            // 直接用List<OcularWrapper>也行？
            ocularNodePaths.add(getPath(wrapper.renderer));
            isScopeOcular.add(wrapper.isScope);
        }

        /**
         * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
         * 按如下顺序命名：
         * division
         * division_2
         * division_3
         * division_4
         * ...
         * division_n
         */
        // 初始化 division 的 node path
        ModelRendererWrapper divisionModel = modelMap.get(DIVISION_NODE);
        path = getPath(modelMap.get(DIVISION_NODE));
        i = 2;
        while (path != null) {
            divisionNodePaths.add(path);
            divisionModel.setHidden(true); // 默认隐藏，开镜时再显示
            divisionModel = modelMap.get(DIVISION_NODE + '_' + i++);
            path = getPath(divisionModel);
        }

        /**
         * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
         * 分组scope_body
         */
        scopeBodyPath = getPath(modelMap.get(SCOPE_BODY_NODE));
        /**
         * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
         * 分组ocular_ring
         */
        ocularRingPath = getPath(modelMap.get(OCULAR_RING_NODE));
    }

//    public int tempLogCount = 0;
    @Nullable
    public List<BedrockPart> getScopeViewPath(int viewIndex) {
        /**
         * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
         */
        if (scopeViewPaths.isEmpty()) {
            return null;
        }
        /**
         * viewIndex隐式依赖{@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}的检查，使得其不会为-1
         */

//        if (tempLogCount < 3) {
//            tempLogCount++;
//            GunMod.LOGGER.debug(tempLogCount == 1 ? "+------------------+------------------------------" : "+------");
//            GunMod.LOGGER.debug("tempLogCount:{}", tempLogCount);
//            GunMod.LOGGER.debug("scopeViewPaths.size()={}, viewIndex={}", scopeViewPaths.size(), viewIndex);
//            List<BedrockPart> returnValue = scopeViewPaths.get(viewIndex >= scopeViewPaths.size() ? 0 : viewIndex);
//            GunMod.LOGGER.debug("scopeViewPaths.get({}).get(0).name={}, scopeViewPaths.get({}).size={}",
//                    viewIndex >= scopeViewPaths.size() ? 0 : viewIndex,
//                    returnValue.get(0).name,
//                    viewIndex >= scopeViewPaths.size() ? 0 : viewIndex,
//                    returnValue.size());
//            if (viewIndex >= scopeViewPaths.size()) {
//                if (tempLogCount > 1) GunMod.LOGGER.debug("非首次:");
//                GunMod.LOGGER.warn("display文件views与模型内scope_view数量不符: viewIndex ({}) >= scopeViewPaths.size ({})", viewIndex, scopeViewPaths.size());
//            }
//            for (BedrockPart b : returnValue) {
//                GunMod.LOGGER.debug("name={}", b.name);
//            }
//            if (attachmentItem != null) {
//                GunMod.LOGGER.debug("currentGunItem:{}, getOrCreateTag:{}, serializeNBT:{}", currentGunItem.toString(), currentGunItem.getOrCreateTag(), currentGunItem.serializeNBT());
//                GunMod.LOGGER.debug("attachmentItem:{}, getOrCreateTag:{}, serializeNBT:{}", attachmentItem.toString(), attachmentItem.getOrCreateTag(), attachmentItem.serializeNBT());
//            }
//        }

        return scopeViewPaths.get(viewIndex >= scopeViewPaths.size() ? 0 : viewIndex);
    }

    public void setIsScope(boolean isScope) {
        this.isScope = isScope;
    }

    public void setIsSight(boolean isSight) {
        this.isSight = isSight;
    }

    public void setScopeCompat(boolean scopeCompat) {
        this.scopeCompat = scopeCompat;
        if (this.scopeCompat) {
            compatibleCheck();
        }
    }

    public void compatibleCheck() {
        // 添加兼容处理
        // 官包的仅scope:true倍镜一半都没使用ocular_scope，导致isScopeOcular里出问题，后续渲染分支错误
        if (isScope() && !isSight()) {
            isScopeOcular.replaceAll(ignored -> true);
        }
        // ocular和ocular_sight都被当非筒镜用，实际不需要兼容
        if (!isScope() && isSight()) {
            isScopeOcular.replaceAll(ignored -> false);
        }
    }

    public boolean isScope() {
        return isScope;
    }

    public boolean isSight() {
        return isSight;
    }

    public void setScopeViewRadiusModifier(float scopeViewRadiusModifier) {
        this.scopeViewRadiusModifier = scopeViewRadiusModifier;
    }

    /**
     * 添加枪械自定义的文本显示
     */
    public void setTextShowList(Map<String, TextShow> textShowList) {
        textShowList.forEach((name, textShow) -> this.setFunctionalRenderer(name,
                bedrockPart -> new TextShowRender(this, textShow, currentGunItem)));
    }

    public void render(@Nullable ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        this.currentGunItem = currentGunItem;
        this.attachmentItem = attachmentItem;
        if (transformType.firstPerson()) { // 仅第一人称渲染准心
            if (isScope && isSight) { // 组合镜
                renderBoth(matrixStack, transformType, renderType, light, overlay);
                // GunMod.LOGGER.debug("isScope && isSight: renderBoth");
            } else if (isScope) {
                renderScope(matrixStack, transformType, renderType, light, overlay);
                // GunMod.LOGGER.debug("isScope: renderScope");
            } else if (isSight) {
                renderSight(matrixStack, transformType, renderType, light, overlay);
                // GunMod.LOGGER.debug("isSight: renderSight");
            }
        } else {
            if (scopeBodyPath != null) {
                renderPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
            }
            if (ocularRingPath != null) {
                renderPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
            }
        }
        // 非瞄准镜配件的激光
        if (!isScope && !isSight && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
            }
        }
        super.render(matrixStack, transformType, renderType, light, overlay);
        // 瞄准镜配件的激光
        if ((isScope || isSight) && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
            }
        }
    }

    private Vector3f getBedrockPartCenter(PoseStack poseStack, @Nonnull List<BedrockPart> path) {
        poseStack.pushPose();
        for (BedrockPart part : path) {
            part.translateAndRotateAndScale(poseStack);
        }
        Vector3f result = new Vector3f(poseStack.last().pose().m30(), poseStack.last().pose().m31(), poseStack.last().pose().m32());
        poseStack.popPose();
        return result;
    }

    // 渲染特定分组
    private void renderPart(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType,
                            int light, int overlay, @Nonnull List<BedrockPart> path) {
        poseStack.pushPose();
        for (int i = 0; i < path.size() - 1; ++i) {
            path.get(i).translateAndRotateAndScale(poseStack);
        }
        BedrockPart part = path.get(path.size() - 1);
        part.visible = true;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        part.render(poseStack, transformType, vertexConsumer, light, overlay);
        if (!OculusCompat.endBatch(bufferSource)) {
            bufferSource.endBatch(renderType);
        }
        part.visible = false;
        poseStack.popPose();
    }

    private static final int STENCIL_SIGHT = 1;
    private static final int STENCIL_SCOPE = 2;
    private static final int STENCIL_BOTH = 3;
    private void renderOcularStencil(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, int stencilMode) {
        // 渲染 ocular 模型以创建模板
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        // 模板测试函数：总是通过测试
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        if (!ocularNodePaths.isEmpty()) {
            // 绘制目镜
            for (int i = 0; i < ocularNodePaths.size(); i++) {
                boolean shouldRender = false;
                switch (stencilMode) {
                    case STENCIL_SIGHT:
                        if (!isScopeOcular.get(i)) shouldRender = true;
                        break;
                    case STENCIL_SCOPE:
                        if (isScopeOcular.get(i)) shouldRender = true;
                        break;
                    case STENCIL_BOTH:
                        shouldRender = true;
                }
                if (shouldRender) {
                    RenderSystem.stencilFunc(GL11.GL_ALWAYS, i + 1, 0xFF);
                    renderPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
                }
            }
        }
        // 恢复渲染状态，渲染镜身，但只渲染模板值为 0 的区域（ocular 外部）
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    private void renderDivision(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        if (!divisionNodePaths.isEmpty()) {
            RenderSystem.disableDepthTest();
            for (int i = 0; i < divisionNodePaths.size(); i++) {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                renderPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
            }
            RenderSystem.enableDepthTest();
        }
    }

    private void renderOcularAndDivision(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, boolean hasBothOcular) {
        if (ocularNodePaths.isEmpty()) {
            return;
        }

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        // 准备渲染圆形模板层
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        // 80是一个随便找的大小合适的数值。
        float rad = 80 * scopeViewRadiusModifier;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            rad *= IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(Minecraft.getInstance().getFrameTime());
        }
        RenderSystem.stencilMask(0xFF);

        // 圆形模板层
        for (int i = 0; i < ocularNodePaths.size(); i++) {
            if (hasBothOcular && !isScopeOcular.get(i)) {
                continue;
            }
            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);

            /**
             * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
             * ocular_scope枢轴点不居中会影响此处计算，现版本默认不会产生影响
             */
            Vector3f ocularCenter = scopeCompat ? new Vector3f(0) : getBedrockPartCenter(matrixStack, ocularNodePaths.get(i));
            // GunMod.LOGGER.debug("Ocular Center: x={}, y={}, z={}", ocularCenter.x(), ocularCenter.y(), ocularCenter.z());

            float centerX = ocularCenter.x() * 16 * 90;
            float centerY = ocularCenter.y() * 16 * 90;
            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(centerX, centerY, -90.0D).color(255, 255, 255, 255).endVertex();
            for (int j = 0; j <= 90; j++) {
                float angle = (float) j * ((float) Math.PI * 2F) / 90.0F;
                float sin = Mth.sin(angle);
                float cos = Mth.cos(angle);
                builder.vertex(centerX + cos * rad, centerY + sin * rad, -90.0D).color(255, 255, 255, 255).endVertex();
            }
            BufferUploader.drawWithShader(builder.end());
        }
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        // 目镜遮罩
        for (int i = 0; i < ocularNodePaths.size(); i++) {
            if (hasBothOcular && !isScopeOcular.get(i)) {
                continue;
            }

            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
            renderPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
        }

        // 准心
        RenderSystem.disableDepthTest(); // 让 division 压住镜身
        for (int i = 0; i < ocularNodePaths.size() && i < divisionNodePaths.size(); i++) {
            if (i > Byte.MAX_VALUE) {
                /**
                 * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
                 * ocular和division分组的数量需小于128
                 */
                throw new IllegalArgumentException("Index of oculus is out of range for 127");
            }
            /**
             * isScopeOcular在此处设置 {@link BedrockAttachmentModel#BedrockAttachmentModel}
             */
            if (hasBothOcular && !isScopeOcular.get(i)) {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                renderPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
            } else {
                // 渲染目镜黑色遮罩
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                renderPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));

                // 渲染准心
                int erased = ~(i+1) & 0xFF;
                RenderSystem.stencilFunc(GL11.GL_EQUAL, erased, 0xFF);
                renderPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
            }
        }
        RenderSystem.enableDepthTest();
    }

    // 渲染组合镜
    private void renderBoth(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        RenderHelper.enableItemEntityStencilTest();
        // 清空模板缓冲区、准备绘制模板缓冲
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        // 渲染目镜外环
        if (ocularRingPath != null) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            renderPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
        }
        // 渲染目镜以写入模板桓冲值
        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, STENCIL_BOTH);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        // 渲染镜身
        if (scopeBodyPath != null) {
            renderPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }
        // 渲染目镜遮罩和准心
        renderOcularAndDivision(matrixStack, transformType, renderType, light, overlay, true);
        // 关闭模板缓冲
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        // 渲染其他部分
        super.render(matrixStack, transformType, renderType, light, overlay);
    }
    private void renderSight(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        RenderHelper.enableItemEntityStencilTest();
        // 清空模板缓冲区、准备绘制模板缓冲
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        // 渲染目镜以写入模板桓冲值
        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, STENCIL_SIGHT);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        // 渲染准心
        renderDivision(matrixStack, transformType, renderType, light, overlay);
        // 关闭模板缓冲
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();

        // 渲染镜身
        if (scopeBodyPath != null) {
            renderPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }
        // 渲染其他部分
        super.render(matrixStack, transformType, renderType, light, overlay);
    }
    private void renderScope(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        RenderHelper.enableItemEntityStencilTest();
        // 清空模板缓冲区、准备绘制模板缓冲
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        // 渲染目镜外环
        if (ocularRingPath != null) {
            // 设置模板测试为始终通过，并确保不修改模板缓冲区
            // 这两个状态会被后续的 `renderOcularStencil` 调用覆盖，因此不需要移到外面
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            renderPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
        }
        // 渲染目镜以写入模板桓冲值
        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, STENCIL_SCOPE);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        // 渲染镜身
        if (scopeBodyPath != null) {
            renderPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }
        // 渲染目镜遮罩和准心
        renderOcularAndDivision(matrixStack, transformType, renderType, light, overlay, false);
        // 关闭模板缓冲
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        // 渲染其他部分
        super.render(matrixStack, transformType, renderType, light, overlay);
    }

    private static class OcularWrapper{
        public ModelRendererWrapper renderer;
        public boolean isScope;

        public OcularWrapper (ModelRendererWrapper renderer, boolean isScope){
            this.renderer = renderer;
            this.isScope = isScope;
        }
    }
}