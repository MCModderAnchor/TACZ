package com.tacz.guns.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import javax.annotation.Nullable;

final class SableClientCompat {
    private SableClientCompat() {
    }

    @Nullable
    static SableCompat.BulletHoleAttachment attachBulletHole(Level level, BlockPos pos, Vec3 position) {
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(pos);
        if (subLevel == null || subLevel.isRemoved()) {
            return null;
        }
        // Accept both Sable's local hits and the world-space hits sent by TaCZ's collision path.
        Vec3 localPosition = SableCompatImpl.toLocalPosition(level, pos, position);
        return new Attachment(subLevel, localPosition);
    }

    private record Attachment(ClientSubLevel subLevel, Vec3 localPosition) implements SableCompat.BulletHoleAttachment {
        @Override
        public Vec3 worldPosition() {
            return subLevel.logicalPose().transformPosition(localPosition);
        }

        @Override
        public SableCompat.BulletHolePose renderPose(Direction direction, float partialTick) {
            Pose3dc pose = subLevel.renderPose(partialTick);
            return new SableCompat.BulletHolePose(pose.transformPosition(localPosition),
                    pose.orientation().get(new Quaternionf()).mul(direction.getRotation()));
        }

        @Override
        public boolean isRemoved() {
            return subLevel.isRemoved();
        }
    }
}
