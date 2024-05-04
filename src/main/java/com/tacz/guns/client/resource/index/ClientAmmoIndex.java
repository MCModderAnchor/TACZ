package com.tacz.guns.client.resource.index;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.loader.ShellDisplay;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoDisplay;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoEntityDisplay;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoTransform;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.resource.DefaultAssets;
import com.tacz.guns.resource.pojo.AmmoIndexPOJO;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientAmmoIndex {
    private static final Pattern COLOR_HEX = Pattern.compile("^#([0-9A-Fa-f]{6})$");

    private String name;
    private @Nullable BedrockAmmoModel ammoModel;
    private @Nullable ResourceLocation modelTextureLocation;
    private ResourceLocation slotTextureLocation;
    private @Nullable BedrockAmmoModel ammoEntityModel;
    private @Nullable ResourceLocation ammoEntityTextureLocation;
    private @Nullable BedrockAmmoModel shellModel;
    private @Nullable ResourceLocation shellTextureLocation;
    private int stackSize;
    private @Nullable AmmoParticle particle;
    private float[] tracerColor = new float[]{1f, 1f, 1f};
    private AmmoTransform transform;

    private ClientAmmoIndex() {
    }

    public static ClientAmmoIndex getInstance(AmmoIndexPOJO clientPojo) throws IllegalArgumentException {
        ClientAmmoIndex index = new ClientAmmoIndex();
        checkIndex(clientPojo, index);
        AmmoDisplay display = checkDisplay(clientPojo);
        checkName(clientPojo, index);
        checkTextureAndModel(display, index);
        checkSlotTexture(display, index);
        checkStackSize(clientPojo, index);
        checkAmmoEntity(display, index);
        checkShell(display, index);
        checkParticle(display, index);
        checkTracerColor(display, index);
        checkTransform(display, index);
        return index;
    }

    private static void checkIndex(AmmoIndexPOJO ammoIndexPOJO, ClientAmmoIndex index) {
        if (ammoIndexPOJO == null) {
            throw new IllegalArgumentException("index object file is empty");
        }
    }

    private static void checkName(AmmoIndexPOJO ammoIndexPOJO, ClientAmmoIndex index) {
        index.name = ammoIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tacz.error.no_name";
        }
    }

    @NotNull
    private static AmmoDisplay checkDisplay(AmmoIndexPOJO ammoIndexPOJO) {
        ResourceLocation pojoDisplay = ammoIndexPOJO.getDisplay();
        if (pojoDisplay == null) {
            throw new IllegalArgumentException("index object missing display field");
        }
        AmmoDisplay display = ClientAssetManager.INSTANCE.getAmmoDisplay(pojoDisplay);
        if (display == null) {
            throw new IllegalArgumentException("there is no corresponding display file");
        }
        return display;
    }

    private static void checkTextureAndModel(AmmoDisplay display, ClientAmmoIndex index) {
        // 检查模型
        ResourceLocation modelLocation = display.getModelLocation();
        if (modelLocation == null) {
            return;
        }
        BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(modelLocation);
        if (modelPOJO == null) {
            throw new IllegalArgumentException("there is no corresponding model file");
        }
        // 检查材质
        index.modelTextureLocation = display.getModelTexture();
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion()) && modelPOJO.getGeometryModelLegacy() != null) {
            index.ammoModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.NEW.getVersion()) && modelPOJO.getGeometryModelNew() != null) {
            index.ammoModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.NEW);
        }
    }

    private static void checkSlotTexture(AmmoDisplay display, ClientAmmoIndex index) {
        // 加载 GUI 内枪械图标
        index.slotTextureLocation = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    private static void checkAmmoEntity(AmmoDisplay display, ClientAmmoIndex index) {
        AmmoEntityDisplay ammoEntity = display.getAmmoEntity();
        if (ammoEntity != null && ammoEntity.getModelLocation() != null && ammoEntity.getModelTexture() != null) {
            index.ammoEntityTextureLocation = ammoEntity.getModelTexture();
            ResourceLocation modelLocation = ammoEntity.getModelLocation();
            BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(modelLocation);
            if (modelPOJO == null) {
                return;
            }
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (modelPOJO.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion()) && modelPOJO.getGeometryModelLegacy() != null) {
                index.ammoEntityModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.LEGACY);
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (modelPOJO.getFormatVersion().equals(BedrockVersion.NEW.getVersion()) && modelPOJO.getGeometryModelNew() != null) {
                index.ammoEntityModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.NEW);
            }
        }
    }

    private static void checkShell(AmmoDisplay display, ClientAmmoIndex index) {
        ShellDisplay shellDisplay = display.getShellDisplay();
        if (shellDisplay != null && shellDisplay.getModelLocation() != null && shellDisplay.getModelTexture() != null) {
            index.shellTextureLocation = shellDisplay.getModelTexture();
            ResourceLocation modelLocation = shellDisplay.getModelLocation();
            BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(modelLocation);
            if (modelPOJO == null) {
                return;
            }
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (modelPOJO.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion()) && modelPOJO.getGeometryModelLegacy() != null) {
                index.shellModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.LEGACY);
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (modelPOJO.getFormatVersion().equals(BedrockVersion.NEW.getVersion()) && modelPOJO.getGeometryModelNew() != null) {
                index.shellModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.NEW);
            }
        }
    }

    private static void checkParticle(AmmoDisplay display, ClientAmmoIndex index) {
        if (display.getParticle() != null) {
            try {
                AmmoParticle particle = display.getParticle();
                String name = particle.getName();
                if (StringUtils.isNoneBlank()) {
                    particle.setParticleOptions(ParticleArgument.readParticle(new StringReader(name)));
                    Preconditions.checkArgument(particle.getCount() > 0, "particle count must be greater than 0");
                    Preconditions.checkArgument(particle.getLifeTime() > 0, "particle life time must be greater than 0");
                    index.particle = particle;
                }
            } catch (CommandSyntaxException e) {
                e.fillInStackTrace();
            }
        }
    }

    private static void checkTracerColor(AmmoDisplay display, ClientAmmoIndex index) {
        String tracerColorText = display.getTracerColor();
        if (StringUtils.isNoneBlank(tracerColorText)) {
            Matcher matcher = COLOR_HEX.matcher(tracerColorText);
            if (!matcher.find()) {
                return;
            }
            int colorHex = Integer.parseInt(matcher.group(1), 16);
            float r = (colorHex >> 16 & 0xff) / 255.0F;
            float g = (colorHex >> 8 & 0xff) / 255.0F;
            float b = (colorHex & 0xff) / 255.0F;
            index.tracerColor = new float[]{r, g, b};
        }
    }

    private static void checkTransform(AmmoDisplay display, ClientAmmoIndex index) {
        AmmoTransform readTransform = display.getTransform();
        AmmoDisplay defaultDisplay = ClientAssetManager.INSTANCE.getAmmoDisplay(DefaultAssets.DEFAULT_AMMO_DISPLAY);
        if (readTransform == null || readTransform.getScale() == null) {
            index.transform = Objects.requireNonNull(defaultDisplay.getTransform());
        } else {
            index.transform = display.getTransform();
        }
    }

    private static void checkStackSize(AmmoIndexPOJO clientPojo, ClientAmmoIndex index) {
        index.stackSize = Math.max(clientPojo.getStackSize(), 1);
    }

    public String getName() {
        return name;
    }

    @Nullable
    public BedrockAmmoModel getAmmoModel() {
        return ammoModel;
    }

    @Nullable
    public ResourceLocation getModelTextureLocation() {
        return modelTextureLocation;
    }

    public ResourceLocation getSlotTextureLocation() {
        return slotTextureLocation;
    }

    public int getStackSize() {
        return stackSize;
    }

    @Nullable
    public BedrockAmmoModel getAmmoEntityModel() {
        return ammoEntityModel;
    }

    @Nullable
    public ResourceLocation getAmmoEntityTextureLocation() {
        return ammoEntityTextureLocation;
    }

    @Nullable
    public BedrockAmmoModel getShellModel() {
        return shellModel;
    }

    @Nullable
    public ResourceLocation getShellTextureLocation() {
        return shellTextureLocation;
    }

    @Nullable
    public AmmoParticle getParticle() {
        return particle;
    }

    public float[] getTracerColor() {
        return tracerColor;
    }

    public AmmoTransform getTransform() {
        return transform;
    }
}
