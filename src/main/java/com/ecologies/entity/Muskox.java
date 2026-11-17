package com.ecologies.entity;

import com.ecologies.ecology.Herds;
import com.ecologies.registry.EcoEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
//? if <1.21.2 {
/*import net.minecraft.world.entity.MobSpawnType;
*///?}
//? if >=1.21.2 {
import net.minecraft.world.entity.EntitySpawnReason;
//?}
//? if >=1.21.6 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?}

public class Muskox extends Animal {
    public static final int BROWN = 0, BLACK = 1;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Muskox.class, EntityDataSerializers.INT);

    public int eatAnimationTicks;
    public boolean ramming;
    private float ramHead, ramHeadO;

    public float ramAmount(float partialTicks) {
        return Mth.lerp(partialTicks, ramHeadO, ramHead);
    }

    public Muskox(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        //? if >=1.21.6 {
        AttributeSupplier.Builder base = Animal.createAnimalAttributes(); // includes the tempt range TemptGoal needs
        //?} else
        /*AttributeSupplier.Builder base = Animal.createMobAttributes();*/
        return base.add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.8));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.2, stack -> stack.is(Items.GLOW_LICHEN), false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, BROWN);
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        entityData.set(VARIANT, variant);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GLOW_LICHEN);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        //? if >=1.21.2 {
        Muskox calf = EcoEntities.MUSKOX.get().create(level, EntitySpawnReason.BREEDING);
        //?} else
        /*Muskox calf = EcoEntities.MUSKOX.get().create(level);*/
        if (calf != null) {
            int other = partner instanceof Muskox m ? m.getVariant() : getVariant();
            calf.setVariant(random.nextBoolean() ? getVariant() : other);
        }
        return calf;
    }

    // slow head turning since it's heavy
    @Override
    public int getHeadRotSpeed() {
        return 4;
    }

    @Override
    public int getMaxHeadXRot() {
        return 30;
    }

    @Override
    public int getMaxHeadYRot() {
        return 45;
    }

    public float headDownAmount(float partialTicks) {
        if (eatAnimationTicks <= 0) return 0.0F;
        float t = eatAnimationTicks - partialTicks;
        float raw = t >= 34 ? (40 - t) / 6.0F : t <= 6 ? t / 6.0F : 1.0F;
        raw = Mth.clamp(raw, 0.0F, 1.0F);
        return raw * raw * (3 - 2 * raw); // smoothstep
    }

    public float grazeNibble(float partialTicks) {
        if (eatAnimationTicks <= 6 || eatAnimationTicks >= 34) return 0.0F;
        float t = (eatAnimationTicks - 6 - partialTicks) / 28.0F;
        return 0.12F * Mth.sin(t * 18.0F);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == Herds.EAT_EVENT) eatAnimationTicks = 40;
        else if (id == EntityEvent.START_RAM) ramming = true;
        else if (id == EntityEvent.END_RAM) ramming = false;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        if (level().isClientSide() && eatAnimationTicks > 0) eatAnimationTicks--;
        if (level().isClientSide()) {
            ramHeadO = ramHead;
            ramHead = ramming ? Math.min(1.0F, ramHead + 0.2F) : Math.max(0.0F, ramHead - 0.12F);
        }
        super.aiStep();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.GOAT_STEP, 0.15F, 0.8F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6F; // much deeper than a goat
    }

    //? if >=1.21.2 {
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data) {
        setVariant(level.getRandom().nextInt(3) == 0 ? BLACK : BROWN);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // not much grass in the tundra so any solid ground works
    public static boolean checkSpawnRules(EntityType<Muskox> type, ServerLevelAccessor level,
                                          EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getRawBrightness(pos, 0) > 8;
    }
    //?} else {
    /*@Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, SpawnGroupData data) {
        setVariant(level.getRandom().nextInt(3) == 0 ? BLACK : BROWN);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    public static boolean checkSpawnRules(EntityType<Muskox> type, ServerLevelAccessor level,
                                          MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getRawBrightness(pos, 0) > 8;
    }
    *///?}

    //? if >=1.21.6 {
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setVariant(input.getIntOr("Variant", BROWN));
    }
    //?} else {
    /*@Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
    }
    *///?}
}
