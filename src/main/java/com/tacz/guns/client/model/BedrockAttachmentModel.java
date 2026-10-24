package com.tacz.guns.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.model.bedrock.BedrockCube;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.model.functional.RangefinderImpactPredictionRender;
import com.tacz.guns.client.model.functional.TextShowRender;
import com.tacz.guns.client.renderer.scope.ScopeLensRenderer;
import com.tacz.guns.client.resource.pojo.display.attachment.ScopeLensConfig;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.client.util.FirstPersonRenderMath;
import com.tacz.guns.compat.ar.ARCompat;
import com.tacz.guns.compat.iris.IrisCompat;
import com.tacz.guns.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

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
    private static final String RANGEFINDER_IMPACT_PREDICTION_NODE = "rangefinder_impact_prediction_pos";
    private static final Pattern LASER_BEAM_PATTERN = Pattern.compile("^laser_beam(_(\\d+))?$");

    protected List<List<BedrockPart>> scopeViewPaths;
    protected @Nullable List<BedrockPart> scopeBodyPath;
    protected @Nullable List<BedrockPart> ocularRingPath;
    protected List<List<BedrockPart>> ocularNodePaths;
    protected List<Boolean> isScopeOcular;
    protected List<List<BedrockPart>> divisionNodePaths;
    protected @Nullable List<List<BedrockPart>> laserBeamPaths;

    private @Nullable ItemStack currentGunItem;
    private @Nullable ItemStack attachmentItem;

    private boolean isScope = false;
    private boolean isSight = false;
    private ScopeLensConfig scopeLens = new ScopeLensConfig();
    private float scopeViewRadiusModifier = 1;

    public BedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        scopeViewPaths = new ArrayList<>();
        ocularNodePaths = new ArrayList<>();
        isScopeOcular = new ArrayList<>();
        divisionNodePaths = new ArrayList<>();
        laserBeamPaths = new ArrayList<>();
        // 初始化 view 的 node path
        List<BedrockPart> path = getPath(modelMap.get(SCOPE_VIEW_NODE));
        int i = 2;
        while (path != null) {
            scopeViewPaths.add(path);
            path = getPath(modelMap.get(SCOPE_VIEW_NODE + '_' + i++));
        }
        // 初始化 ocular 的 node path
        String ocularRegex = "^(" + OCULAR_NODE + "|" + OCULAR_SIGHT_NODE + "|" + OCULAR_SCOPE_NODE + ")(_(\\d+))?$";
        Pattern ocularPattern = Pattern.compile(ocularRegex);
        TreeMap<Integer, OcularWrapper> map = new TreeMap<>();
        for (Map.Entry<String, ModelRendererWrapper> entry : modelMap.entrySet()) {
            Matcher matcher = ocularPattern.matcher(entry.getKey());
            if (matcher.matches()) {
                int num = 1;
                String numStr = matcher.group(3);
                if (numStr != null) {
                    num = Integer.parseInt(numStr);
                }
                String type = matcher.group(1);
                boolean isScope = OCULAR_SCOPE_NODE.equals(type);
                map.put(num, new OcularWrapper(entry.getValue(), isScope));
            }
            if (LASER_BEAM_PATTERN.matcher(entry.getKey()).find()) {
                laserBeamPaths.add(getPath(entry.getValue()));
            }
        }
        for (OcularWrapper wrapper : map.values()) {
            ocularNodePaths.add(getPath(wrapper.renderer));
            isScopeOcular.add(wrapper.isScope);
        }
        // 初始化 division 的 node path
        ModelRendererWrapper divisionModel = modelMap.get(DIVISION_NODE);
        path = getPath(modelMap.get(DIVISION_NODE));
        i = 2;
        while (path != null) {
            divisionNodePaths.add(path);
            divisionModel.setHidden(true);
            divisionModel = modelMap.get(DIVISION_NODE + '_' + i++);
            path = getPath(divisionModel);
        }

        scopeBodyPath = getPath(modelMap.get(SCOPE_BODY_NODE));
        ocularRingPath = getPath(modelMap.get(OCULAR_RING_NODE));
    }

    @Nullable
    public List<BedrockPart> getScopeViewPath(int viewSwitchCount) {
        if (scopeViewPaths.isEmpty()) {
            return null;
        }
        if (viewSwitchCount >= scopeViewPaths.size()) {
            return scopeViewPaths.get(0);
        }
        return scopeViewPaths.get(viewSwitchCount);
    }

    public void setIsScope(boolean isScope) {
        this.isScope = isScope;
    }

    public void setIsSight(boolean isSight) {
        this.isSight = isSight;
    }

    public boolean isScope() {
        return isScope;
    }

    public boolean isSight() {
        return isSight;
    }

    public void setScopeLens(ScopeLensConfig scopeLens) {
        this.scopeLens = scopeLens;
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
        boolean hasRangefinder = textShowList.values().stream().anyMatch(textShow -> textShow.getRangefinder() != null);
        if (hasRangefinder && modelMap.containsKey(RANGEFINDER_IMPACT_PREDICTION_NODE)) {
            this.setFunctionalRenderer(RANGEFINDER_IMPACT_PREDICTION_NODE,
                    bedrockPart -> new RangefinderImpactPredictionRender(this));
        }
    }

    public void render(@Nullable ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        render(attachmentItem, currentGunItem, matrixStack, transformType, renderType, light, overlay, 1.0F);
    }

    public void render(@Nullable ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, float alpha) {
        render(attachmentItem, currentGunItem, matrixStack, transformType, renderType, light, overlay, alpha, RenderHelper.getBufferSource(transformType));
    }

    public void render(@Nullable ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, float alpha, MultiBufferSource bufferSource) {
        this.currentGunItem = currentGunItem;
        this.attachmentItem = attachmentItem;
        if (transformType.firstPerson()) {
            if (isScope && isSight) {
                renderBoth(matrixStack, transformType, renderType, light, overlay, bufferSource);
            } else if (isScope) {
                renderScope(matrixStack, transformType, renderType, light, overlay, bufferSource);
            } else if (isSight) {
                renderSight(matrixStack, transformType, renderType, light, overlay, bufferSource);
            }
        } else {
            if (scopeBodyPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, alpha, bufferSource);
            }
            if (ocularRingPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath, alpha, bufferSource);
            }
        }
        if (!isScope && !isSight && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry, bufferSource);
            }
        }
        super.render(matrixStack, transformType, renderType, light, overlay, 1.0F, 1.0F, 1.0F, alpha, bufferSource);
        if ((isScope || isSight) && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry, bufferSource);
            }
        }
    }

    private Vector3f getBedrockPartCenter(PoseStack poseStack, @Nonnull List<BedrockPart> path) {
        poseStack.pushPose();
        for (BedrockPart part : path) {
            part.translateAndRotateAndScale(poseStack);
        }
        Vector3f result = FirstPersonRenderMath.viewPosition(RenderSystem.getModelViewMatrix(), poseStack.last().pose());
        poseStack.popPose();
        return result;
    }

    private void renderTempPart(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType,
                                int light, int overlay, @Nonnull List<BedrockPart> path, MultiBufferSource bufferSource) {
        renderTempPart(poseStack, transformType, renderType, light, overlay, path, 1.0F, bufferSource);
    }

    private void renderTempPart(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType,
                                int light, int overlay, @Nonnull List<BedrockPart> path, float alpha, MultiBufferSource bufferSource) {
        poseStack.pushPose();
        for (int i = 0; i < path.size() - 1; ++i) {
            path.get(i).translateAndRotateAndScale(poseStack);
        }
        BedrockPart part = path.get(path.size() - 1);
        part.visible = true;
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        part.render(poseStack, transformType, vertexConsumer, light, overlay, 1.0F, 1.0F, 1.0F, alpha, bufferSource);
        if (!IrisCompat.endBatch(bufferSource)) {
            RenderHelper.endBatch(bufferSource, renderType);
        }
        part.visible = false;
        poseStack.popPose();
    }

    private void renderOcularStencil(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, boolean isScope, MultiBufferSource bufferSource) {
        if (!ocularNodePaths.isEmpty()) {
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(false);
            RenderSystem.stencilMask(0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            // 绘制目镜
            for (int i = ocularNodePaths.size() - 1; i >= 0; i--) {
                if (isScope == isScopeOcular.get(i)) {
                    RenderSystem.stencilFunc(GL11.GL_GREATER, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i), bufferSource);
                }
            }
            // 恢复渲染状态
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    private void renderDivisionOnly(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource) {
        if (!divisionNodePaths.isEmpty()) {
            RenderSystem.disableDepthTest();
            for (int i = 0; i < divisionNodePaths.size(); i++) {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i), bufferSource);
            }
            RenderSystem.enableDepthTest();
        }
    }

    private void renderOcularAndDivision(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, boolean selective, MultiBufferSource bufferSource) {
        if (!ocularNodePaths.isEmpty()) {
            // 准备渲染圆形模板层
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT);
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(false);
            // 80是一个随便找的大小合适的数值。
            float rad = 80 * scopeViewRadiusModifier;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                rad *= IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            }
            // 圆形在视图空间生成；提交前抵消 shader 稍后施加的 ModelView。
            Matrix4f viewToRender = FirstPersonRenderMath.viewToRender(RenderSystem.getModelViewMatrix());
            for (int i = 0; i < ocularNodePaths.size(); i++) {
                if (selective && !isScopeOcular.get(i)) {
                    continue;
                }
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                Vector3f ocularCenter = getBedrockPartCenter(matrixStack, ocularNodePaths.get(i));
                float centerX = ocularCenter.x() * 16 * 90;
                float centerY = ocularCenter.y() * 16 * 90;
                BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                builder.addVertex(viewToRender, centerX, centerY, -90.0F).setColor(255, 255, 255, 255);
                for (int j = 0; j <= 90; j++) {
                    float angle = (float) j * ((float) Math.PI * 2F) / 90.0F;
                    float sin = Mth.sin(angle);
                    float cos = Mth.cos(angle);
                    builder.addVertex(viewToRender, centerX + cos * rad, centerY + sin * rad, -90.0F).setColor(255, 255, 255, 255);
                }
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            for (int i = 0; i < ocularNodePaths.size() && i < divisionNodePaths.size(); i++) {
                if (i > Byte.MAX_VALUE) {
                    throw new IllegalArgumentException("Index of oculus is out of range for 127");
                }
                if (selective && !isScopeOcular.get(i)) {
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i), bufferSource);
                } else {
                    // 渲染目镜黑色遮罩
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i), bufferSource);
                    // 渲染划分
                    int b = ~(i+1) & 0xFF;
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, b, 0xFF);
                    Vector3f center = getBedrockPartCenter(matrixStack, ocularNodePaths.get(i));
                    List<BedrockPart> ocular = ocularNodePaths.get(i);
                    List<BedrockPart> division = divisionNodePaths.get(i);
                    if (ScopeLensRenderer.capture(center.x() * 16 * 90, center.y() * 16 * 90, rad, b, scopeLens,
                            bounds -> captureLensBounds(matrixStack, ocular, bounds),
                            buffers -> captureLensDivision(matrixStack, transformType, renderType, light, overlay, division, buffers))) {
                        continue;
                    }
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i), bufferSource);
                }
            }
        }
    }

    private void captureLensBounds(PoseStack poseStack, List<BedrockPart> path, VertexConsumer bounds) {
        poseStack.pushPose();
        try {
            for (int i = 0; i < path.size() - 1; i++) path.get(i).translateAndRotateAndScale(poseStack);
            captureLensPartBounds(poseStack, path.get(path.size() - 1), bounds);
        } finally {
            poseStack.popPose();
        }
    }

    /** Inspect geometry only; functional renderers must not run a second time for the bounds probe. */
    private void captureLensPartBounds(PoseStack poseStack, BedrockPart part, VertexConsumer bounds) {
        poseStack.pushPose();
        part.translateAndRotateAndScale(poseStack);
        for (BedrockCube cube : part.cubes) cube.compile(poseStack.last(), bounds, 0, 0, 1, 1, 1, 1);
        for (BedrockPart child : part.children) captureLensPartBounds(poseStack, child, bounds);
        poseStack.popPose();
    }

    /** Resolve functional readouts now, before the shared model is animated/reset for another draw. */
    private void captureLensDivision(PoseStack poseStack, ItemDisplayContext context, RenderType type,
                                     int light, int overlay, List<BedrockPart> division, MultiBufferSource buffers) {
        List<IFunctionalRenderer> previous = delegateRenderers;
        delegateRenderers = new ArrayList<>();
        try {
            renderTempPart(poseStack, context, type, light, overlay, division, buffers);
            for (int i = 0; i < delegateRenderers.size(); i++) {
                delegateRenderers.get(i).render(poseStack, buffers.getBuffer(type), context, light, overlay, buffers);
            }
        } finally {
            delegateRenderers = previous;
        }
    }

    private void renderBoth(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource) {
		// 当加速渲染加载则使用加速渲染提供的方法进行加速
		if (ARCompat.shouldAccelerate()) {
			renderBothAccelerated(matrixStack, transformType, renderType, light, overlay, bufferSource);
			return;
		}

        RenderHelper.enableItemEntityStencilTest();
        // 清空模板缓冲区、准备绘制模板缓冲
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        if (ocularRingPath != null) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            // 渲染目镜外环
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath, bufferSource);
        }
        // 渲染目镜以写入模板桓冲值 (暂时只渲染 ocular_scope)
        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, true, bufferSource);
        // 渲染镜身
        if (scopeBodyPath != null) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, bufferSource);
        }
        // 渲染目镜以写入模板桓冲值 (渲染其他的目镜)
        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, false, bufferSource);
        // 渲染目镜遮罩和划分
        renderOcularAndDivision(matrixStack, transformType, renderType, light, overlay, true, bufferSource);
        // 关闭模板缓冲
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        // 渲染其他部分
        super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);
    }

    private void renderSight(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource) {
		if (ARCompat.shouldAccelerate()) {
			renderSightAccelerated(matrixStack, transformType, renderType, light, overlay, bufferSource);
			return;
		}

        RenderHelper.enableItemEntityStencilTest();
        // 清空模板缓冲区、准备绘制模板缓冲
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        // 渲染目镜以写入模板桓冲值
        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, false, bufferSource);
        // 渲染划分
        renderDivisionOnly(matrixStack, transformType, renderType, light, overlay, bufferSource);
        // 关闭模板缓冲
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        // 渲染其他部分
        if (scopeBodyPath != null) {
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, bufferSource);
        }
        super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);
    }

    private void renderScope(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource) {
		if (ARCompat.shouldAccelerate()) {
			renderScopeAccelerated(matrixStack, transformType, renderType, light, overlay, bufferSource);
			return;
		}

        RenderHelper.enableItemEntityStencilTest();
        // 清空模板缓冲区、准备绘制模板缓冲
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        // 渲染目镜外环
        if (ocularRingPath != null) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath, bufferSource);
        }
        // 渲染目镜以写入模板桓冲值
        renderOcularStencil(matrixStack, transformType, renderType, light, overlay, false, bufferSource);
        // 渲染镜身
        if (scopeBodyPath != null) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, bufferSource);
        }
        // 渲染目镜遮罩和划分
        renderOcularAndDivision(matrixStack, transformType, renderType, light, overlay, false, bufferSource);
        // 关闭模板缓冲
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        // 渲染其他部分
        super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);
    }

	public void renderBothAccelerated(
			PoseStack matrixStack,
			ItemDisplayContext transformType,
			RenderType renderType,
			int light,
			int overlay
	) {
		renderBothAccelerated(matrixStack, transformType, renderType, light, overlay, RenderHelper.getBufferSource(transformType));
	}

	public void renderBothAccelerated(
			PoseStack matrixStack,
			ItemDisplayContext transformType,
			RenderType renderType,
			int light,
			int overlay,
			MultiBufferSource bufferSource
	) {
		// 缓存PoseStack防止之后坐标变换出现问题
		var poseStack = new PoseStack();
		poseStack.last().pose().set(matrixStack.last().pose());
		poseStack.last().normal().set(matrixStack.last().normal());

		// 获取先前的层前/层后任务, 防止覆盖
		Runnable before = ARCompat.getRenderBeforeFunction();
		Runnable after = ARCompat.getRenderAfterFunction();

		// 设置外环的渲染层和渲染前后任务
		// 清空模板缓冲区、准备绘制模板缓冲
		ARCompat.setRenderLayer(-943);
		ARCompat.setRenderBeforeFunction(() -> {
			// 先执行被覆盖的层前方法
			before.run();

			// 清除上模板残留, 这里其实可以按原始渲染方法移出去, 但为了统一先放入第一层渲染
			RenderHelper.enableItemEntityStencilTest();
			RenderSystem.clearStencil(0);
			RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

			// 渲染目镜外环所需的模板函数
			RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
			RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		});

		ARCompat.setRenderAfterFunction(() -> {
			// 画完目镜外环后, 临时关闭模板缓冲区防止渲染异常
			RenderHelper.disableItemEntityStencilTest();

			// 最后执行被覆盖的层后方法
			after.run();
		});

		if (ocularRingPath != null) {
			// 渲染目镜外环
			renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath, bufferSource);
		}

		// 重置外环层, 还原现场, 进行下一步绘制
		ARCompat.resetRenderLayer();
		ARCompat.resetRenderBeforeFunction();
		ARCompat.resetRenderAfterFunction();

		// 设置镜身的渲染层和渲染前后任务
		ARCompat.setRenderLayer(-943 + 1);
		ARCompat.setRenderBeforeFunction(() -> {
			// 先执行被覆盖的层前方法
			before.run();

			// 重新启用上一层关闭的模板缓冲区
			RenderHelper.enableItemEntityStencilTest();

			// 我们在层中进行渲染, 需要关闭加速, 否则这里的渲染会被导向到加速管线中, 造成被延后和内存泄漏
			ARCompat.disableAcceleration();

			// 在层间操作已经绑定了VAO, Minecraft的标准绘制方法会改变全局VAO状态, 因此需要缓存VAO
			int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
			// 并且需要Invalidate一下原版缓存的VAO绑定, 防止他直接认为缓存还有效导致绑定错误的VAO
			BufferUploader.invalidate();

			// 渲染目镜以写入模板桓冲值 (暂时只渲染 ocular_scope)
			renderOcularStencil(poseStack, transformType, renderType, light, overlay, true, bufferSource);

			// 重新绑回VAO
			GL30.glBindVertexArray(vao);

			// 画完了, 重新开启加速
			ARCompat.resetAcceleration();

			// 设置镜身需要的模板函数
			RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
		});

		// 画完镜身后, 渲染其他目镜及遮罩并关闭模板缓冲
		ARCompat.setRenderAfterFunction(() -> {
			// 我们在层中进行渲染, 需要关闭加速, 否则这里的渲染会被导向到加速管线中, 造成被延后和内存泄漏
			ARCompat.disableAcceleration();

			// 在层间操作已经绑定了VAO, Minecraft的标准绘制方法会改变全局VAO状态, 因此需要缓存VAO
			int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
			// 并且需要Invalidate一下原版缓存的VAO绑定, 防止他直接认为缓存还有效导致绑定错误的VAO
			BufferUploader.invalidate();

			// 渲染目镜以写入模板桓冲值 (渲染其他的目镜)
			renderOcularStencil(poseStack, transformType, renderType, light, overlay, false, bufferSource);
			// 渲染目镜遮罩和划分
			renderOcularAndDivision(poseStack, transformType, renderType, light, overlay, true, bufferSource);

			// 重新绑回VAO
			GL30.glBindVertexArray(vao);

			// 画完了, 重新开启加速
			ARCompat.resetAcceleration();

			// 关闭模板缓冲
			RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
			RenderHelper.disableItemEntityStencilTest();

			// 最后执行被覆盖的层后方法
			after.run();
		});

		if (scopeBodyPath != null) {
			// 渲染镜身
			renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, bufferSource);
		}

		// 重置镜身层, 还原现场, 进行最后一步其他部分绘制
		ARCompat.resetRenderLayer();
		ARCompat.resetRenderBeforeFunction();
		ARCompat.resetRenderAfterFunction();

		// 设置其他的渲染层, 保证其在最后一部分渲染
		ARCompat.setRenderLayer(-943 + 2);

		// 渲染其他部分
		super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);

		// 重置层, 还原现场, 完成配件渲染
		ARCompat.resetRenderLayer();
	}

	private void renderSightAccelerated(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource) {
		// 缓存PoseStack防止之后坐标变换出现问题
		var poseStack = new PoseStack();
		poseStack.last().pose().set(matrixStack.last().pose());
		poseStack.last().normal().set(matrixStack.last().normal());

		// 获取先前的层前任务, 防止覆盖
		Runnable before = ARCompat.getRenderBeforeFunction();

		// 设置层前任务, 清除模板缓冲并绘制目镜和划分
		ARCompat.setRenderLayer(-943);
		ARCompat.setRenderBeforeFunction(() -> {
			// 先执行被覆盖的层前方法
			before.run();

			// 清除上模板残留, 这里其实可以按原始渲染方法移出去, 但为了统一先放入第一层渲染
			RenderHelper.enableItemEntityStencilTest();
			RenderSystem.clearStencil(0);
			RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

			// 我们在层中进行渲染, 需要关闭加速, 否则这里的渲染会被导向到加速管线中, 造成被延后和内存泄漏
			ARCompat.disableAcceleration();

			// 在层间操作已经绑定了VAO, Minecraft的标准绘制方法会改变全局VAO状态, 因此需要缓存VAO
			int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
			// 并且需要Invalidate一下原版缓存的VAO绑定, 防止他直接认为缓存还有效导致绑定错误的VAO
			BufferUploader.invalidate();

			// 渲染目镜以写入模板桓冲值
			renderOcularStencil(poseStack, transformType, renderType, light, overlay, false, bufferSource);
			// 渲染划分
			renderDivisionOnly(poseStack, transformType, renderType, light, overlay, bufferSource);

			// 重新绑回VAO
			GL30.glBindVertexArray(vao);

			// 画完了, 重新开启加速
			ARCompat.resetAcceleration();

			// 关闭模板缓冲
			RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
			RenderHelper.disableItemEntityStencilTest();
		});

		// 渲染其他部分
		if (scopeBodyPath != null) {
			renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, bufferSource);
		}
		super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);

		// 重置层, 还原现场, 完成渲染
		ARCompat.resetRenderLayer();
		ARCompat.resetRenderBeforeFunction();
	}

	private void renderScopeAccelerated(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource) {
		// 缓存PoseStack防止之后坐标变换出现问题
		var poseStack = new PoseStack();
		poseStack.last().pose().set(matrixStack.last().pose());
		poseStack.last().normal().set(matrixStack.last().normal());

		// 获取先前的层前/层后任务, 防止覆盖
		Runnable before = ARCompat.getRenderBeforeFunction();
		Runnable after = ARCompat.getRenderAfterFunction();

		// 设置外环的渲染层和渲染前后任务
		// 清空模板缓冲区、准备绘制模板缓冲
		ARCompat.setRenderLayer(-943);
		ARCompat.setRenderBeforeFunction(() -> {
			// 先执行被覆盖的层前方法
			before.run();

			// 清除上模板残留, 这里其实可以按原始渲染方法移出去, 但为了统一先放入第一层渲染
			RenderHelper.enableItemEntityStencilTest();
			RenderSystem.clearStencil(0);
			RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

			// 渲染目镜外环所需的模板函数
			RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
			RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		});

		ARCompat.setRenderAfterFunction(() -> {
			// 画完目镜外环后, 临时关闭模板缓冲区防止渲染异常
			RenderHelper.disableItemEntityStencilTest();

			// 最后执行被覆盖的层后方法
			after.run();
		});

		// 渲染目镜外环
		if (ocularRingPath != null) {
			renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath, bufferSource);
		}

		// 重置外环层, 还原现场, 进行下一步绘制
		ARCompat.resetRenderLayer();
		ARCompat.resetRenderBeforeFunction();
		ARCompat.resetRenderAfterFunction();

		// 设置镜身的渲染层和渲染前后任务
		ARCompat.setRenderLayer(-943 + 1);
		ARCompat.setRenderBeforeFunction(() -> {
			// 先执行被覆盖的层前方法
			before.run();

			// 重新启用上一层关闭的模板缓冲区
			RenderHelper.enableItemEntityStencilTest();

			// 我们在层中进行渲染, 需要关闭加速, 否则这里的渲染会被导向到加速管线中, 造成被延后和内存泄漏
			ARCompat.disableAcceleration();

			// 在层间操作已经绑定了VAO, Minecraft的标准绘制方法会改变全局VAO状态, 因此需要缓存VAO
			int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
			// 并且需要Invalidate一下原版缓存的VAO绑定, 防止他直接认为缓存还有效导致绑定错误的VAO
			BufferUploader.invalidate();

			// 渲染目镜以写入模板桓冲值
			renderOcularStencil(poseStack, transformType, renderType, light, overlay, false, bufferSource);

			// 重新绑回VAO
			GL30.glBindVertexArray(vao);

			// 画完了, 重新开启加速
			ARCompat.resetAcceleration();

			// 设置镜身需要的模板函数
			RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
		});

		// 画完镜身后, 渲染其他目镜及遮罩并关闭模板缓冲
		ARCompat.setRenderAfterFunction(() -> {
			// 我们在层中进行渲染, 需要关闭加速, 否则这里的渲染会被导向到加速管线中, 造成被延后和内存泄漏
			ARCompat.disableAcceleration();

			// 在层间操作已经绑定了VAO, Minecraft的标准绘制方法会改变全局VAO状态, 因此需要缓存VAO
			int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
			// 并且需要Invalidate一下原版缓存的VAO绑定, 防止他直接认为缓存还有效导致绑定错误的VAO
			BufferUploader.invalidate();

			// 渲染目镜遮罩和划分
			renderOcularAndDivision(poseStack, transformType, renderType, light, overlay, false, bufferSource);

			// 重新绑回VAO
			GL30.glBindVertexArray(vao);

			// 画完了, 重新开启加速
			ARCompat.resetAcceleration();

			// 关闭模板缓冲
			RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
			RenderHelper.disableItemEntityStencilTest();

			// 最后执行被覆盖的层后方法
			after.run();
		});

		// 渲染镜身
		if (scopeBodyPath != null) {
			renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, bufferSource);
		}

		// 重置镜身层, 还原现场, 进行最后一步其他部分绘制
		ARCompat.resetRenderLayer();
		ARCompat.resetRenderBeforeFunction();
		ARCompat.resetRenderAfterFunction();

		// 设置其他的渲染层, 保证其在最后一部分渲染
		ARCompat.setRenderLayer(-943 + 2);

		// 渲染其他部分
		super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);

		// 重置层, 还原现场, 完成配件渲染
		ARCompat.resetRenderLayer();
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
