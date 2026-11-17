package com.ecologies.ecology;

import com.ecologies.data.PredatorRules;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

// Based on wild wolves. Not faster than the prey, they wear it out.
final class PackHuntGoal extends Goal {
    private static final double RANGE = 24.0, FLANK_RADIUS = 4.0;
    private static final int RELAY_TICKS = 80;

    private final PathfinderMob mob;
    private final PredatorRules rules;
    private int repath;

    PackHuntGoal(PathfinderMob mob, PredatorRules rules) {
        this.mob = mob;
        this.rules = rules;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    static boolean wild(Mob mob) {
        return !(mob instanceof TamableAnimal t && t.isTame()) && !mob.isBaby();
    }

    static List<Mob> pack(Mob mob, LivingEntity prey) {
        List<Mob> members = mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(RANGE),
                m -> m.getType() == mob.getType() && m.isAlive() && wild(m) && m.getTarget() == prey);
        members.sort(Comparator.comparingInt(Mob::getId));
        return members;
    }

    private LivingEntity prey() {
        LivingEntity prey = mob.getTarget();
        return prey != null && prey.isAlive() && rules.prey().test(prey.getType()) && wild(mob) ? prey : null;
    }

    private boolean active() {
        LivingEntity prey = prey();
        return rules.packSize() > 0 && prey != null && !PreyStamina.exhausted(prey) && pack(mob, prey).size() >= rules.packSize();
    }

    @Override
    public boolean canUse() {
        return active();
    }

    @Override
    public boolean canContinueToUse() {
        return active();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity prey = prey();
        if (prey == null) return;
        mob.getLookControl().setLookAt(prey, 30.0F, 30.0F);
        if (--repath > 0) return;
        repath = 5;
        List<Mob> pack = pack(mob, prey);
        int n = pack.size(), me = pack.indexOf(mob);
        if (me < 0) return;
        int lead = (int) (mob.level().getGameTime() / RELAY_TICKS % n);
        if (me == lead) {
            // Chaser stays on its tail.
            mob.getNavigation().moveTo(prey, 1.1);
            return;
        }
        // Flankers go wide and ahead.
        Vec3 run = prey.getDeltaMovement().multiply(1, 0, 1);
        double heading = run.lengthSqr() > 1.0E-4 ? Math.atan2(run.z, run.x)
                : Math.atan2(prey.getZ() - pack.get(lead).getZ(), prey.getX() - pack.get(lead).getX());
        int slot = me < lead ? me : me - 1; // 0.. among the flankers
        double side = slot % 2 == 0 ? 1 : -1;
        double angle = heading + side * Math.toRadians(70 + 35 * (slot / 2));
        double x = prey.getX() + Math.cos(angle) * FLANK_RADIUS, z = prey.getZ() + Math.sin(angle) * FLANK_RADIUS;
        mob.getNavigation().moveTo(x, prey.getY(), z, 1.15);
    }

    static void rally(Mob finder, LivingEntity prey, PredatorRules rules) {
        if (rules.packSize() <= 0) return;
        List<Mob> others = finder.level().getEntitiesOfClass(Mob.class, finder.getBoundingBox().inflate(RANGE / 1.5),
                m -> m != finder && m.getType() == finder.getType() && m.isAlive() && wild(m) && m.getTarget() == null
                        && Predation.isHungry(m, rules));
        if (others.size() + 1 < rules.packSize()) return;
        for (Mob m : others) m.setTarget(prey);
    }

    static float wrap(float degrees) {
        return Mth.wrapDegrees(degrees);
    }
}
