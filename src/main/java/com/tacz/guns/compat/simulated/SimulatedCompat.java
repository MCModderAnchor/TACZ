package com.tacz.guns.compat.simulated;

import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.compat.sable.AssemblyBlockPairs;
import dev.simulated_team.simulated.index.SimBlockMovementChecks;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class SimulatedCompat {
    private SimulatedCompat() {
    }

    public static void registerAssemblyChecks() {
        SimBlockMovementChecks.registerAdditionalBlocks((state, level, pos, visited) -> {
            if (state.getBlock() instanceof GunDisplayCaseBlock displayCase) {
                List<BlockPos> neighbors = displayCase.getConnectedNeighbors(level, pos, state);
                neighbors.removeIf(visited::contains);
                return neighbors;
            }
            BlockPos otherPos = AssemblyBlockPairs.getOtherHalf(level, pos, state);
            return otherPos != null && !visited.contains(otherPos) ? List.of(otherPos) : List.of();
        });
    }
}
