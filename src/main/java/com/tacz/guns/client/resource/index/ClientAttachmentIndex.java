package com.tacz.guns.client.resource.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentLod;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.util.ColorHex;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

public class ClientAttachmentIndex {
    private final Map<ResourceLocation, ClientAttachmentSkinIndex> skinIndexMap = Maps.newHashMap();
    private String name;
    private @Nullable BedrockAttachmentModel attachmentModel;
    private @Nullable ResourceLocation modelTexture;
    private @Nullable Pair<BedrockAttachmentModel, ResourceLocation> lodModel;
    private ResourceLocation slotTexture;
    private AttachmentData data;
    private float[] viewsFov;
    private float @Nullable [] zoom;
    private int [] views;
    private boolean isScope;
    private boolean isSight;
    private boolean showMuzzle;
    private @Nullable String adapterNodeName;
    private @Nullable String tooltipKey;
    private Map<String, ResourceLocation> sounds;
    private @Nullable LaserConfig laserConfig;

    private ClientAttachmentIndex() {
    }

    public static ClientAttachmentIndex getInstance(ResourceLocation registryName, AttachmentIndexPOJO indexPOJO) throws IllegalArgumentException {
        ClientAttachmentIndex index = new ClientAttachmentIndex();
        checkIndex(indexPOJO, index);
        AttachmentDisplay display = checkDisplay(indexPOJO, index);
        checkData(indexPOJO, index);
        checkName(indexPOJO, index);
        checkSlotTexture(display, index);
        checkTextureAndModel(display, index);
        checkTextShow(display, index.attachmentModel);
        checkLod(display, index);
//        checkSkins(registryName, index);
        checkSounds(display, index);
        return index;
    }

    private static void checkIndex(AttachmentIndexPOJO attachmentIndexPOJO, ClientAttachmentIndex index) {
        Preconditions.checkArgument(attachmentIndexPOJO != null, "index object file is empty");
        index.tooltipKey = attachmentIndexPOJO.getTooltip();
    }

    @Nonnull
    private static AttachmentDisplay checkDisplay(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        ResourceLocation pojoDisplay = indexPOJO.getDisplay();
        Preconditions.checkArgument(pojoDisplay != null, "index object missing display field");
        AttachmentDisplay display = ClientAssetsManager.INSTANCE.getAttachmentDisplay(pojoDisplay);
        Preconditions.checkArgument(display != null, "there is no corresponding display file");

        /**
         * 优先使用"views_fov"列表，缺失则使用"fov"
         * 各项需大于0F
         * {@link com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay}
         */
        index.viewsFov = display.getViewsFov();
        if (index.viewsFov != null) {
            for(float fov : index.viewsFov) {
                Preconditions.checkArgument(fov > 0, "fov must > 0");
            }
        } else {
            Preconditions.checkArgument(display.getFov() > 0, "fov must > 0");
            index.viewsFov = new float[]{display.getFov()};
        }

        /**
         * "zoom"列表
         * 各项需不小于1.0F
         * {@link com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay}
         */
        index.zoom = display.getZoom();
        if (index.zoom != null) {
            for (int i = 0; i < index.zoom.length; i++) {
                if (index.zoom[i] < 1) {
                    throw new IllegalArgumentException("zoom must >= 1");
                }
            }
        }

        /**
         * "views"int列表，默认为"views": [1]
         * 各项需不小于1
         * {@link com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay}
         *
         * 仅在1处使用{@link com.tacz.guns.client.event.FirstPersonRenderGunEvent#applyFirstPersonPositioningTransform}
         * "views"int列表各项使用为viewIndex = views[zoomNumber % views.length] - 1;
         * zoomNumber在没tag的时候返回0，第一次调用实际值为0，暂不清楚先zoom还是先读取zoomNumber
         * 每次按切换瞄准镜倍率，zoomNumber都加1
         * zoomNumber第一次zoom后实际使用值为1，且仅在一处被设置{@link com.tacz.guns.entity.shooter.LivingEntityAim#zoom}
         * 既然索引减1，那就表明能处理viewIndex为-1的情况
         * viewIndex是先获取views里的int，再减1，而views里的int已经在此处 if (index.views[i] < 1) throw new IllegalArgumentException("view index must >= 1");
         * 所以实际viewIndex已经在配置文件的检查里保证了最小为0（不会让List抛异常）
         * List<BedrockPart> scopeViewPath = attachmentModel.getScopeViewPath(currentViewIndex == -1 ? viewIndex : currentViewIndex); {@link com.tacz.guns.client.model.BedrockAttachmentModel#getScopeViewPath}
         * return scopeViewPaths.get(viewIndex >= scopeViewPaths.size() ? 0 : viewIndex);
         *
         * 从模型读取分组"scope_view"下读取 {@link com.tacz.guns.client.model.BedrockAttachmentModel#BedrockAttachmentModel}
         * "views"int列表内的值对应模型分组scope_view，scope_view2，scope_view3...
         * 1 -> scope_view
         * 2 -> scope_view2
         * 3 -> scope_view3
         * List<List<BedrockPart>> scopeViewPaths = {scope_view, scope_view2, scope_view3... }
         */
        index.views = display.getViews();
        if (index.views != null) {
            for (int i = 0; i < index.views.length; i++) {
                /**
                 * 此处不小于1的检查为{@link com.tacz.guns.client.model.BedrockAttachmentModel#getScopeViewPath}的隐式依赖
                 */
                if (index.views[i] < 1) {
                    throw new IllegalArgumentException("view index must >= 1");
                }
            }
        } else {
            index.views = new int[]{1};
        }

        /**
         * "scope"和"sight"形成一个组合{@link com.tacz.guns.client.model.BedrockGunModel#render(PoseStack matrixStack, ItemStack gunItem, ItemDisplayContext transformType, RenderType renderType, int light, int overlay)}
         * {@link com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay}
         *
         * {@link com.tacz.guns.client.model.BedrockAttachmentModel#BedrockAttachmentModel}
         * 准心division
             * 1 -> division
             * 2 -> division_2
             * 3 -> division_3
         * 高倍镜本体组名: scope_body
         * 高倍镜视野前的环组名: ocular_ring
         * 镜片组名: 以 ocular 或 ocular_sight 或 ocular_scope开头，后面可带下划线+数字
             * 读取完的结果自动按后缀排序
             * ocular 或 ocular_1 只能存在其一，否则会冲突
             * ocular_2
             * ocular_sight_3
             * ocular_scope_n
             * ocular，ocular_sight，ocular_scope共享后缀的排序，例如ocular_sight_2和ocular_scope_2只能用其一
             * ocular和ocular_sight无区别
             * ocular_scope影响此处分支 {@link BedrockAttachmentModel#renderOcularAndDivision}
         * ocular和division分组的数量需小于128 {@link com.tacz.guns.client.model.BedrockAttachmentModel#renderOcularAndDivision}
         */
        index.isScope = display.isScope();
        index.isSight = display.isSight();

        /**
         * "adapter"字符串，装备配件后显示该名称分组下的模型
         * {@link com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay}
         */
        index.adapterNodeName = display.getAdapterNodeName();

        /**
         * "show_muzzle"使装备枪口后不隐藏分组"muzzle_default"
         * {@link com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay}
         */
        index.showMuzzle = display.isShowMuzzle();

        /**
         * "laser"JsonObject，激光瞄准器参数，不提供着弹点预测的同时还默认提供PUBG特色“消音器穿墙”，废物激光剑
         * "default_color"
         * "can_edit"
         * "length"
         * "width"
         * "third_person_length"
         * "third_person_width"
         * {@link com.tacz.guns.client.resource.pojo.display.LaserConfig}
         *
         * {@link com.tacz.guns.client.model.BedrockAttachmentModel#BedrockAttachmentModel}
         * 激光剑组名：以laser_beam开头，后面可带下划线+数字
         * 读取完的结果不自动排序，且顺序不定且无关紧要，实际会使用相同配置全部渲染
         * 带后缀和不带后缀的可同时存在，且可重复
         */
        index.laserConfig = display.getLaserConfig();
        return display;
    }

    private static void checkTextShow(AttachmentDisplay display, BedrockAttachmentModel model) {
        if (model != null) {
            Map<String, TextShow> textShowMap = Maps.newHashMap();
            display.getTextShows().forEach((key, textShow) -> {
                if (StringUtils.isNoneBlank(key)) {
                    int color = ColorHex.colorTextToRbgInt(textShow.getColorText());
                    textShow.setColorInt(color);
                    textShowMap.put(key, textShow);
                }
            });
            model.setTextShowList(textShowMap);
        }
    }

    private static void checkData(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        ResourceLocation dataId = indexPOJO.getData();
        Preconditions.checkArgument(dataId != null, "index object missing pojoData field");
        AttachmentData data = CommonAssetsManager.get().getAttachmentData(dataId);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        // 剩下的不需要校验了，Common的读取逻辑中已经校验过了
        index.data = data;
    }

    private static void checkName(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        index.name = indexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tacz.error.no_name";
        }
    }

    private static void checkSlotTexture(AttachmentDisplay display, ClientAttachmentIndex index) {
        index.slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    private static void checkTextureAndModel(AttachmentDisplay display, ClientAttachmentIndex index) {
        // 不检查模型/材质是否为 null，模型/材质可以为 null
        index.attachmentModel = getOrLoadAttachmentModel(display.getModel());
        if (index.attachmentModel != null) {
            index.attachmentModel.setIsScope(display.isScope());
            index.attachmentModel.setIsSight(display.isSight());
        }
        index.modelTexture = display.getTexture();
    }

    @Nullable
    public static BedrockAttachmentModel getOrLoadAttachmentModel(@Nullable ResourceLocation modelLocation) {
        if (modelLocation == null) {
            return null;
        }
        BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(modelLocation);
        if (modelPOJO == null) {
            return null;
        }
        return getAttachmentModel(modelPOJO);
    }

    @Nullable
    public static BedrockAttachmentModel getAttachmentModel(BedrockModelPOJO modelPOJO) {
        BedrockAttachmentModel attachmentModel = null;
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
            attachmentModel = new BedrockAttachmentModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
            attachmentModel = new BedrockAttachmentModel(modelPOJO, BedrockVersion.NEW);
        }
        return attachmentModel;
    }

    private static void checkLod(AttachmentDisplay display, ClientAttachmentIndex index) {
        AttachmentLod gunLod = display.getAttachmentLod();
        if (gunLod != null) {
            ResourceLocation texture = gunLod.getModelTexture();
            if (gunLod.getModelLocation() == null) {
                return;
            }
            if (texture == null) {
                return;
            }
            BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(gunLod.getModelLocation());
            if (modelPOJO == null) {
                return;
            }
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
                BedrockAttachmentModel model = new BedrockAttachmentModel(modelPOJO, BedrockVersion.LEGACY);
                index.lodModel = Pair.of(model, texture);
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
                BedrockAttachmentModel model = new BedrockAttachmentModel(modelPOJO, BedrockVersion.NEW);
                index.lodModel = Pair.of(model, texture);
            }
        }
    }


    private static void checkSounds(AttachmentDisplay display, ClientAttachmentIndex index) {
        Map<String, ResourceLocation> displaySounds = display.getSounds();
        if (displaySounds == null) {
            index.sounds = Maps.newHashMap();
            return;
        }
        index.sounds = displaySounds;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getTooltipKey() {
        return tooltipKey;
    }

    @Nullable
    public BedrockAttachmentModel getAttachmentModel() {
        return attachmentModel;
    }

    @Nullable
    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    @Nullable
    public Pair<BedrockAttachmentModel, ResourceLocation> getLodModel() {
        return lodModel;
    }

    public ResourceLocation getSlotTexture() {
        return slotTexture;
    }

    public float[] getViewsFov() {
        return viewsFov;
    }

    public float @Nullable [] getZoom() {
        return zoom;
    }

    public int[] getViews() {
        return views;
    }

    public AttachmentData getData() {
        return data;
    }

    @Deprecated
    @Nullable
    public ClientAttachmentSkinIndex getSkinIndex(@Nullable ResourceLocation skinName) {
        return null;
    }

    public boolean isScope() {
        return isScope;
    }

    public boolean isSight() {
        return isSight;
    }

    @Nullable
    public String getAdapterNodeName() {
        return adapterNodeName;
    }

    public boolean isShowMuzzle() {
        return showMuzzle;
    }

    public Map<String, ResourceLocation> getSounds() {
        return sounds;
    }

    public @Nullable LaserConfig getLaserConfig() {
        return laserConfig;
    }
}
