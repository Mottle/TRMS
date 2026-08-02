package moe.liar.trms.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import moe.liar.trms.common.MoldCooling;

/** Lightweight, local-only ambience emitted by a placed, visually filled mold. */
final class MoldMoltenEffects {
    private static final int SPARK_INTERVAL = 50;
    private static final int SMOKE_INTERVAL = 60;
    private static final int AMBIENT_SOUND_INTERVAL = 100;
    private static final double MODEL_SCALE = 1.0D / 16.0D;
    /** Start effects a hair above the lowered fill surface, rather than at the ceramic rim. */
    private static final double PARTICLE_Y = MoldMeshTopology.FILL_SURFACE_Y * MODEL_SCALE + 0.005D;

    private MoldMoltenEffects() {
    }

    /**
     * Reuses vanilla lava's restrained ambient cadence without creating a FluidState or server event.
     * The caller supplies a client-side level and the server-synchronized pattern snapshot.
     */
    static void animate(Level level, BlockPos pos, MoldPattern pattern, Direction facing, int coolingTicks,
                        RandomSource random) {
        if (pattern.carvedCount() == 0 || level.getBlockState(pos.above()).isSolidRender()) {
            return;
        }
        if (random.nextInt(intervalForCooling(SPARK_INTERVAL, coolingTicks)) == 0) {
            Surface surface = randomSurface(pattern, facing, random);
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + surface.x(), pos.getY() + PARTICLE_Y, pos.getZ() + surface.z(),
                    0.0D, 0.0D, 0.0D);
            level.playLocalSound(pos.getX() + surface.x(), pos.getY() + PARTICLE_Y, pos.getZ() + surface.z(),
                    SoundEvents.LAVA_POP, SoundSource.AMBIENT,
                    0.16F + random.nextFloat() * 0.08F, 0.90F + random.nextFloat() * 0.15F, false);
        }
        if (random.nextInt(intervalForCooling(SMOKE_INTERVAL, coolingTicks)) == 0) {
            Surface surface = randomSurface(pattern, facing, random);
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + surface.x(), pos.getY() + PARTICLE_Y, pos.getZ() + surface.z(),
                    0.0D, 0.012D, 0.0D);
        }
        if (random.nextInt(intervalForCooling(AMBIENT_SOUND_INTERVAL, coolingTicks)) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + PARTICLE_Y, pos.getZ() + 0.5D,
                    SoundEvents.LAVA_AMBIENT, SoundSource.AMBIENT,
                    0.16F + random.nextFloat() * 0.08F, 0.90F + random.nextFloat() * 0.15F, false);
        }
    }

    /** Lowers the probability in the same linear temperature curve as the material tint. */
    static int intervalForCooling(int baseInterval, int coolingTicks) {
        if (baseInterval <= 0) {
            throw new IllegalArgumentException("Effect interval must be positive: " + baseInterval);
        }
        return Math.max(1, Math.round(baseInterval / MoldCooling.brightnessForElapsedTicks(coolingTicks)));
    }

    /** Selects one actually filled canonical cell and rotates it into physical block-local coordinates. */
    static Surface randomSurface(MoldPattern pattern, Direction facing, RandomSource random) {
        int remaining = random.nextInt(pattern.carvedCount());
        for (int z = 1; z <= MoldPattern.INNER_SIZE; z++) {
            for (int x = 1; x <= MoldPattern.INNER_SIZE; x++) {
                if (!pattern.isCarved(x, z)) {
                    continue;
                }
                if (remaining-- != 0) {
                    continue;
                }
                double canonicalX = (x + random.nextDouble()) * MODEL_SCALE;
                double canonicalZ = (z + random.nextDouble()) * MODEL_SCALE;
                return switch (facing) {
                    case SOUTH -> new Surface(canonicalX, canonicalZ);
                    case WEST -> new Surface(1.0D - canonicalZ, canonicalX);
                    case NORTH -> new Surface(1.0D - canonicalX, 1.0D - canonicalZ);
                    case EAST -> new Surface(canonicalZ, 1.0D - canonicalX);
                    case UP, DOWN -> throw new IllegalArgumentException("Mold facing must be horizontal: " + facing);
                };
            }
        }
        throw new IllegalStateException("Mold pattern reported carved cells but did not contain one");
    }

    /** A block-local location on the molten surface after its presentation rotation. */
    record Surface(double x, double z) {
    }
}
