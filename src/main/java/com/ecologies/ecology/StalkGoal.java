package com.ecologies.ecology;

import com.ecologies.data.PredatorRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

final class StalkGoal extends Goal {
    private static final double MAX_RANGE = 24.0;
    private final PathfinderMob mob;
    private final PredatorRules rules;
    private int repath;

    StalkGoal(PathfinderMob mob, PredatorRules rules) {
        this.mob = mob;
        this.rules = rules;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private LivingEntity prey() {
        LivingEntity prey = mob.getTarget();
        boolean fox = mob instanceof net.minecraft.world.entity.animal./*? if >=26.1 {*/fox./*?}*/Fox f && !f.isSleeping();
        if (!fox || prey == null || !prey.isAlive() || !rules.prey().test(prey.getType()) || !PackHuntGoal.wild(mob)
                || !Predation.isHungry(mob, rules) || mob.isInWater()
                || (prey.isInWater() && !Predation.isFish(prey))) return null;
        double distance = mob.distanceTo(prey);
        return distance > rules.rushRange() && distance <= MAX_RANGE ? prey : null;
    }

    @Override
    public boolean canUse() {
        return prey() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return prey() != null;
    }

    @Override
    public void start() {
        repath = 0;
        crouch(true);
    }

    @Override
    public void stop() {
        crouch(false);
    }

    private void crouch(boolean on) {
        if (mob instanceof net.minecraft.world.entity.animal./*? if >=26.1 {*/fox./*?}*/Fox f) f.setIsCrouching(on);
    }

    @Override
    public void tick() {
        LivingEntity prey = prey();
        if (prey == null) return;
        crouch(true);
        mob.getLookControl().setLookAt(prey, 30.0F, 30.0F);
        if (--repath <= 0) {
            repath = 10;
            mob.getNavigation().moveTo(prey, 0.6); // a slow creep
        }
    }
}
