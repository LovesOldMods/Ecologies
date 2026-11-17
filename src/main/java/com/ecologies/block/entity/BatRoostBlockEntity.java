package com.ecologies.block.entity;

import com.ecologies.data.EcoDataTypes;
import com.ecologies.entity.ColonyBat;
import com.ecologies.registry.EcoBlockEntities;
import com.ecologies.registry.EcoEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//? if >=1.21.6 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?}

public class BatRoostBlockEntity extends BlockEntity {
    // Limits are in roost/colony_bat.json.

    private int harvested;
    private long lastUpdate;
    private long abandonedUntil;

    public BatRoostBlockEntity(BlockPos pos, BlockState state) {
        super(EcoBlockEntities.BAT_ROOST.get(), pos, state);
    }

    public boolean isAbandoned(Level level) {
        return level.getGameTime() < abandonedUntil;
    }

    public void recordHarvest(Level level, int layers) {
        var rules = EcoDataTypes.roostRules(EcoEntities.COLONY_BAT.get());
        long now = level.getGameTime();
        harvested = Math.max(0, harvested - (int) ((now - lastUpdate) / rules.recoveryTicksPerLayer()));
        lastUpdate = now;
        harvested += layers;
        if (harvested > rules.overharvestLimit()) {
            harvested = 0;
            abandonedUntil = now + rules.abandonTicks();
            for (ColonyBat bat : level.getEntitiesOfClass(ColonyBat.class, new AABB(worldPosition).inflate(64),
                    b -> worldPosition.equals(b.roost()))) {
                bat.setRoost(null);
            }
        }
        setChanged();
    }

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Harvested", harvested);
        output.putLong("LastUpdate", lastUpdate);
        output.putLong("AbandonedUntil", abandonedUntil);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        harvested = input.getIntOr("Harvested", 0);
        lastUpdate = input.getLongOr("LastUpdate", 0L);
        abandonedUntil = input.getLongOr("AbandonedUntil", 0L);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Harvested", harvested);
        tag.putLong("LastUpdate", lastUpdate);
        tag.putLong("AbandonedUntil", abandonedUntil);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        harvested = tag.getInt("Harvested");
        lastUpdate = tag.getLong("LastUpdate");
        abandonedUntil = tag.getLong("AbandonedUntil");
    }
    *///?}
}
