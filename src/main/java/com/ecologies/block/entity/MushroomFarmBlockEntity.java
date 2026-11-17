package com.ecologies.block.entity;

import com.ecologies.block.MushroomFarmBlock;
import com.ecologies.data.EcoDataTypes;
import com.ecologies.data.MushroomSpecies;
import com.ecologies.registry.EcoBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
//? if >=1.21.6 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?}

public class MushroomFarmBlockEntity extends BlockEntity {
    private Identifier species;
    private Identifier planted;
    private int progress;
    private BlockState display = Blocks.AIR.defaultBlockState();

    public MushroomFarmBlockEntity(BlockPos pos, BlockState state) {
        super(EcoBlockEntities.MUSHROOM_FARM.get(), pos, state);
    }

    public BlockState display() {
        return display;
    }

    private MushroomSpecies speciesData() {
        return species == null ? null : EcoDataTypes.MUSHROOM_FARM.get(species);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MushroomFarmBlockEntity farm) {
        if (state.getValue(MushroomFarmBlock.STAGE) != MushroomFarmBlock.GROWING) return;
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        MushroomSpecies data = farm.speciesData();
        if (data == null) return;  // species got taken out of the datapack, so it stays put until it's back
        farm.progress += 20;
        farm.setChanged();
        farm.updateLook(data);
    }

    private void updateLook(MushroomSpecies data) {
        BlockState state = getBlockState();
        if (state.getValue(MushroomFarmBlock.STAGE) != MushroomFarmBlock.GROWING) return;
        BlockState next = progress >= data.growthTime()
                ? state.setValue(MushroomFarmBlock.STAGE, MushroomFarmBlock.FULL).setValue(MushroomFarmBlock.FILL, 7)
                : state.setValue(MushroomFarmBlock.FILL, 1 + Math.min(5, progress * 6 / Math.max(1, data.growthTime())));
        if (next != state) level.setBlock(worldPosition, next, Block.UPDATE_ALL);
    }

    public boolean tryPlant(ItemStack stack, Player player) {
        Map.Entry<Identifier, MushroomSpecies> entry = EcoDataTypes.speciesFor(stack);
        if (entry == null) return false;
        species = entry.getKey();
        planted = BuiltInRegistries.ITEM.getKey(stack.getItem());
        display = entry.getValue().displayState();
        progress = 0;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.setBlock(worldPosition, getBlockState().setValue(MushroomFarmBlock.STAGE, MushroomFarmBlock.GROWING)
                .setValue(MushroomFarmBlock.FILL, 1), Block.UPDATE_ALL);
        level.playSound(null, worldPosition, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        sync();
        return true;
    }

    public boolean tryFeed(ItemStack stack, Player player) {
        MushroomSpecies data = speciesData();
        if (data == null || getBlockState().getValue(MushroomFarmBlock.STAGE) != MushroomFarmBlock.GROWING) return false;
        int ticks = data.accelerationFor(stack);
        if (ticks <= 0) return false;
        progress += ticks;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        updateLook(data);
        level.playSound(null, worldPosition, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05,
                    worldPosition.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.0);
        }
        setChanged();
        return true;
    }

    public boolean tryHarvest() {
        MushroomSpecies data = speciesData();
        if (data == null || getBlockState().getValue(MushroomFarmBlock.STAGE) != MushroomFarmBlock.FULL) return false;
        int count = data.minYield() + level.getRandom().nextInt(Math.max(1, data.maxYield() - data.minYield() + 1));
        Item result = data.resultItem();
        if (count > 0 && result != null) Block.popResource(level, worldPosition.above(), new ItemStack(result, count));
        for (MushroomSpecies.Extra extra : data.extras()) {
            if (level.getRandom().nextFloat() >= extra.chance()) continue;
            var item = BuiltInRegistries.ITEM.getOptional(extra.item());
            int n = extra.min() + level.getRandom().nextInt(Math.max(1, extra.max() - extra.min() + 1));
            if (item.isPresent() && n > 0) Block.popResource(level, worldPosition.above(), new ItemStack(item.get(), n));
        }
        progress = 0;
        level.setBlock(worldPosition, getBlockState().setValue(MushroomFarmBlock.STAGE, MushroomFarmBlock.GROWING)
                .setValue(MushroomFarmBlock.FILL, 1), Block.UPDATE_ALL);
        level.playSound(null, worldPosition, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        sync();
        return true;
    }

    public boolean tryRemove() {
        if (species == null) return false;
        dropPlanted();
        species = null;
        planted = null;
        progress = 0;
        display = Blocks.AIR.defaultBlockState();
        level.setBlock(worldPosition, getBlockState().setValue(MushroomFarmBlock.STAGE, MushroomFarmBlock.EMPTY)
                .setValue(MushroomFarmBlock.FILL, 0), Block.UPDATE_ALL);
        sync();
        return true;
    }

    public void dropPlanted() {
        if (planted == null || level == null) return;
        BuiltInRegistries.ITEM.getOptional(planted)
                .ifPresent(item -> Block.popResource(level, worldPosition.above(), new ItemStack(item)));
        planted = null;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    //? if >=1.21.6 {
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        dropPlanted();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (species != null) output.putString("species", species.toString());
        if (planted != null) output.putString("planted", planted.toString());
        output.putInt("progress", progress);
        output.store("display", BlockState.CODEC, display);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        species = input.getString("species").map(Identifier::tryParse).orElse(null);
        planted = input.getString("planted").map(Identifier::tryParse).orElse(null);
        progress = input.getIntOr("progress", 0);
        display = input.read("display", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (species != null) tag.putString("species", species.toString());
        if (planted != null) tag.putString("planted", planted.toString());
        tag.putInt("progress", progress);
        tag.put("display", NbtUtils.writeBlockState(display));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        species = tag.contains("species") ? Identifier.tryParse(tag.getString("species")) : null;
        planted = tag.contains("planted") ? Identifier.tryParse(tag.getString("planted")) : null;
        progress = tag.getInt("progress");
        display = tag.contains("display")
                ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("display"))
                : Blocks.AIR.defaultBlockState();
    }
    *///?}

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
