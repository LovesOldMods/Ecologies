package com.ecologies.ecology;

import com.ecologies.data.PredatorRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import java.util.Comparator;
import java.util.EnumSet;

// vanilla targeting barely sees fish underwater, bears and foxes basically never fished without this
final class FishTargetGoal extends TargetGoal {
    private final PredatorRules rules;
    private LivingEntity fish;
    private int cooldown;

    FishTargetGoal(Mob mob, PredatorRules rules) {
        super(mob, false);
        this.rules = rules;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // goal selectors skip ticks so count down instead
        if (--cooldown > 0) return false;
        cooldown = 10;
        if (mob.getTarget() != null || !Predation.isHungry(mob, rules) || !PackHuntGoal.wild(mob)) return false;
        fish = mob.level().getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(12, 6, 12),
                        e -> e.isAlive() && e.isInWater() && Predation.isFish(e) && rules.prey().test(e.getType()))
                .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        return fish != null;
    }

    @Override
    public void start() {
        mob.setTarget(fish);
        super.start();
    }
}
