package com.tacz.guns.util;

import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.compat.sable.SableCompat;
import com.tacz.guns.util.block.ProjectileExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;

public class ExplodeUtil {
    public static void createExplosion(Entity owner, Entity exploder, float damage, float radius, boolean knockback, boolean destroy, Vec3 hitPos) {
        // 半径无效时跳过爆炸，避免伤害和击退计算出现 NaN
        if (!Float.isFinite(radius) || radius <= 0) {
            return;
        }
        // 客户端不执行
        if (!(exploder.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 worldHitPos = SableCompat.toWorldPosition(level, hitPos);
        // 依据配置文件读取方块破坏方式
        Explosion.BlockInteraction mode;
        if (destroy) {
            mode = Explosion.BlockInteraction.DESTROY;
        } else {
            mode = Explosion.BlockInteraction.KEEP;
        }
        // 创建爆炸
        ProjectileExplosion explosion = new ProjectileExplosion(level, owner, exploder, null, null, worldHitPos.x(), worldHitPos.y(), worldHitPos.z(), damage, radius, knockback, mode);
        // 监听 forge 事件
        if (EventHooks.onExplosionStart(level, explosion)) {
            return;
        }
        // 执行爆炸逻辑
        explosion.explode();
        explosion.finalizeExplosion(true);
        if (mode == Explosion.BlockInteraction.KEEP) {
            explosion.clearToBlow();
        }
        // Plot blocks synchronize through block updates. The vanilla packet only encodes byte offsets
        // from the world-space explosion; sending plot positions would wrap onto unrelated world blocks.
        List<BlockPos> packetBlocks = getPacketBlocks(explosion.getToBlow(), worldHitPos);
        // 客户端发包，发送爆炸相关信息
        level.players().stream().filter(player -> Mth.sqrt((float) player.distanceToSqr(worldHitPos)) < AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE.get()).forEach(player -> {
            ClientboundExplodePacket packet = new ClientboundExplodePacket(worldHitPos.x(), worldHitPos.y(), worldHitPos.z(), radius, packetBlocks, explosion.getHitPlayers().get(player), mode, explosion.getSmallExplosionParticles(), explosion.getLargeExplosionParticles(), explosion.getExplosionSound());
            player.connection.send(packet);
        });
    }

    static List<BlockPos> getPacketBlocks(List<BlockPos> blocks, Vec3 position) {
        BlockPos origin = BlockPos.containing(position);
        return blocks.stream().filter(pos -> {
            long dx = (long) pos.getX() - origin.getX();
            long dy = (long) pos.getY() - origin.getY();
            long dz = (long) pos.getZ() - origin.getZ();
            return dx >= -128 && dx <= 127 && dy >= -128 && dy <= 127 && dz >= -128 && dz <= 127;
        }).toList();
    }
}
