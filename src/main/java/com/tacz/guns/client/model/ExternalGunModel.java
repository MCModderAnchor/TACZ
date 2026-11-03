package com.tacz.guns.client.model;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BonesItem;
import com.tacz.guns.client.resource.pojo.model.CubesItem;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 外部视角的 SBM 模型资源。它从 TACZ 已解析的同一份枪包几何构造，
 * 不读取或渲染 BedrockGunModel 的可变骨骼树。
 */
public class ExternalGunModel {
    private static final Gson SOURCE_GSON = new GsonBuilder()
            .registerTypeAdapter(CubesItem.class, (JsonSerializer<CubesItem>) (cube, type, context) -> {
                JsonObject json = new JsonObject();
                if (cube.getUv() != null) {
                    json.add("uv", context.serialize(cube.getUv()));
                } else if (cube.getFaceUv() != null) {
                    json.add("uv", context.serialize(cube.getFaceUv()));
                }
                json.add("size", context.serialize(cube.getSize()));
                json.add("origin", context.serialize(cube.getOrigin()));
                if (cube.getRotation() != null) {
                    json.add("rotation", context.serialize(cube.getRotation()));
                }
                if (cube.getPivot() != null) {
                    json.add("pivot", context.serialize(cube.getPivot()));
                }
                json.addProperty("inflate", cube.getInflate());
                if (cube.isHasMirror()) {
                    json.addProperty("mirror", cube.isMirror());
                }
                return json;
            })
            .create();

    private final BakedBedrockModel model;
    private final DisplayGunModelInstance displayInstance;

    public ExternalGunModel(BakedBedrockModel model, Set<String> hiddenTextBones,
                             DisplayGunModelInstance.Kind kind) {
        this.model = model;
        this.displayInstance = new DisplayGunModelInstance(model, hiddenTextBones, kind);
    }

    /**
     * 由资源后台任务执行一次。保留全部命名骨骼，以便外观显隐、挂点和动画边界可查询。
     * 后续按测量结果收窄 preservedBones，合并确定不变的枪身部分。
     */
    public static ExternalGunModel bake(BedrockModelPOJO source, Set<String> hiddenTextBones,
                                        DisplayGunModelInstance.Kind kind) {
        Objects.requireNonNull(source, "source");
        var sbmSource = GsonUtil.CLIENT_GSON.fromJson(SOURCE_GSON.toJsonTree(source),
                com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO.class);
        if (sbmSource == null) {
            throw new IllegalArgumentException("Missing Bedrock model");
        }

        Set<String> preserved = new LinkedHashSet<>();
        BonesItem[] bones;
        if (source.getGeometryModelNew() != null) {
            var items = source.getGeometryModelNew().getBones();
            bones = items == null ? new BonesItem[0] : items.toArray(BonesItem[]::new);
        } else if (source.getGeometryModelLegacy() != null) {
            var items = source.getGeometryModelLegacy().getBones();
            bones = items == null ? new BonesItem[0] : items.toArray(BonesItem[]::new);
        } else {
            throw new IllegalArgumentException("No Bedrock geometry in gun model");
        }

        Arrays.stream(bones).map(BonesItem::getName).filter(Objects::nonNull)
                .forEach(preserved::add);

        var options = new BakerOptions(Set.of(), preserved, true, false);
        return new ExternalGunModel(BakedBedrockModel.bake(sbmSource, options), hiddenTextBones, kind);
    }

    public BakedBedrockModel model() {
        return model;
    }

    /** 捕获过程顺序执行，复用此模型的骨骼状态；重新加载资源时模型与实例一同替换。 */
    public DisplayGunModelInstance displayInstance() {
        return displayInstance;
    }
}
