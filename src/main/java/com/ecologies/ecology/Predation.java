package com.ecologies.ecology;

import com.ecologies.EcoConfig;
import com.ecologies.Ecologies;
import com.ecologies.data.DefenseRules;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;
import com.ecologies.data.EcoDataTypes;
import com.ecologies.data.PredatorRules;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
//? if >=26.1 {
import net.minecraft.world.entity.animal.polarbear.PolarBear;
//?} else
/*import net.minecraft.world.entity.animal.PolarBear;*/
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class Predation {
    private static final String LAST_MEAL = "ecologies:last_meal";
    private static final double BABY_SEARCH_RANGE = 16.0;

    private Predation() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Predation::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(Predation::onDeath);
        NeoForge.EVENT_BUS.addListener(Predation::onDrops);
        NeoForge.EVENT_BUS.addListener(Predation::onJoin);
    }

    public static boolean isHungry(Mob predator, PredatorRules rules) {
        long lastMeal = EntityData.getLong(predator, LAST_MEAL, Long.MIN_VALUE);
        return lastMeal == Long.MIN_VALUE || predator.level().getGameTime() - lastMeal >= rules.hungerCooldown();
    }

    private static boolean isHuntingInTheOpen(Entity maybePredator, Entity prey) {
        if (!isHungryPredatorOf(maybePredator, prey)) return false;
        Mob predator = (Mob) maybePredator;
        // fish only react to predators that are in the water and can be seen
        if (isFish(prey) && !(predator.isInWater() && prey instanceof LivingEntity fish && fish.hasLineOfSight(predator))) return false;
        LivingEntity target = predator.getTarget();
        if (target == null || target.getType() != prey.getType()) return false;
        PredatorRules rules = EcoDataTypes.predatorRules(predator.getType());
        if (HuntGoal.sneaking(predator)) return false;
        return !rules.stalks() || predator.distanceTo(prey) < 3.0 || predator.getTarget() == prey && predator.distanceTo(prey) < 5.0
                && !predator.getNavigation().isDone() && predator.getDeltaMovement().horizontalDistance() > 0.12;
    }

    static boolean isFish(Entity entity) {
        var category = entity.getType().getCategory();
        return category == MobCategory.WATER_AMBIENT || category == MobCategory.WATER_CREATURE;
    }

    private static boolean isHungryPredatorOf(Entity maybePredator, Entity prey) {
        if (!(maybePredator instanceof Mob predator)) return false;
        PredatorRules rules = EcoDataTypes.predatorRules(predator.getType());
        return rules != null && rules.prey().test(prey.getType()) && isHungry(predator, rules);
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!EcoConfig.on(EcoConfig.PREDATION)) return;
        if (!(event.getEntity() instanceof Mob predator) || predator.level().isClientSide()) return;
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null || target instanceof Player) return;
        PredatorRules rules = EcoDataTypes.predatorRules(predator.getType());
        if (rules == null || !rules.prey().test(target.getType())) return;
        if (predator.getLastHurtByMob() == target) return; // fighting back is not hunting
        // pets attack whatever their owner tells them to
        if (predator instanceof TamableAnimal pet && pet.isTame()) return;

        if (!isHungry(predator, rules) || HuntGoal.gaveUpOn(predator, target) || scared(predator)) {
            event.setCanceled(true);
            return;
        }
        if (rules.prefersBabies() && target instanceof AgeableMob adult && !adult.isBaby()) {
            AABB area = predator.getBoundingBox().inflate(BABY_SEARCH_RANGE);
            List<AgeableMob> babies = predator.level().getEntitiesOfClass(AgeableMob.class, area,
                    m -> m.isBaby() && m.isAlive() && m.getType() == target.getType() && predator.hasLineOfSight(m));
            babies.stream().min(Comparator.comparingDouble(predator::distanceToSqr)).ifPresent(event::setNewAboutToBeSetTarget);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity prey = event.getEntity();
        if (!EcoConfig.on(EcoConfig.PREDATION)) return;
        if (prey.level().isClientSide() || !(event.getSource().getEntity() instanceof Mob predator)) return;
        PredatorRules rules = EcoDataTypes.predatorRules(predator.getType());
        if (rules == null || !rules.prey().test(prey.getType())) return;
        EntityData.putLong(predator, LAST_MEAL, predator.level().getGameTime());
        predator.heal(predator.getMaxHealth() * 0.25F); // a meal
        predator.setTarget(null);
    }

    private static void onDrops(LivingDropsEvent event) {
        if (!EcoConfig.on(EcoConfig.PREDATION)) return;
        if (!(event.getSource().getEntity() instanceof Mob predator)) return;
        PredatorRules rules = EcoDataTypes.predatorRules(predator.getType());
        if (rules == null || !rules.eatsKill() || !rules.prey().test(event.getEntity().getType())) return;
        event.getDrops().removeIf(drop -> drop.getItem().has(DataComponents.FOOD));
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !EcoConfig.on(EcoConfig.PREDATION)) return;
        Entity entity = event.getEntity();

        if (entity instanceof PathfinderMob defender) {
            var defense = EcoDataTypes.forAnimal(EcoDataTypes.DEFENSE, defender.getType(), DefenseRules::animal);
            if (defense != null) defender.goalSelector.addGoal(0, new DefendGoal(defender, defense));
        }

        if (entity instanceof PathfinderMob prey) {
            if (EcoDataTypes.PREDATION.values().stream().anyMatch(r -> r.prey().test(prey.getType()))) {
                prey.goalSelector.addGoal(0, new PreyStamina.Goal(prey));
            }
            for (PredatorRules rules : EcoDataTypes.PREDATION.values()) {
                if (rules.preyFleeRange() > 0 && rules.prey().test(prey.getType())) {
                    // close to the hunters' pace, solo wolves and foxes gain on them and packs wear them out
                    prey.goalSelector.addGoal(1, new AvoidEntityGoal<>(prey, Mob.class, rules.preyFleeRange(), 1.0, 1.25,
                            other -> !PreyStamina.exhausted(prey) && isHuntingInTheOpen(other, prey)));
                }
            }
        }

        if (entity instanceof Mob predator) {
            PredatorRules rules = EcoDataTypes.predatorRules(predator.getType());
            // bear rear-ups stop it dead and the prey gets away, so bite on the run instead
            if (rules != null && rules.chaseSpeed() > 0 && predator instanceof PathfinderMob runner) {
                runner.goalSelector.addGoal(0, new ChaseGoal(runner, rules));
            }
            if (rules != null) predator.targetSelector.addGoal(3, new FishTargetGoal(predator, rules));
            if (rules != null && predator instanceof PathfinderMob hunter) {
                hunter.goalSelector.addGoal(0, new HuntGoal(hunter, rules));
                if (rules.packSize() > 0) hunter.goalSelector.addGoal(0, new PackHuntGoal(hunter, rules));
                if (rules.rushRange() > 0) {
                    hunter.goalSelector.addGoal(0, new RushGoal(hunter, rules));
                    hunter.goalSelector.addGoal(0, new StalkGoal(hunter, rules));
                }
            }
            if (rules != null && rules.addsHunting()) {
                predator.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(predator, LivingEntity.class, 10, true, false,
                        //? if >=1.21.2 {
                        (target, level) -> rules.prey().test(target.getType()) && isHungry(predator, rules)
                        //?} else
                        /*target -> rules.prey().test(target.getType()) && isHungry(predator, rules)*/
                ));
            }
        }
    }

    private static final String SCARED_UNTIL = "ecologies:scared_until";

    static boolean scared(Mob mob) {
        return mob.level().getGameTime() < EntityData.getLong(mob, SCARED_UNTIL, 0);
    }

    static void scareIfWithYoung(Mob predator, LivingEntity from) {
        boolean young = !predator.level().getEntitiesOfClass(LivingEntity.class, predator.getBoundingBox().inflate(24),
                e -> e.getType() == predator.getType() && e.isBaby() && e.isAlive()).isEmpty();
        if (!young || predator.isBaby()) return;
        EntityData.putLong(predator, SCARED_UNTIL, predator.level().getGameTime() + 600);
        predator.setTarget(null);
        if (predator instanceof PathfinderMob runner) {
            // Back to its young.
            LivingEntity cub = predator.level().getEntitiesOfClass(LivingEntity.class, predator.getBoundingBox().inflate(24),
                    e -> e.getType() == predator.getType() && e.isBaby() && e.isAlive()).get(0);
            Vec3 away = cub.position().subtract(from.position()).normalize().scale(6);
            runner.getNavigation().moveTo(cub.getX() + away.x, cub.getY(), cub.getZ() + away.z, 1.4);
        }
        if (predator instanceof PolarBear bear) bear.setStanding(false);
    }

    static void bite(Mob mob, LivingEntity prey) {
        //? if >=26.1 {
        mob.swingForAttack(InteractionHand.MAIN_HAND);
        //?} else
        /*mob.swing(InteractionHand.MAIN_HAND);*/
        //? if >=1.21.2 {
        mob.doHurtTarget((ServerLevel) mob.level(), prey);
        //?} else
        /*mob.doHurtTarget(prey);*/
    }

    static final class HuntGoal extends Goal {
        private static final int CHECK_EVERY = 20, REPLAN_AFTER = 2, STUCK_GIVE_UP = 4, GIVE_UP_TICKS = 600;
        private static final double LOSE_SCENT = 15.0;
        private static final Map<Mob, GaveUp> GAVE_UP = new WeakHashMap<>();
        //? if >=1.21.2 {
        private static final Identifier SPRINT = Ecologies.id("hunt_sprint");
        //?} else
        /*private static final net.minecraft.resources.ResourceLocation SPRINT = Ecologies.id("hunt_sprint");*/

        private record GaveUp(UUID prey, long until) {}

        private final PathfinderMob mob;
        private final PredatorRules rules;
        private LivingEntity quarry;
        private Vec3 lastPos;
        private int ticks, openChase, stuckChecks;
        private boolean wasOnGround = true;

        HuntGoal(PathfinderMob mob, PredatorRules rules) {
            this.mob = mob;
            this.rules = rules;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        static boolean gaveUpOn(Mob predator, LivingEntity target) {
            GaveUp g = GAVE_UP.get(predator);
            return g != null && g.prey().equals(target.getUUID()) && predator.level().getGameTime() < g.until();
        }

        static boolean sneaking(Mob mob) {
            return mob.isCrouching() || (mob instanceof net.minecraft.world.entity.animal./*? if >=26.1 {*/fox./*?}*/Fox fox && fox.isPouncing());
        }

        private boolean fair(LivingEntity target) {
            return target != null && target.isAlive() && !(target instanceof Player) && rules.prey().test(target.getType())
                    && PackHuntGoal.wild(mob) && mob.getLastHurtByMob() != target;
        }

        @Override
        public boolean canUse() {
            return fair(mob.getTarget()) && Predation.isHungry(mob, rules);
        }

        @Override
        public boolean canContinueToUse() {
            return quarry != null && fair(quarry) && Predation.isHungry(mob, rules) && mob.distanceTo(quarry) <= LOSE_SCENT
                    && !gaveUpOn(mob, quarry);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            quarry = mob.getTarget();
            lastPos = mob.position();
            ticks = 0;
            openChase = 0;
            stuckChecks = 0;
            PackHuntGoal.rally(mob, quarry, rules);
        }

        @Override
        public void stop() {
            sprint(false);
            quarry = null;
        }

        private boolean inPack() {
            return rules.packSize() > 0 && quarry != null && PackHuntGoal.pack(mob, quarry).size() >= rules.packSize();
        }

        private void sprint(boolean on) {
            var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null || rules.sprint() == 1.0) return;
            boolean has = speed.getModifier(SPRINT) != null;
            if (on && !has) {
                speed.addTransientModifier(new AttributeModifier(SPRINT, rules.sprint() - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!on && has) {
                speed.removeModifier(SPRINT);
            }
        }

        private void giveUp(LivingEntity prey) {
            GAVE_UP.put(mob, new GaveUp(prey.getUUID(), mob.level().getGameTime() + GIVE_UP_TICKS));
            sprint(false);
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity prey = quarry;
            if (prey == null) return;
            ticks++;
            // lock back on if it lost sight
            if (mob.getTarget() != prey) mob.setTarget(prey);
            boolean onGround = mob.onGround();
            boolean leapt = wasOnGround && !onGround && mob.getDeltaMovement().y > 0.15;
            wasOnGround = onGround;
            if (sneaking(mob)) {
                // still stalking so don't count it against the chase
                sprint(false);
                lastPos = mob.position();
                stuckChecks = 0;
                return;
            }
            boolean pack = inPack();
            sprint(!pack);
            PreyStamina.chased(prey);
            double distance = mob.distanceTo(prey);
            boolean close = distance < (mob.getBbWidth() + prey.getBbWidth()) / 2 + 1.5;
            if (distance < 6.0) {
                // Face the prey when striking.
                mob.getLookControl().setLookAt(prey, 30.0F, 30.0F);
                if (close) {
                    float yaw = (float) (Mth.atan2(prey.getZ() - mob.getZ(), prey.getX() - mob.getX()) * Mth.RAD_TO_DEG) - 90.0F;
                    mob.setYBodyRot(Mth.approachDegrees(mob.yBodyRot, yaw, 20.0F));
                }
            }
            if (leapt && distance < 8.0) leadLeap(prey);
            // swim straight at fish, walking paths can't go under
            if (mob.isInWater() && prey.isInWater() && isFish(prey)) {
                Vec3 to = prey.position().add(0, prey.getBbHeight() / 2, 0)
                        .subtract(mob.position().add(0, mob.getBbHeight() / 3, 0));
                if (to.lengthSqr() > 1.0E-4) {
                    mob.setDeltaMovement(mob.getDeltaMovement().scale(0.8).add(to.normalize().scale(0.07)));
                    mob.getLookControl().setLookAt(prey, 30.0F, 30.0F);
                }
                if (distance < (mob.getBbWidth() + prey.getBbWidth()) / 2 + 1.0 && ticks % 12 == 0) bite(mob, prey);
            }
            int stamina = pack ? rules.giveUpAfter() * 3 : rules.giveUpAfter();
            if (++openChase > stamina && !close && !PreyStamina.exhausted(prey)) {
                giveUp(prey); // spent; it got away
                return;
            }
            if (ticks % CHECK_EVERY != 0) return;
            double moved = mob.position().distanceTo(lastPos);
            lastPos = mob.position();
            if (close || moved > 0.6 || mob.getNavigation().isDone()) {
                stuckChecks = 0;
                return;
            }
            // barely moved in a second so it's stuck
            stuckChecks++;
            if (mob.onGround() && !mob.isInWater()) mob.getJumpControl().jump();
            if (stuckChecks >= STUCK_GIVE_UP) {
                giveUp(prey);
            } else if (stuckChecks >= REPLAN_AFTER) {
                mob.getNavigation().stop();
                mob.getNavigation().moveTo(prey, Math.max(1.0, rules.chaseSpeed()));
            }
        }

        private void leadLeap(LivingEntity prey) {
            Vec3 v = mob.getDeltaMovement();
            double hSpeed = Math.max(0.3, Math.sqrt(v.x * v.x + v.z * v.z));
            double flight = Mth.clamp(mob.distanceTo(prey) / hSpeed, 3, 12);
            Vec3 aim = RushGoal.lead(prey, flight).subtract(mob.position());
            double len = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
            if (len < 1.0E-3) return;
            mob.setDeltaMovement(aim.x / len * hSpeed, v.y, aim.z / len * hSpeed);
        }
    }

    private static final class ChaseGoal extends MeleeAttackGoal {
        private final PredatorRules rules;

        ChaseGoal(PathfinderMob mob, PredatorRules rules) {
            super(mob, rules.chaseSpeed(), true);
            this.rules = rules;
        }

        private boolean hunting() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive() && !(target instanceof Player) && !mob.isBaby()
                    && rules.prey().test(target.getType()) && mob.getLastHurtByMob() != target;
        }

        @Override
        public boolean canUse() {
            return hunting() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return hunting() && super.canContinueToUse();
        }

        @Override
        public void stop() {
            super.stop();
            rearUp(false);
        }

        private boolean nextIsRear, lastWasRear;

        private boolean worthRearing(LivingEntity target) {
            return !target.isBaby() && !target.isInWater() && target.getBbHeight() >= mob.getBbHeight() * 0.6F;
        }

        private void chooseNext(LivingEntity target) {
            nextIsRear = !lastWasRear && worthRearing(target) && mob.getRandom().nextFloat() < 0.4F;
        }

        @Override
        public void start() {
            super.start();
            lastWasRear = false;
            LivingEntity target = mob.getTarget();
            if (target != null) chooseNext(target);
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity target = mob.getTarget();
            // dive after fish instead of pawing at the surface
            if (target != null && target.isInWater() && mob.isInWater() && target.getY() < mob.getY() - 0.4) {
                mob.setDeltaMovement(mob.getDeltaMovement().add(0, -0.06, 0));
            }
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            double distance = mob.distanceTo(target);
            boolean near = distance < (mob.getBbWidth() + target.getBbWidth()) / 2 + 1.5;
            if (canPerformAttack(target)) {
                resetAttackCooldown();
                hit(target);
                rearUp(false);
                lastWasRear = nextIsRear;
                chooseNext(target);
            } else if (near && nextIsRear && getTicksUntilNextAttack() <= 10) {
                rearUp(true);
            } else if (!near) {
                rearUp(false);
            }
        }

        private void hit(LivingEntity target) {
            //? if >=26.1 {
            mob.swingForAttack(InteractionHand.MAIN_HAND);
            //?} else
            /*mob.swing(InteractionHand.MAIN_HAND);*/
            //? if >=1.21.2 {
            mob.doHurtTarget((ServerLevel) mob.level(), target);
            //?} else
            /*mob.doHurtTarget(target);*/
        }

        private void rearUp(boolean standing) {
            if (!(mob instanceof PolarBear bear) || bear.isStanding() == standing) return;
            bear.setStanding(standing);
            if (standing) bear.playSound(SoundEvents.POLAR_BEAR_WARNING, 1.0F, bear.getVoicePitch());
        }
    }
}
