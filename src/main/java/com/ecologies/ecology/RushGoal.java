package com.ecologies.ecology;

import com.ecologies.data.PredatorRules;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

final class RushGoal extends Goal {
    private static final double LEAP_RANGE = 3.5;
    private final PathfinderMob mob;
    private final PredatorRules rules;
    private int leapCooldown, biteCooldown, ticks;
    private boolean airborne, lastWasLeap, leapNext;

    RushGoal(PathfinderMob mob, PredatorRules rules) {
        this.mob = mob;
        this.rules = rules;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private LivingEntity prey() {
        LivingEntity prey = mob.getTarget();
        boolean fox = mob instanceof net.minecraft.world.entity.animal./*? if >=26.1 {*/fox./*?}*/Fox f && !f.isSleeping();
        return fox && prey != null && prey.isAlive() && rules.prey().test(prey.getType()) && PackHuntGoal.wild(mob)
                && Predation.isHungry(mob, rules) ? prey : null;
    }

    @Override
    public boolean canUse() {
        LivingEntity prey = prey();
        if (prey == null || mob.distanceTo(prey) > rules.rushRange()) return false;
        // fish from the bank only, once it's in the water it just bites
        return !prey.isInWater() || (Predation.isFish(prey) && !mob.isInWater());
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity prey = prey();
        return prey != null && (airborne || mob.distanceTo(prey) <= rules.rushRange() + 3) && ticks < 120;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        airborne = false;
        lastWasLeap = false;
        leapNext = mob.getRandom().nextFloat() < 0.7F; // usually opens with a pounce
        fox(false, false);
    }

    @Override
    public void stop() {
        fox(false, false);
        airborne = false;
    }

    private void fox(boolean crouching, boolean pouncing) {
        if (mob instanceof net.minecraft.world.entity.animal./*? if >=26.1 {*/fox./*?}*/Fox f) {
            f.setIsCrouching(crouching);
            f.setIsPouncing(pouncing);
        }
    }

    static Vec3 lead(LivingEntity prey, double ticks) {
        Vec3 v = prey.getDeltaMovement();
        return prey.position().add(v.x * ticks, 0, v.z * ticks);
    }

    @Override
    public void tick() {
        LivingEntity prey = prey();
        if (prey == null) return;
        ticks++;
        if (leapCooldown > 0) leapCooldown--;
        if (biteCooldown > 0) biteCooldown--;
        mob.getLookControl().setLookAt(prey, 60.0F, 30.0F);
        double distance = mob.distanceTo(prey);
        double reach = (mob.getBbWidth() + prey.getBbWidth()) / 2 + 0.9;

        if (airborne) {
            if ((mob.onGround() || mob.isInWater()) && mob.getDeltaMovement().y <= 0) {
                airborne = false;
                fox(false, false);
            }
        } else if ((leapNext || (Predation.isFish(prey) && !mob.isInWater())) && distance < LEAP_RANGE + 1
                && distance > reach * 0.8 && mob.onGround() && leapCooldown == 0) {
            // aim for where it'll be in about 10 ticks
            Vec3 aim = lead(prey, 10).subtract(mob.position());
            Vec3 flat = new Vec3(aim.x, 0, aim.z);
            double len = flat.length();
            if (len > 1.0E-3) {
                double push = Mth.clamp(len / 10.0, 0.25, 0.6);
                // Lower, flatter arc for fish.
                double up = prey.isInWater() ? 0.3 : 0.42;
                mob.setDeltaMovement(flat.x / len * push, up, flat.z / len * push);
                mob.setYRot((float) (Mth.atan2(flat.z, flat.x) * Mth.RAD_TO_DEG) - 90.0F);
                mob.yBodyRot = mob.getYRot();
                airborne = true;
                leapCooldown = 40;
                lastWasLeap = true;
                leapNext = false; // never two pounces in a row: a bite or two goes in between
                fox(false, true);
                mob.getNavigation().stop();
            }
        } else {
            mob.getNavigation().moveTo(prey, 1.5);
        }
        if (distance <= reach && biteCooldown == 0) {
            biteCooldown = 12;
            Predation.bite(mob, prey);
            // only pounce again sometimes after a bite
            leapNext = lastWasLeap ? false : mob.getRandom().nextFloat() < 0.4F;
            lastWasLeap = false;
        } else if (!leapNext && !airborne && distance > LEAP_RANGE + 1 && !lastWasLeap) {
            leapNext = true; // the prey got away from its jaws: pounce to catch up
        }
    }
}
