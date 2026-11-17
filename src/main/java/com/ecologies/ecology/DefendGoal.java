package com.ecologies.ecology;

import com.ecologies.Compat;
import com.ecologies.data.DefenseRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

final class DefendGoal extends Goal {
    private static final int MEMORY = 200, RAM_COOLDOWN = 40, GIVE_UP = 300;
    private final PathfinderMob mob;
    private final DefenseRules rules;
    private static final int RETREAT_TICKS = 60, REARM_TICKS = 200;
    private LivingEntity threat;
    private int cooldown, ticks, retreat;
    // hurt count at the last ram, a new hit sets it off again
    private int lastRamTick = -100000, hurtAtRam;
    private boolean lowered;

    // same events as the goat ram so both mobs animate them
    private void lowerHead(boolean down) {
        if (lowered == down) return;
        lowered = down;
        mob.level().broadcastEntityEvent(mob, down ? EntityEvent.START_RAM : EntityEvent.END_RAM);
        if (down) mob.playSound(SoundEvents.GOAT_PREPARE_RAM, 1.0F, Compat.isType(mob, "goat") ? 1.0F : 0.7F);
    }

    DefendGoal(PathfinderMob mob, DefenseRules rules) {
        this.mob = mob;
        this.rules = rules;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean recentAttacker(LivingEntity victim, LivingEntity attacker) {
        return attacker != null && attacker.isAlive() && victim.tickCount - victim.getLastHurtByMobTimestamp() < MEMORY
                && !(attacker instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    private AABB area() {
        return mob.getBoundingBox().inflate(rules.range());
    }

    private LivingEntity findThreat() {
        LivingEntity own = mob.getLastHurtByMob();
        if (recentAttacker(mob, own)) return own;
        if (!rules.protectsYoung()) return null;
        for (LivingEntity young : mob.level().getEntitiesOfClass(LivingEntity.class, area(),
                e -> e.getType() == mob.getType() && e.isBaby() && e.isAlive())) {
            if (recentAttacker(young, young.getLastHurtByMob())) return young.getLastHurtByMob();
            List<Mob> hunters = mob.level().getEntitiesOfClass(Mob.class, area(), m -> m.getTarget() == young && m.isAlive());
            if (!hunters.isEmpty()) return hunters.get(0);
        }
        return null;
    }

    private boolean outnumbered() {
        List<LivingEntity> herd = mob.level().getEntitiesOfClass(LivingEntity.class, area(),
                e -> e.getType() == mob.getType() && e.isAlive());
        int defenders = (int) herd.stream().filter(e -> !e.isBaby()).count();
        int attackers = mob.level().getEntitiesOfClass(Mob.class, area().inflate(4),
                m -> m.isAlive() && m.getTarget() != null && herd.contains(m.getTarget())).size();
        if (threat instanceof Player) attackers = Math.max(attackers, 1);
        return attackers > defenders;
    }

    private boolean rearmed() {
        return mob.tickCount - lastRamTick > REARM_TICKS || mob.getLastHurtByMobTimestamp() > hurtAtRam;
    }

    @Override
    public boolean canUse() {
        if (mob.isBaby() || !rearmed()) return false;
        threat = findThreat();
        boolean go = threat != null && !outnumbered();
        return go;
    }

    private boolean stillThreatening() {
        if (threat == mob.getLastHurtByMob() && recentAttacker(mob, threat)) return true;
        if (threat instanceof Mob m && m.getTarget() != null && m.getTarget().getType() == mob.getType()) return true;
        return mob.level().getEntitiesOfClass(LivingEntity.class, area(),
                e -> e.getType() == mob.getType() && e.getLastHurtByMob() == threat && recentAttacker(e, threat)).size() > 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (retreat > 0) return threat != null;
        return threat != null && threat.isAlive() && stillThreatening() && ticks < GIVE_UP && mob.distanceTo(threat) < rules.range() + 6
                && !outnumbered() && !(threat instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    @Override
    public void start() {
        ticks = 0;
        retreat = 0;
    }

    @Override
    public void stop() {
        lowerHead(false);
        threat = null;
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (threat == null) return;
        ticks++;
        if (cooldown > 0) cooldown--;
        if (retreat > 0) {
            // back off toward the herd
            if (--retreat == 0) threat = null;
            return;
        }
        mob.getLookControl().setLookAt(threat, 30.0F, 30.0F);
        double reach = (mob.getBbWidth() + threat.getBbWidth()) / 2 + 0.8;
        double distance = mob.distanceTo(threat);
        if (cooldown > RAM_COOLDOWN - 15) {
            return; // just rammed: a moment to gather itself
        }
        mob.getNavigation().moveTo(threat, distance > 4 ? 1.7 : 1.3); // a charge
        if (distance < 7) lowerHead(true);
        if (distance <= reach && cooldown == 0 && mob.level() instanceof ServerLevel level) {
            cooldown = RAM_COOLDOWN;
            var source = mob.damageSources().mobAttack(mob);
            Compat.hurt(level, threat, source, rules.damage());
            //? if >=26.1 {
            threat.knockback(rules.knockback(), mob.getX() - threat.getX(), mob.getZ() - threat.getZ(), source, rules.damage());
            //?} else
            /*threat.knockback(rules.knockback(), mob.getX() - threat.getX(), mob.getZ() - threat.getZ());*/
            mob.playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0F, 0.8F);
            lowerHead(false);
            if (threat instanceof Mob attacker) Predation.scareIfWithYoung(attacker, mob);
            lastRamTick = mob.tickCount;
            hurtAtRam = mob.getLastHurtByMobTimestamp();
            retreat = RETREAT_TICKS;
            Vec3 away = mob.position().subtract(threat.position());
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            mob.getNavigation().moveTo(mob.getX() + away.x * 8, mob.getY(), mob.getZ() + away.z * 8, 1.6);
        }
    }
}
