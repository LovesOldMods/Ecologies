package com.ecologies.ecology;

import com.ecologies.EcoConfig;
import com.ecologies.data.AnimalRules;
import com.ecologies.data.EcoDataTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.EnumSet;
import java.util.List;

public final class Herds {
    public static final byte EAT_EVENT = 10; // same id sheep use for their grazing animation
    private static final String NEXT_MEAL = "ecologies:next_meal";
    private static final String LOST_KIN = "ecologies:lost_kin";
    private static final String FIRST_DROP = "ecologies:first_drop";

    private Herds() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Herds::onJoin);
        NeoForge.EVENT_BUS.addListener(Herds::onDeath);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob mob)) return;
        if (!EcoConfig.on(EcoConfig.HERDS)) return;
        AnimalRules.Grazing grazing = EcoDataTypes.forAnimal(EcoDataTypes.GRAZING, mob.getType(), AnimalRules.Grazing::animal);
        if (grazing != null) {
            mob.goalSelector.addGoal(4, new GrazeGoal(mob, grazing));
            // new animals wait a bit before their first drop
            if (EntityData.getLong(mob, FIRST_DROP, Long.MIN_VALUE) == Long.MIN_VALUE) {
                EntityData.putLong(mob, FIRST_DROP, mob.level().getGameTime() + grazing.firstDropDelay());
            }
        }
        AnimalRules.Herding herding = EcoDataTypes.forAnimal(EcoDataTypes.HERDING, mob.getType(), AnimalRules.Herding::animal);
        if (herding != null) mob.goalSelector.addGoal(5, new HerdGoal(mob, herding));
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || !EcoConfig.on(EcoConfig.HERDS)) return;
        AnimalRules.NaturalBreeding rules = EcoDataTypes.forAnimal(EcoDataTypes.NATURAL_BREEDING, dead.getType(), AnimalRules.NaturalBreeding::animal);
        if (rules == null) return;
        for (Animal kin : dead.level().getEntitiesOfClass(Animal.class, dead.getBoundingBox().inflate(rules.range()),
                a -> a != dead && a.getType() == dead.getType() && a.isAlive())) {
            EntityData.putLong(kin, LOST_KIN, dead.level().getGameTime());
        }
    }

    private static void maybeFallInLove(PathfinderMob mob) {
        if (!(mob instanceof Animal animal) || animal.isBaby() || animal.getAge() != 0 || animal.isInLove()) return;
        AnimalRules.NaturalBreeding rules = EcoDataTypes.forAnimal(EcoDataTypes.NATURAL_BREEDING, mob.getType(), AnimalRules.NaturalBreeding::animal);
        if (rules == null) return;
        long lost = EntityData.getLong(mob, LOST_KIN, Long.MIN_VALUE);
        if (lost == Long.MIN_VALUE || mob.level().getGameTime() - lost > rules.memoryTicks()) return;
        boolean young = !mob.level().getEntitiesOfClass(Animal.class, mob.getBoundingBox().inflate(rules.range()),
                a -> a.getType() == mob.getType() && a.isBaby()).isEmpty();
        if (young) return;
        animal.setInLoveTime(rules.loveTicks());
        EntityData.putLong(mob, LOST_KIN, Long.MIN_VALUE);
        mob.level().broadcastEntityEvent(mob, (byte) 18); // heart particles
    }

    private static void eat(Level level, BlockPos pos, BlockState state) {
        for (Direction face : Direction.values()) {
            BooleanProperty property = MultifaceBlock.getFaceProperty(face);
            if (state.hasProperty(property) && state.getValue(property)) {
                BlockState less = state.setValue(property, false);
                boolean any = false;
                for (Direction d : Direction.values()) {
                    BooleanProperty p = MultifaceBlock.getFaceProperty(d);
                    if (less.hasProperty(p) && less.getValue(p)) any = true;
                }
                level.setBlockAndUpdate(pos, any ? less : less.getFluidState().createLegacyBlock());
                return;
            }
        }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static void afterMeal(PathfinderMob mob, AnimalRules.Grazing rules) {
        Level level = mob.level();
        mob.heal(2.0F);
        EntityData.putLong(mob, NEXT_MEAL, level.getGameTime() + rules.eatCooldown());
        maybeFallInLove(mob);
        if (rules.drop().isEmpty() || mob.isBaby()) return;
        if (level.getGameTime() < EntityData.getLong(mob, FIRST_DROP, Long.MIN_VALUE)) return;
        int welfare = welfare(mob, rules);
        if (welfare < 0 || rules.dropCounts().isEmpty()) return;
        int count = rules.dropCounts().get(Math.min(welfare, rules.dropCounts().size() - 1));
        BuiltInRegistries.ITEM.getOptional(rules.drop().get()).ifPresent(item -> {
            if (count > 0) Block.popResource(level, mob.blockPosition(), new ItemStack(item, count));
        });
    }

    public static int welfare(PathfinderMob mob, AnimalRules.Grazing rules) {
        Level level = mob.level();
        List<LivingEntity> close = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(8),
                e -> e != mob && e.getType() == mob.getType());
        if (close.size() + 1 >= rules.overcrowdedAt()) return -1;
        int met = 0;
        // Space: most of a 9x9 is open.
        BlockPos feet = mob.blockPosition();
        int free = 0, total = 0;
        for (BlockPos p : BlockPos.betweenClosed(feet.offset(-4, 0, -4), feet.offset(4, 1, 4))) {
            total++;
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()) free++;
        }
        if (free >= total * 0.85) met++;
        // Company: some nearby, not a crowd.
        int near = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(12),
                e -> e != mob && e.getType() == mob.getType()).size();
        if (near >= 1 && near <= 6) met++;
        // Open sky.
        if (level.canSeeSky(feet.above())) met++;
        // Home biome.
        if (rules.habitat().isPresent()
                && level.getBiome(feet).is(TagKey.create(Registries.BIOME, rules.habitat().get()))) met++;
        return met;
    }

    static final class GrazeGoal extends Goal {
        private final PathfinderMob mob;
        private final AnimalRules.Grazing rules;
        private BlockPos target;
        private int ticks;

        GrazeGoal(PathfinderMob mob, AnimalRules.Grazing rules) {
            this.mob = mob;
            this.rules = rules;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (mob.getRandom().nextInt(40) != 0) return false;
            if (mob.level().getGameTime() < EntityData.getLong(mob, NEXT_MEAL, 0)) return false;
            target = findFood();
            return target != null;
        }

        private BlockPos findFood() {
            BlockPos center = mob.blockPosition();
            int r = rules.searchRange();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -3, -r), center.offset(r, 3, r))) {
                if (!rules.food().test(mob.level().getBlockState(p))) continue;
                double d = p.distSqr(center);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            }
            return best;
        }

        @Override
        public void start() {
            ticks = 0;
            mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && ticks < 300 && rules.food().test(mob.level().getBlockState(target));
        }

        @Override
        public void tick() {
            ticks++;
            mob.getLookControl().setLookAt(Vec3.atCenterOf(target));
            if (mob.position().distanceToSqr(Vec3.atBottomCenterOf(target)) < 2.5 * 2.5) {
                BlockState state = mob.level().getBlockState(target);
                mob.level().broadcastEntityEvent(mob, EAT_EVENT);
                mob.level().playSound(null, target, SoundEvents.GRASS_BREAK, SoundSource.NEUTRAL, 0.8F, 0.9F);
                eat(mob.level(), target, state);
                afterMeal(mob, rules);
                target = null;
            } else if (mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public void stop() {
            target = null;
            mob.getNavigation().stop();
        }
    }

    static final class HerdGoal extends Goal {
        private final PathfinderMob mob;
        private final AnimalRules.Herding rules;
        private Vec3 middle;

        HerdGoal(PathfinderMob mob, AnimalRules.Herding rules) {
            this.mob = mob;
            this.rules = rules;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (mob.getRandom().nextInt(60) != 0) return false;
            List<LivingEntity> herd = mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(rules.range()),
                    e -> e != mob && e.getType() == mob.getType() && e.isAlive());
            if (herd.isEmpty()) return false;
            Vec3 sum = Vec3.ZERO;
            for (LivingEntity e : herd) sum = sum.add(e.position());
            middle = sum.scale(1.0 / herd.size());
            return mob.position().distanceTo(middle) > rules.spread();
        }

        @Override
        public void start() {
            // aim somewhere inside the herd, not the exact middle
            Vec3 toward = mob.position().add(middle.subtract(mob.position()).scale(0.7));
            mob.getNavigation().moveTo(toward.x, toward.y, toward.z, rules.speed());
        }

        @Override
        public boolean canContinueToUse() {
            return !mob.getNavigation().isDone() && mob.position().distanceTo(middle) > rules.spread() * 0.6;
        }
    }
}
