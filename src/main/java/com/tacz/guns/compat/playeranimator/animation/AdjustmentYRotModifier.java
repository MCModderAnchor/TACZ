package com.tacz.guns.compat.playeranimator.animation;

import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.Function;

public class AdjustmentYRotModifier implements Function<String, Optional<AdjustmentModifier.PartModifier>> {
    private final Player player;

    private AdjustmentYRotModifier(Player player) {
        this.player = player;
    }

    @Override
    public Optional<AdjustmentModifier.PartModifier> apply(String partName) {
        Minecraft mc = Minecraft.getInstance();
        if (player.equals(mc.player) && mc.screen != null) {
            return Optional.empty();
        }

        if (player.getVehicle() != null && "body".equals(partName)) {
            return Optional.empty();
        }

        float partialTick = mc.getPartialTick();
        float yBodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float yHeadRot = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float xRot = Mth.lerp(partialTick, player.xRotO, player.getXRot());

        float yaw = yHeadRot - yBodyRot;
        yaw = Mth.wrapDegrees(yaw);
        yaw = Mth.clamp(yaw, -85f, 85f);

        float pitch = Mth.wrapDegrees(xRot);

        return switch (partName) {
            case "body" -> {
                if (!player.isSwimming() && player.getPose() == Pose.SWIMMING) {
                    yield Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(0, 0, -yaw * Mth.DEG_TO_RAD), Vec3f.ZERO));
                }
                yield Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(0, -yaw * Mth.DEG_TO_RAD, 0), Vec3f.ZERO));
            }
            case "head", "leftArm", "rightArm" ->
                    Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(pitch * Mth.DEG_TO_RAD, 0, 0), Vec3f.ZERO));
            default -> Optional.empty();
        };
    }

    public static AdjustmentModifier getModifier(Player player) {
        return new AdjustmentModifier(new AdjustmentYRotModifier(player));
    }
}
