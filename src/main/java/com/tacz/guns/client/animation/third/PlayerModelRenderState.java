package com.tacz.guns.client.animation.third;

import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.HumanoidArm;

import java.util.List;
import java.util.stream.Stream;

/** Restore the shared player model after a vanilla or mod-provided hand draw. */
public final class PlayerModelRenderState implements AutoCloseable {
    private final PlayerModel<?> model;
    private final List<PartState> parts;
    private final float attackTime;
    private final float swimAmount;
    private final boolean crouching;
    private final boolean riding;
    private final boolean young;
    private final HumanoidModel.ArmPose leftArmPose;
    private final HumanoidModel.ArmPose rightArmPose;
    private final Runnable restoreAnimation;

    public PlayerModelRenderState(PlayerModel<?> model) {
        this.model = model;
        parts = Stream.of(model.head, model.hat, model.body, model.leftArm, model.rightArm,
                        model.leftLeg, model.rightLeg, model.jacket, model.leftSleeve, model.rightSleeve,
                        model.leftPants, model.rightPants)
                .flatMap(ModelPart::getAllParts).distinct().map(PartState::new).toList();
        attackTime = model.attackTime;
        swimAmount = model.swimAmount;
        crouching = model.crouching;
        riding = model.riding;
        young = model.young;
        leftArmPose = model.leftArmPose;
        rightArmPose = model.rightArmPose;
        restoreAnimation = PlayerAnimatorCompat.captureModelState(model);
    }

    public void prepareArm(HumanoidArm arm, boolean visible) {
        // A RenderArmEvent replacement can draw sleeve geometry before vanilla initializes it.
        // Vanilla's fallback still applies the player's skin-part settings in renderHand().
        if (arm == HumanoidArm.RIGHT) {
            model.rightArm.visible = visible;
            model.rightSleeve.visible = visible;
        } else {
            model.leftArm.visible = visible;
            model.leftSleeve.visible = visible;
        }
    }

    @Override
    public void close() {
        restoreAnimation.run();
        parts.forEach(PartState::restore);
        model.attackTime = attackTime;
        model.swimAmount = swimAmount;
        model.crouching = crouching;
        model.riding = riding;
        model.young = young;
        model.leftArmPose = leftArmPose;
        model.rightArmPose = rightArmPose;
    }

    private record PartState(ModelPart part, PartPose pose, float xScale, float yScale, float zScale,
                             boolean visible, boolean skipDraw) {
        private PartState(ModelPart part) {
            this(part, part.storePose(), part.xScale, part.yScale, part.zScale, part.visible, part.skipDraw);
        }

        private void restore() {
            part.loadPose(pose);
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
            part.visible = visible;
            part.skipDraw = skipDraw;
        }
    }
}
