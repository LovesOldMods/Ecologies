package com.ecologies.entity;

import com.ecologies.Compat;
import com.ecologies.block.BoltingCropBlock;
import com.ecologies.block.entity.BatRoostBlockEntity;
import com.ecologies.data.EcoDataTypes;
import com.ecologies.data.RoostRules;
import com.ecologies.registry.EcoBlocks;
import com.ecologies.registry.EcoEntities;
import com.ecologies.registry.EcoTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
//? if >=1.21.2 {
import net.minecraft.world.entity.EntitySpawnReason;
//?}
//? if >=1.21.6 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?}

public class ColonyBat extends Bat {
    public static final TagKey<Block> FORAGE_CROPS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("ecologies", "bat_forage_crops"));
    public static final TagKey<Block> SEEDS_ON_FARMLAND = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("ecologies", "bat_planted_crops"));
    private static final int ROOST_SEARCH = 32;
    private static final int MAX_ROAM = 96;
    private static final int HOMELESS_DEATH_TICKS = 2400;

    private BlockPos roost;
    private BlockPos knownCrops;
    private boolean fed;
    private boolean feast;
    private int homelessTicks;
    private int loveTicks;
    private int breedCooldown;
    private int attackCooldown;
    private BlockPos target;
    private BlockPos homeSpot;
    private BlockPos lastFeedingGround;

    private float exploreHeading = Float.NaN;
    private double bestHomeDistance = Double.MAX_VALUE;
    private int homeStuckTicks;
    private Path path;
    private BlockPos pathTarget;

    public ColonyBat(EntityType<? extends Bat> type, Level level) {
        super(type, level);
    }

    // long path range so they can get home from farms
    public static AttributeSupplier.Builder createColonyAttributes() {
        return Bat.createAttributes().add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public RoostRules rules() {
        return EcoDataTypes.roostRules(getType());
    }

    public BlockPos roost() {
        return roost;
    }

    public void setRoost(BlockPos roost) {
        this.roost = roost == null ? null : roost.immutable();
        this.homelessTicks = 0;
    }

    public void setKnownCrops(BlockPos crops) {
        if (knownCrops == null) knownCrops = crops;
    }

    // only from worldgen, breeding or spawn eggs
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    //? if >=1.21.2 {
    @Override
    protected void customServerAiStep(ServerLevel level) {
        colonyTick(level);
    }
    //?} else {
    /*@Override
    protected void customServerAiStep() {
        colonyTick((ServerLevel) level());
    }
    *///?}

    private void colonyTick(ServerLevel level) {
        if (loveTicks > 0) loveTicks--;
        if (breedCooldown > 0) breedCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (tickCount % 100 == 0 || (roost == null && tickCount % 20 == 1)) checkRoost(level);
        if (loveTicks > 0 && tickCount % 20 == 0) tryBreed(level);

        boolean night = !Compat.isDay(level);
        Player luring = level.getNearestPlayer(getX(), getY(), getZ(), 10,
                p -> p instanceof Player pl && (isLure(pl.getMainHandItem()) || isLure(pl.getOffhandItem())));

        if (isResting()) {
            // sleeps during the day unless something bugs it
            if (night || luring != null || !level.getBlockState(blockPosition().above()).isRedstoneConductor(level, blockPosition())) {
                setResting(false);
            }
            return;
        }

        if (luring != null) {
            target = nearLure(luring);
        } else if (!night && roost != null) {
            goHome(level);
        } else if (night) {
            forage(level);
        } else {
            wander(level);
        }
        steerTowards(target);
        if (luring != null) {
            // stay out of the player's face
            Vec3 away = position().subtract(luring.getEyePosition());
            double d = away.length();
            if (d < 2.0 && d > 1.0E-3) setDeltaMovement(getDeltaMovement().add(away.scale(0.12 / d)));
        }
    }

    private BlockPos nearLure(Player player) {
        float phase = (getId() * 0.618F) % 1.0F * Mth.TWO_PI;     // spread the bats round the circle
        double radius = 2.5 + (getId() % 3) * 0.6;              // and over a few distances
        double angle = phase + tickCount * 0.02;                // drifting slowly round
        double y = player.getEyeY() + 0.6 + Mth.sin((float) (tickCount * 0.05 + phase)) * 0.5;
        return BlockPos.containing(player.getX() + Math.cos(angle) * radius, y, player.getZ() + Math.sin(angle) * radius);
    }

    private void goHome(ServerLevel level) {
        if (roost == null) return;
        if (homeSpot == null || !level.isEmptyBlock(homeSpot) || !level.getBlockState(homeSpot.above()).isRedstoneConductor(level, homeSpot.above())) {
            homeSpot = findHangingSpot(level, roost);
            if (homeSpot == null) {
                target = roost.below();
                return;
            }
        }
        target = homeSpot;
        double distance = position().distanceTo(Vec3.atCenterOf(homeSpot));
        if (distance < bestHomeDistance - 0.5) {
            bestHomeDistance = distance;
            homeStuckTicks = 0;
        } else if (++homeStuckTicks % 60 == 0) {
            // stuck, make a new path and fly up
            path = null;
            pathTarget = null;
            if (homeStuckTicks % 120 == 0 && level.isEmptyBlock(blockPosition().above(2))) target = blockPosition().above(3);
            if (homeStuckTicks >= 600 && level.getNearestPlayer(this, 24) == null) {
                // lost for 30s with nobody around, just put it back
                setPos(homeSpot.getX() + 0.5, homeSpot.getY() + 0.1, homeSpot.getZ() + 0.5);
                homeStuckTicks = 0;
            }
        }
        if (distance < 1.2) {
            bestHomeDistance = Double.MAX_VALUE;
            homeStuckTicks = 0;
            setDeltaMovement(Vec3.ZERO);
            setPos(homeSpot.getX() + 0.5, homeSpot.getY() + 0.1, homeSpot.getZ() + 0.5);
            setResting(true);
            if (fed) {
                depositGuano(level, homeSpot);
                if (feast) depositGuano(level, homeSpot);
                feast = false;
                shareCrops(level);
                fed = false;
            }
        }
    }

    private BlockPos findHangingSpot(ServerLevel level, BlockPos roost) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(roost.offset(-3, -3, -3), roost.offset(3, 0, 3))) {
            if (!level.isEmptyBlock(p) || !level.getBlockState(p.above()).isRedstoneConductor(level, p.above())) continue;
            BlockPos spot = p.immutable();
            // One bat to a spot.
            boolean taken = !level.getEntitiesOfClass(ColonyBat.class, new AABB(spot).inflate(4), b -> b != this
                    && (spot.equals(b.homeSpot) || (b.isResting() && b.blockPosition().equals(spot)))).isEmpty();
            if (taken) continue;
            double d = p.distSqr(roost) + random.nextDouble();
            if (d < bestDist) {
                bestDist = d;
                best = p.immutable();
            }
        }
        return best;
    }

    public static void depositGuano(ServerLevel level, BlockPos from) {
        BlockPos pos = from.below();
        for (int i = 0; i < 24 && level.isEmptyBlock(pos) && pos.getY() > Compat.minY(level); i++) pos = pos.below();
        BlockState floor = level.getBlockState(pos);
        BlockState layer = EcoBlocks.GUANO_LAYER.get().defaultBlockState();
        if (floor.is(EcoBlocks.GUANO_LAYER.get())) {
            int layers = floor.getValue(SnowLayerBlock.LAYERS);
            level.setBlockAndUpdate(pos, layers >= 7 ? EcoBlocks.GUANO_BLOCK.get().defaultBlockState() : floor.setValue(SnowLayerBlock.LAYERS, layers + 1));
            return;
        }
        BlockPos onTop = pos.above();
        if (level.isEmptyBlock(onTop) && layer.canSurvive(level, onTop)) level.setBlockAndUpdate(onTop, layer);
    }

    private void shareCrops(ServerLevel level) {
        if (knownCrops == null || roost == null) return;
        for (ColonyBat other : level.getEntitiesOfClass(ColonyBat.class, new AABB(roost).inflate(32), b -> roost.equals(b.roost))) {
            other.setKnownCrops(knownCrops);
        }
    }

    private void forage(ServerLevel level) {
        bestHomeDistance = Double.MAX_VALUE; // a new trip home each morning
        homeStuckTicks = 0;
        LivingEntity spider = nearbySpider(level);
        if (spider != null) {
            target = spider.blockPosition();
            if (distanceTo(spider) < 1.6 && attackCooldown == 0) {
                Compat.hurt(level, spider, damageSources().mobAttack(this), 1.0F);
                attackCooldown = 30;
            }
            return;
        }
        if (roost != null && !roost.closerThan(blockPosition(), MAX_ROAM)) {
            target = roost.below(2);
            return;
        }
        if (knownCrops != null) {
            target = knownCrops.above(2);
            if (blockPosition().closerThan(knownCrops, 4) && tickCount % 40 == 0) {
                feedOnCrops(level);
                // move on after feeding for a while
                if (random.nextInt(6) == 0) {
                    lastFeedingGround = knownCrops;
                    knownCrops = null;
                }
            }
            return;
        }
        if (tickCount % 40 == 0) {
            BlockPos crop = findCrop(level);
            if (crop != null) {
                knownCrops = crop;
                return;
            }
            // grass has insects too if there's no farm around
            var ground = level.getBlockState(groundBelow(level));
            // 26.x took grass and moss out of #dirt so check them by name
            if ((ground.is(BlockTags.DIRT) || ground.is(Blocks.GRASS_BLOCK)
                    || ground.is(EcoTags.MOSS)) && random.nextInt(8) == 0) fed = true;
        }
        explore(level);
    }

    private void explore(ServerLevel level) {
        if (target != null && random.nextInt(30) != 0 && !target.closerToCenterThan(position(), 2.0) && level.isEmptyBlock(target)) return;
        if (level.canSeeSky(blockPosition())) {
            BlockPos ground = groundBelow(level);
            int heightAbove = blockPosition().getY() - ground.getY();
            int dy = heightAbove > 8 ? -4 : heightAbove < 3 ? 2 : random.nextInt(3) - 1;
            // hold a heading and turn a bit each time, head back if it's too far from the roost
            if (Float.isNaN(exploreHeading)) exploreHeading = random.nextFloat() * Mth.TWO_PI;
            exploreHeading += (random.nextFloat() - 0.5F) * 1.2F;
            if (roost != null && !roost.closerThan(blockPosition(), MAX_ROAM * 0.75)) {
                exploreHeading = (float) Math.atan2(roost.getZ() - getZ(), roost.getX() - getX()) + (random.nextFloat() - 0.5F);
            }
            double step = 16 + random.nextInt(13);
            target = BlockPos.containing(getX() + Math.cos(exploreHeading) * step, getY() + dy, getZ() + Math.sin(exploreHeading) * step);
            return;
        }
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < 8; i++) {
            BlockPos p = BlockPos.containing(getX() + random.nextInt(17) - 8, getY() + random.nextInt(9) - 3, getZ() + random.nextInt(17) - 8);
            if (!level.isEmptyBlock(p)) continue;
            int score = level.getBrightness(LightLayer.SKY, p) * 4 + p.getY() - blockPosition().getY() + random.nextInt(6);
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        if (best != null) target = best;
        else wander(level);
    }

    private BlockPos groundBelow(ServerLevel level) {
        BlockPos p = blockPosition();
        for (int i = 0; i < 64 && level.isEmptyBlock(p) && p.getY() > Compat.minY(level); i++) p = p.below();
        return p;
    }

    private BlockPos findCrop(ServerLevel level) {
        BlockPos center = blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-8, -8, -8), center.offset(8, 2, 8))) {
            if (rules().forage().test(level.getBlockState(p))) return p.immutable();
        }
        return null;
    }

    private void feedOnCrops(ServerLevel level) {
        boolean any = false;
        for (int i = 0; i < 6; i++) {
            BlockPos p = knownCrops.offset(random.nextInt(9) - 4, random.nextInt(3) - 1, random.nextInt(9) - 4);
            BlockState state = level.getBlockState(p);
            if (rules().forage().test(state)) {
                any = true;
                fed = true;
                if (BoltingCropBlock.isBolted(state)) feast = true; // flowering crops draw more insects
                if (random.nextFloat() < 0.15F && Compat.bonemeal(level, p, state)) {
                    level.levelEvent(1505, p, 0);
                }
            } else if (state.is(Blocks.FARMLAND) && level.isEmptyBlock(p.above()) && random.nextFloat() < 0.05F) {
                // seeds from other farms
                List<Block> crops = level.registryAccess().lookupOrThrow(Registries.BLOCK).get(SEEDS_ON_FARMLAND)
                        .map(set -> set.stream().map(h -> h.value()).toList()).orElse(List.of());
                if (!crops.isEmpty()) {
                    Block crop = crops.get(random.nextInt(crops.size()));
                    BlockState planted = crop.defaultBlockState();
                    if (planted.canSurvive(level, p.above())) level.setBlockAndUpdate(p.above(), planted);
                }
            }
        }
        if (!any && random.nextInt(4) == 0) knownCrops = null; // the farm is gone
    }

    private LivingEntity nearbySpider(ServerLevel level) {
        List<LivingEntity> spiders = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6),
                e -> e.isAlive() && (Compat.isType(e, "spider") || Compat.isType(e, "cave_spider")));
        for (LivingEntity s : spiders) {
            boolean guarded = (roost != null && s.blockPosition().closerThan(roost, 16))
                    || (knownCrops != null && s.blockPosition().closerThan(knownCrops, 16));
            if (guarded) return s;
        }
        return null;
    }

    private void wander(ServerLevel level) {
        if (target == null || random.nextInt(30) == 0 || target.closerToCenterThan(position(), 2.0) || !level.isEmptyBlock(target)) {
            target = BlockPos.containing(getX() + random.nextInt(7) - random.nextInt(7), getY() + random.nextInt(6) - 2.0,
                    getZ() + random.nextInt(7) - random.nextInt(7));
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        var navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    private void steerTowards(BlockPos to) {
        if (to == null) return;
        boolean far = !to.closerToCenterThan(position(), 3.0);
        if (far && (path == null || !to.equals(pathTarget) || tickCount % 40 == 0)) {
            path = getNavigation().createPath(to, 1);
            pathTarget = to;
        }
        Vec3 aim = Vec3.atCenterOf(to);
        if (far && path != null && !path.isDone()) {
            Vec3 node = path.getNextEntityPos(this);
            if (node.distanceTo(position()) < 1.2) path.advance();
            if (!path.isDone()) aim = path.getNextEntityPos(this).add(0, 0.5, 0);
        }
        double dx = aim.x - getX(), dy = aim.y - 0.4 - getY(), dz = aim.z - getZ();
        Vec3 m = getDeltaMovement();
        Vec3 next = m.add((Math.signum(dx) * 0.5 - m.x) * 0.1, (Math.signum(dy) * 0.7 - m.y) * 0.1, (Math.signum(dz) * 0.5 - m.z) * 0.1);
        setDeltaMovement(next);
        float yaw = (float) (Mth.atan2(next.z, next.x) * 180.0 / Math.PI) - 90.0F;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
        zza = 0.5F;
    }

    private void checkRoost(ServerLevel level) {
        boolean roostOk = roost != null && rules().roost().test(level.getBlockState(roost))
                && !(level.getBlockEntity(roost) instanceof BatRoostBlockEntity r && r.isAbandoned(level));
        if (roostOk) return;
        BlockPos found = findRoost(level);
        if (found != null) {
            setRoost(found);
            return;
        }
        homelessTicks += 100;
        if (roost != null && homelessTicks >= HOMELESS_DEATH_TICKS) {
            // no roost left anywhere
            Compat.hurt(level, this, damageSources().starve(), getMaxHealth() * 2);
        }
    }

    private BlockPos findRoost(ServerLevel level) {
        BlockPos center = blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-ROOST_SEARCH, -16, -ROOST_SEARCH), center.offset(ROOST_SEARCH, 16, ROOST_SEARCH))) {
            if (!rules().roost().test(level.getBlockState(p))) continue;
            if (level.getBlockEntity(p) instanceof BatRoostBlockEntity r && r.isAbandoned(level)) continue;
            double d = p.distSqr(center);
            if (d < bestDist) {
                bestDist = d;
                best = p.immutable();
            }
        }
        return best;
    }

    private boolean isLure(ItemStack stack) {
        return rules().lure().test(stack);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isLure(stack) && breedCooldown == 0 && loveTicks == 0) {
            if (!level().isClientSide()) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                loveTicks = 600;
                level().broadcastEntityEvent(this, (byte) 18);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void tryBreed(ServerLevel level) {
        for (ColonyBat mate : level.getEntitiesOfClass(ColonyBat.class, getBoundingBox().inflate(3), b -> b != this && b.loveTicks > 0)) {
            //? if >=1.21.2 {
            ColonyBat pup = EcoEntities.COLONY_BAT.get().create(level, EntitySpawnReason.BREEDING);
            //?} else
            /*ColonyBat pup = EcoEntities.COLONY_BAT.get().create(level);*/
            if (pup == null) return;
            Compat.place(pup, getX(), getY(), getZ(), getYRot());
            pup.setRoost(roost != null ? roost : mate.roost);
            level.addFreshEntity(pup);
            loveTicks = mate.loveTicks = 0;
            breedCooldown = mate.breedCooldown = 6000;
            return;
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 18) {
            for (int i = 0; i < 5; i++) {
                level().addParticle(ParticleTypes.HEART, getRandomX(1.0), getRandomY() + 0.3, getRandomZ(1.0), 0, 0, 0);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    private static long pack(BlockPos pos) {
        return pos == null ? Long.MIN_VALUE : pos.asLong();
    }

    private static BlockPos unpack(long value) {
        return value == Long.MIN_VALUE ? null : BlockPos.of(value);
    }

    //? if >=1.21.6 {
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("Roost", pack(roost));
        output.putLong("KnownCrops", pack(knownCrops));
        output.putBoolean("Fed", fed);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        roost = unpack(input.getLongOr("Roost", Long.MIN_VALUE));
        knownCrops = unpack(input.getLongOr("KnownCrops", Long.MIN_VALUE));
        fed = input.getBooleanOr("Fed", false);
    }
    //?} else {
    /*@Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("Roost", pack(roost));
        tag.putLong("KnownCrops", pack(knownCrops));
        tag.putBoolean("Fed", fed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        roost = tag.contains("Roost") ? unpack(tag.getLong("Roost")) : null;
        knownCrops = tag.contains("KnownCrops") ? unpack(tag.getLong("KnownCrops")) : null;
        fed = tag.getBoolean("Fed");
    }
    *///?}
}
