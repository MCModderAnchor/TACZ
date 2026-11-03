package com.tacz.guns.compat.playeranimator.animation;

import dev.kosmx.playerAnim.core.impl.AnimationProcessor;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.core.util.SetableSupplier;
import dev.kosmx.playerAnim.impl.IMutableModel;
import dev.kosmx.playerAnim.impl.animation.IBendHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.IdentityHashMap;
import java.util.Map;

public final class PlayerAnimatorModelState {
    private PlayerAnimatorModelState() {
    }

    public static Runnable capture(PlayerModel<?> model) {
        if (!(model instanceof IMutableModel mutable)) {
            return () -> {};
        }
        SetableSupplier<AnimationProcessor> supplier = mutable.getEmoteSupplier();
        AnimationProcessor processor = supplier.get();
        Map<ModelPart, Pair<Float, Float>> bends = new IdentityHashMap<>();
        captureBend(bends, processor, "torso", model.body, model.jacket);
        captureBend(bends, processor, "leftArm", model.leftArm, model.leftSleeve);
        captureBend(bends, processor, "rightArm", model.rightArm, model.rightSleeve);
        captureBend(bends, processor, "leftLeg", model.leftLeg, model.leftPants);
        captureBend(bends, processor, "rightLeg", model.rightLeg, model.rightPants);
        return () -> {
            supplier.set(processor);
            mutable.setEmoteSupplier(supplier);
            bends.forEach((part, bend) -> IBendHelper.INSTANCE.bend(part, bend));
        };
    }

    private static void captureBend(Map<ModelPart, Pair<Float, Float>> bends, AnimationProcessor processor,
                                    String name, ModelPart... parts) {
        Pair<Float, Float> bend = processor == null ? null : processor.getBend(name);
        if (processor != null && "torso".equals(name)) {
            // Match AnimationApplier.updatePart: the torso also includes the body bend.
            Pair<Float, Float> bodyBend = processor.getBend("body");
            bend = new Pair<>(bend.getLeft() + bodyBend.getLeft(), bend.getRight() + bodyBend.getRight());
        }
        for (ModelPart part : parts) {
            bends.put(part, bend);
        }
    }
}
