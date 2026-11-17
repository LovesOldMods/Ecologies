package com.ecologies.client;

import com.ecologies.registry.EcoParticles;
import com.ecologies.worldgen.EcoBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

public final class Flurries {
    private static final int RANGE = 20, ABOVE_MIN = 3, ABOVE_MAX = 16;
    private static final float FLAKES_PER_TICK = 5.0F;

    private Flurries() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        var player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;
        float cloud = level.getRainLevel(1.0F);
        if (cloud < 0.2F) return;
        RandomSource random = level.getRandom();
        // Slow wind that drifts over the day.
        float t = level.getGameTime() / 2400.0F;
        double windX = Mth.sin(t) * 0.02, windZ = Mth.cos(t * 0.7F) * 0.02;
        float wanted = FLAKES_PER_TICK * cloud;
        int count = (int) wanted + (random.nextFloat() < wanted - (int) wanted ? 1 : 0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (random.nextDouble() * 2 - 1) * RANGE;
            double z = player.getZ() + (random.nextDouble() * 2 - 1) * RANGE;
            double y = player.getY() + ABOVE_MIN + random.nextDouble() * (ABOVE_MAX - ABOVE_MIN);
            pos.set(x, y, z);
            if (y < level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ())) continue; // under cover
            var biome = level.getBiome(pos);
            if (!biome.is(EcoBiomes.TUNDRA) && !biome.is(EcoBiomes.LICHEN_WOODLAND)) continue;
            level.addParticle(EcoParticles.FLURRY.get(), x, y, z, windX, 0.0, windZ);
        }
    }
}
