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
    private boolean forceOcularCenter = false;
    private float scopeViewRadiusModifier = 1;

    private record OcularWrapper (ModelRendererWrapper renderer, boolean isScope) {}

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
        if (path == null) {
            path = getPath(modelMap.get(SCOPE_VIEW_NODE + "_1"));
        }
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
        if (path == null) {
            path = getPath(modelMap.get(DIVISION_NODE + "_1"));
        }
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
    public void setOcularCenter(boolean ocularCenter) {
        this.forceOcularCenter = ocularCenter;
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



    public void render(@Nullable ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        this.currentGunItem = currentGunItem;
        this.attachmentItem = attachmentItem;
        if (transformType.firstPerson()) { // 仅第一人称渲染准心
            if (isScope && isSight) { // 组合镜
                renderBoth(matrixStack, transformType, renderType, light, overlay);
            } else if (isScope) {
                renderScope(matrixStack, transformType, renderType, light, overlay);
            } else if (isSight) {
                renderSight(matrixStack, transformType, renderType, light, overlay);
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

    /**
     * GL渲染逻辑：
     * 重置所有地方模板值为 0
     * 渲染 ocular_ring
     * 对第 n 个 ocular 创建模板值 n，任意 n > 0
     * 在模板值为 0 的地方渲染 scope_body
     * 对第 n 个 ocular 在模板值为 n 的地方处理遮罩和准心渲染
     * 关闭模板功能
     * 正常渲染其他部分
     */
    private void renderBoth(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        // 目前跟renderScope没有区别
        renderScope(matrixStack, transformType, renderType, light, overlay);
    }
    private void renderScope(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        RenderHelper.enableItemEntityStencilTest();
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

        renderOcularRing(matrixStack, transformType, renderType, light, overlay);

        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, STENCIL_BOTH);

        renderScopeBody(matrixStack, transformType, renderType, light, overlay);

        renderOcularAndDivision(matrixStack, transformType, renderType, light, overlay);

        // 关闭模板缓冲，渲染其他部分
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        super.render(matrixStack, transformType, renderType, light, overlay);
    }

    /**
     * GL渲染逻辑：
     * 重置所有地方模板值为 0
     * 对第 n 个 ocular 创建模板值 n，任意 n > 0
     * 对第 n 个 ocular 在模板值为 n 的地方渲染准心
     * 关闭模板功能
     * 无视模板值渲染 scope_body (ocular不挡住镜身)
     * 正常渲染其他部分
     */
    private void renderSight(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        RenderHelper.enableItemEntityStencilTest();
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, STENCIL_SIGHT);

        renderDivision(matrixStack, transformType, renderType, light, overlay);

        // 关闭模板缓冲，渲染镜身及其他部分
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        // 已经关掉了模板测试，不会被ocular挡住
        renderScopeBody(matrixStack, transformType, renderType, light, overlay);

        super.render(matrixStack, transformType, renderType, light, overlay);
    }

    private void renderOcularRing(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        if (ocularRingPath != null) {
            // 无视模板值，先保证ocular_ring始终渲染
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP); // 即使渲染通过也不要修改模板值
            renderPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
        }
    }

    private static final int STENCIL_SIGHT = 1;
    private static final int STENCIL_SCOPE = 2;
    private static final int STENCIL_BOTH = 3;
    /**
     * 对第 n 个ocular创建模板值 n
     * 不会还原模板条件
     */
    private void renderOcularStencil(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, int stencilMode) {
        // 渲染 ocular 模型并创建模板
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE); // 测试通过时替换模板值

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
                    /**
                     * 模板缓冲区清空时，背景均为 0
                     * 第 n 个ocular使用模板值 n
                     * ocular自己是需要渲染的，后续渲染division时再覆盖ocular
                     * 这里是设置模板值还是渲染ocular?
                     */
                    RenderSystem.stencilFunc(GL11.GL_ALWAYS, i + 1, 0xFF); // 始终通过，并替换模板值
                    renderPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
                }
            }
        }
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    private void renderScopeBody(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        if (scopeBodyPath != null) {
            // 仅在没被ocular写入模板值的范围渲染scope_body
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
            renderPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }
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

    // 利用提前设置好的模板值，渲染目镜遮罩和准心
    private void renderOcularAndDivision(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        if (ocularNodePaths.isEmpty()) {
            return;
        }

        // 圆形模板层
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.stencilMask(0xFF);
        // 80是一个随便找的大小合适的数值。
        float rad = 80 * scopeViewRadiusModifier;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            // 瞄准进度越小，圆越小
            rad *= IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(Minecraft.getInstance().getFrameTime());
        }
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT); // 写入反转值
        for (int i = 0; i < ocularNodePaths.size(); i++) {
            if (!isScopeOcular.get(i)) {
                continue;
            }

            // 第 n 个ocular使用模板值 n
            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF); // 写入 ~(i + 1) & 0xFF

            /**
             * {@link com.tacz.guns.client.resource.index.ClientAttachmentIndex#checkDisplay}
             * ocular_scope枢轴点不居中会影响此处计算，可在配置里设置成不需要居中（遮罩效果不同）
             */
            Vector3f ocularCenter = this.forceOcularCenter ? new Vector3f(0) : getBedrockPartCenter(matrixStack, ocularNodePaths.get(i));
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
            if (!isScopeOcular.get(i)) {
                continue;
            }
            // 第 n 个ocular使用模板值 n
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
            if (!isScopeOcular.get(i)) {
                // 第 n 个ocular使用模板值 n
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                renderPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
            } else {
                // 渲染目镜自己的材质
                // 第 n 个ocular使用模板值 n
                // 模板值 n 为没被准心覆盖的部分
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                renderPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));

                // 渲染准心
                // 在模板值反转的地方渲染
                int erased = ~(i+1) & 0xFF; // ~1 = 254
                RenderSystem.stencilFunc(GL11.GL_EQUAL, erased, 0xFF);
                renderPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
            }
        }
        RenderSystem.enableDepthTest();
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
}