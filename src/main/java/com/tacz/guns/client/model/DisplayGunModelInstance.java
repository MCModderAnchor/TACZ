package com.tacz.guns.client.model;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.tacz.guns.client.model.GunModelConstant.*;

/** 捕获阶段复用的模型状态；骨骼名称只在资源构造时解析一次。 */
public final class DisplayGunModelInstance extends BakedModelInstance {
    public enum Kind { GUN, ATTACHMENT }

    private static final AttachmentType[] TYPES = AttachmentType.values();

    private final BoneState bulletInBarrel;
    private final BoneState bulletInMag;
    private final BoneState bulletChain;
    private final BoneState mount;
    private final BoneState carry;
    private final BoneState sight;
    private final BoneState sightFolded;
    private final BoneState magStandard;
    private final BoneState magExtended1;
    private final BoneState magExtended2;
    private final BoneState magExtended3;
    private final BoneState handguardDefault;
    private final BoneState handguardTactical;
    private final BoneState[] attachmentDefaults = new BoneState[TYPES.length];
    private final int[] attachmentPositions = new int[TYPES.length];
    private final BoneState[] adapterChildren;

    public DisplayGunModelInstance(BakedBedrockModel model, Set<String> hiddenTextBones, Kind kind) {
        super(model);
        for (var definition : model.bones()) {
            String name = definition.name();
            if (name == null) continue;
            BoneState bone = getBone(definition.index());
            bone.illuminated = name.endsWith("_illuminated");
            if (hiddenTextBones.contains(name) || name.startsWith("laser_beam")
                    || kind == Kind.GUN && isHiddenGunBone(name)
                    || kind == Kind.ATTACHMENT && name.startsWith("division")) bone.visible = false;
        }

        bulletInBarrel = getBone(BULLET_IN_BARREL);
        bulletInMag = getBone(BULLET_IN_MAG);
        bulletChain = getBone(BULLET_CHAIN);
        mount = getBone(MOUNT);
        carry = getBone(CARRY);
        sight = getBone(SIGHT);
        sightFolded = getBone(SIGHT_FOLDED);
        magStandard = getBone(MAG_STANDARD);
        magExtended1 = getBone(MAG_EXTENDED_1);
        magExtended2 = getBone(MAG_EXTENDED_2);
        magExtended3 = getBone(MAG_EXTENDED_3);
        handguardDefault = getBone(HANDGUARD_DEFAULT_NODE);
        handguardTactical = getBone(HANDGUARD_TACTICAL_NODE);
        for (AttachmentType type : TYPES) {
            if (type == AttachmentType.NONE) continue;
            String prefix = type.name().toLowerCase(Locale.ROOT);
            int position = getIndex(prefix + ATTACHMENT_POS_SUFFIX);
            attachmentPositions[type.ordinal()] = position;
            attachmentDefaults[type.ordinal()] = getBone(prefix + DEFAULT_ATTACHMENT_SUFFIX);
            if (kind == Kind.GUN) setVisible(getBone(position), false);
        }
        BoneState adapter = getBone(ATTACHMENT_ADAPTER_NODE);
        if (adapter == null) {
            adapterChildren = new BoneState[0];
        } else {
            int[] children = adapter.children();
            adapterChildren = new BoneState[children.length];
            for (int i = 0; i < children.length; i++) adapterChildren[i] = getBone(children[i]);
        }
    }

    public int attachmentPosition(AttachmentType type) {
        return attachmentPositions[type.ordinal()];
    }

    public void configureGun(IGun gun, ItemStack stack, Map<AttachmentType, ClientAttachmentIndex> attachments) {
        ClientAttachmentIndex scope = attachments.get(AttachmentType.SCOPE);
        ClientAttachmentIndex magazine = attachments.get(AttachmentType.EXTENDED_MAG);
        boolean hasScope = scope != null;
        boolean tacticalHandguard = attachments.containsKey(AttachmentType.LASER)
                || attachments.containsKey(AttachmentType.GRIP);
        int magLevel = magazine == null ? 0 : magazine.getData().getExtendedMagLevel();
        boolean hasAmmo = gun.getCurrentAmmoCount(stack) > 0;

        setVisible(bulletInBarrel, gun.hasBulletInBarrel(stack));
        setVisible(bulletInMag, hasAmmo);
        setVisible(bulletChain, hasAmmo);
        setVisible(mount, scope != null && scope.isShowMount());
        setVisible(carry, !hasScope);
        setVisible(sight, !hasScope);
        setVisible(sightFolded, hasScope);
        setVisible(magStandard, magLevel == 0);
        setVisible(magExtended1, magLevel == 1);
        setVisible(magExtended2, magLevel == 2);
        setVisible(magExtended3, magLevel == 3);
        setVisible(handguardDefault, !tacticalHandguard);
        setVisible(handguardTactical, tacticalHandguard);

        for (AttachmentType type : TYPES) {
            if (type == AttachmentType.NONE) continue;
            ClientAttachmentIndex index = attachments.get(type);
            setVisible(attachmentDefaults[type.ordinal()],
                    index == null || type == AttachmentType.MUZZLE && index.isShowMuzzle());
        }
        for (BoneState child : adapterChildren) {
            if (child == null) continue;
            boolean visible = false;
            for (ClientAttachmentIndex index : attachments.values()) {
                if (child.name() != null && child.name().equals(index.getAdapterNodeName())) {
                    visible = true;
                    break;
                }
            }
            child.visible = visible;
        }
    }

    private static void setVisible(BoneState bone, boolean visible) {
        if (bone != null) bone.visible = visible;
    }

    private static boolean isHiddenGunBone(String name) {
        return name.equals(MAG_ADDITIONAL_NODE) || name.equals(MUZZLE_FLASH_ORIGIN_NODE)
                || name.equals(LEFTHAND_POS_NODE) || name.equals(RIGHTHAND_POS_NODE)
                || name.equals(SHELL_ORIGIN_NODE) || name.matches("shell_\\d+");
    }
}
