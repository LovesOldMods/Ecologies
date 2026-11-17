package com.ecologies.ecology;

import com.ecologies.Ecologies;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

public final class PreyStamina {
    // 8 seconds of being chased.
    static final int STAMINA = 160;
    static final int EXHAUSTED_TICKS = 140;
    //? if >=1.21.2 {
    private static final Identifier TIRED = Ecologies.id("exhausted");
    //?} else
    /*private static final net.minecraft.resources.ResourceLocation TIRED = Ecologies.id("exhausted");*/

    private static final class State {
        int fatigue;
        long lastChased, exhaustedUntil;
    }

    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    private PreyStamina() {}

    static void chased(LivingEntity prey) {
        long now = prey.level().getGameTime();
        State s = STATES.computeIfAbsent(prey, p -> new State());
        if (s.lastChased == now || now < s.exhaustedUntil) return;
        // Recovered since it was last chased.
        if (s.lastChased > 0) s.fatigue = Math.max(0, s.fatigue - (int) Math.max(0, now - s.lastChased - 1) / 2);
        s.lastChased = now;
        if (++s.fatigue >= STAMINA) {
            s.fatigue = 0;
            s.exhaustedUntil = now + EXHAUSTED_TICKS;
            var speed = prey.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(TIRED) == null) {
                speed.addTransientModifier(new AttributeModifier(TIRED, -0.55, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    public static boolean exhausted(LivingEntity prey) {
        State s = STATES.get(prey);
        if (s == null) return false;
        if (prey.level().getGameTime() < s.exhaustedUntil) return true;
        var speed = prey.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(TIRED) != null) speed.removeModifier(TIRED);
        return false;
    }

    static final class Goal extends net.minecraft.world.entity.ai.goal.Goal {
        private final LivingEntity prey;

        Goal(LivingEntity prey) {
            this.prey = prey;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            return STATES.containsKey(prey);
        }

        @Override
        public void tick() {
            exhausted(prey);
        }
    }
}
