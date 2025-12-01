package net.arcana.friendsforlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

public class RaycastHelper {
    /**
     * Finds a safe BlockPos for an entity.
     * Tries the hit block, then adjacent blocks toward the player, then falls back
     * to player position.
     *
     */
    public static BlockPos getRaycastBlock(Player player) {
        double reach = player.isCreative() ? 5.0D : 4.5D;
        Level world = player.level();
        HitResult hit = player.pick(reach, 0.0F, false);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos hitPos = blockHit.getBlockPos();
            Direction face = blockHit.getDirection();

            // Start with the block in front of the hit face
            BlockPos start = hitPos.relative(face);

            // List of candidate positions: the block in front, then the cardinal neighbors
            List<BlockPos> candidates = new ArrayList<>();
            candidates.add(start);

            // Add the other horizontal neighbors around the hit block
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = hitPos.relative(dir);
                if (!candidate.equals(start)) {
                    candidates.add(candidate);
                }
            }

            // Check each candidate upwards for a safe block (air with solid block
            // underneath)
            int maxUpwards = 1;
            for (BlockPos candidate : candidates) {
                for (int i = 0; i <= maxUpwards; i++) {
                    BlockPos pos = candidate.above(i);
                    BlockPos below = pos.below();
                    if (world.isEmptyBlock(pos) && !world.isEmptyBlock(below)) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 
     * @param player
     * @return A safe BlockPos; defaults to player's feet if no safe spot found.
     */
    public static BlockPos getSafeBlock(Player player) {
        BlockPos pos = getRaycastBlock(player);
        if (pos == null) {
            return player.blockPosition();
        }
        return pos;
    }
}