package com.ecologies.registry;

import com.ecologies.Compat;
import com.ecologies.Ecologies;
import com.ecologies.block.BatRoostBlock;
import com.ecologies.block.BoltingCropBlock;
import com.ecologies.block.ColoredMossBlock;
import com.ecologies.block.GuanoBlock;
import com.ecologies.block.GuanoLayerBlock;
import com.ecologies.block.MushroomFarmBlock;
import com.ecologies.block.ShrubBlock;
import com.ecologies.block.SkinnyLogBlock;
import com.ecologies.block.SproutedGlowLichenBlock;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public final class EcoBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Ecologies.MOD_ID);

    // BLOCK on 1.21.1, IMMOVEABLE on 26.x.
    //? if >=26.1 {
    private static final PushReaction IMMOVABLE = PushReaction.IMMOVEABLE;
    private static final PushReaction POPPED = PushReaction.POPPED;
    //?} else {
    /*private static final PushReaction IMMOVABLE = PushReaction.BLOCK;
    private static final PushReaction POPPED = PushReaction.DESTROY;
    *///?}

    public static final DeferredBlock<GuanoLayerBlock> GUANO_LAYER = register("guano_layer", GuanoLayerBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.1F)
                    .sound(SoundType.MUD).replaceable().forceSolidOff());

    public static final DeferredBlock<GuanoBlock> GUANO_BLOCK = register("guano_block", GuanoBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5F)
                    .sound(SoundType.MUD).randomTicks());

    public static final DeferredBlock<Block> AGED_GUANO_BLOCK = register("aged_guano_block", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.5F).sound(SoundType.MUD));

    public static final DeferredBlock<BatRoostBlock> BAT_ROOST = register("bat_roost", BatRoostBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(1.5F)
                    .sound(SoundType.DRIPSTONE_BLOCK).requiresCorrectToolForDrops().pushReaction(IMMOVABLE));

    public static final DeferredBlock<MushroomFarmBlock> MUSHROOM_FARM = register("mushroom_farm", MushroomFarmBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.6F).sound(SoundType.WOOD));

    public static final DeferredBlock<SproutedGlowLichenBlock> SPROUTED_GLOW_LICHEN = register("sprouted_glow_lichen", SproutedGlowLichenBlock::new,
            () -> Compat.noCollision(BlockBehaviour.Properties.of().mapColor(MapColor.GLOW_LICHEN).replaceable()).strength(0.2F)
                    .sound(SoundType.GLOW_LICHEN).lightLevel(GlowLichenBlock.emission(7)).ignitedByLava()
                    .pushReaction(POPPED).randomTicks());

    public static final DeferredBlock<BoltingCropBlock> BOLTING_WHEAT = bolting("bolting_wheat", () -> Blocks.WHEAT, () -> Items.WHEAT_SEEDS);
    public static final DeferredBlock<BoltingCropBlock> BOLTING_CARROTS = bolting("bolting_carrots", () -> Blocks.CARROTS, () -> Items.CARROT);
    public static final DeferredBlock<BoltingCropBlock> BOLTING_POTATOES = bolting("bolting_potatoes", () -> Blocks.POTATOES, () -> Items.POTATO);
    public static final DeferredBlock<BoltingCropBlock> BOLTING_BEETROOTS = bolting("bolting_beetroots", () -> Blocks.BEETROOTS, () -> Items.BEETROOT_SEEDS);

    private static DeferredBlock<BoltingCropBlock> bolting(String name, Supplier<Block> original, Supplier<ItemLike> seed) {
        return register(name, props -> new BoltingCropBlock(props, original, seed),
                () -> Compat.noCollision(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)).randomTicks().instabreak()
                        .sound(SoundType.CROP).pushReaction(POPPED));
    }

    // Red shrub sounds on 26.x, grass on 1.21.1.
    //? if >=26.1 {
    private static final SoundType SHRUB_SOUND = SoundType.RED_SHRUB;
    //?} else
    /*private static final SoundType SHRUB_SOUND = SoundType.GRASS;*/

    public static final DeferredBlock<Block> SMOOTH_BASALT_IRON_ORE = register("smooth_basalt_iron_ore", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F, 4.2F)
                    .sound(SoundType.BASALT).requiresCorrectToolForDrops());

    public static final DeferredBlock<SkinnyLogBlock> STRIPPED_DWARF_SPRUCE_LOG = register("stripped_dwarf_spruce_log",
            props -> new SkinnyLogBlock(props, 8, null), EcoBlocks::dwarfLog);
    public static final DeferredBlock<SkinnyLogBlock> DWARF_SPRUCE_LOG = register("dwarf_spruce_log",
            props -> new SkinnyLogBlock(props, 8, STRIPPED_DWARF_SPRUCE_LOG), EcoBlocks::dwarfLog);
    public static final DeferredBlock<SkinnyLogBlock> STRIPPED_DWARF_SPRUCE_LOG_POST_CROWN = register("stripped_dwarf_spruce_log_post_crown",
            props -> new SkinnyLogBlock(props, 4, null), EcoBlocks::dwarfLog);
    public static final DeferredBlock<SkinnyLogBlock> DWARF_SPRUCE_LOG_POST_CROWN = register("dwarf_spruce_log_post_crown",
            props -> new SkinnyLogBlock(props, 4, STRIPPED_DWARF_SPRUCE_LOG_POST_CROWN), EcoBlocks::dwarfLog);

    private static BlockBehaviour.Properties dwarfLog() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(2.0F).sound(SoundType.WOOD)
                .ignitedByLava().noOcclusion().forceSolidOff();
    }

    public static final DeferredBlock<SaplingBlock> DWARF_SPRUCE_SAPLING = register("dwarf_spruce_sapling",
            props -> new SaplingBlock(TreeGrower.SPRUCE, props),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SAPLING));

    public static final DeferredBlock<ShrubBlock> STUNTED_RED_SHRUB = shrub("stunted_red_shrub", MapColor.CRIMSON_NYLIUM);
    public static final DeferredBlock<ShrubBlock> ORANGE_SHRUB = shrub("orange_shrub", MapColor.COLOR_ORANGE);
    public static final DeferredBlock<ShrubBlock> YELLOW_SHRUB = shrub("yellow_shrub", MapColor.COLOR_YELLOW);
    //? if <26.1 {
    /*// 26.x has a vanilla red shrub; 1.21.1 gets ours (or whichever one's already registered, if something else adds it).
    public static final DeferredBlock<ShrubBlock> RED_SHRUB = shrub("red_shrub", MapColor.CRIMSON_NYLIUM);
    *///?}

    private static DeferredBlock<ShrubBlock> shrub(String name, MapColor color) {
        return register(name, ShrubBlock::new, () -> Compat.noCollision(BlockBehaviour.Properties.of().mapColor(color).replaceable())
                .instabreak().sound(SHRUB_SOUND).ignitedByLava().pushReaction(POPPED)
                .offsetType(BlockBehaviour.OffsetType.XZ));
    }

    public static final DeferredBlock<CarpetBlock> RED_MOSS_CARPET = mossCarpet("red_moss_carpet", MapColor.COLOR_RED);
    public static final DeferredBlock<CarpetBlock> ORANGE_MOSS_CARPET = mossCarpet("orange_moss_carpet", MapColor.COLOR_ORANGE);
    public static final DeferredBlock<CarpetBlock> YELLOW_MOSS_CARPET = mossCarpet("yellow_moss_carpet", MapColor.COLOR_YELLOW);
    public static final DeferredBlock<ColoredMossBlock> RED_MOSS_BLOCK = moss("red_moss_block", MapColor.COLOR_RED, RED_MOSS_CARPET);
    public static final DeferredBlock<ColoredMossBlock> ORANGE_MOSS_BLOCK = moss("orange_moss_block", MapColor.COLOR_ORANGE, ORANGE_MOSS_CARPET);
    public static final DeferredBlock<ColoredMossBlock> YELLOW_MOSS_BLOCK = moss("yellow_moss_block", MapColor.COLOR_YELLOW, YELLOW_MOSS_CARPET);

    private static DeferredBlock<ColoredMossBlock> moss(String name, MapColor color, Supplier<? extends Block> carpet) {
        return register(name, props -> new ColoredMossBlock(props, carpet), () -> BlockBehaviour.Properties.of().mapColor(color)
                .strength(0.1F).sound(SoundType.MOSS).pushReaction(POPPED));
    }

    private static DeferredBlock<CarpetBlock> mossCarpet(String name, MapColor color) {
        return register(name, CarpetBlock::new, () -> BlockBehaviour.Properties.of().mapColor(color)
                .strength(0.1F).sound(SoundType.MOSS_CARPET).pushReaction(POPPED));
    }

    public static final DeferredBlock<IceBlock> WHITE_ICE = register("white_ice", IceBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).friction(0.98F).randomTicks().strength(0.5F)
                    .sound(SoundType.GLASS));
    public static final DeferredBlock<Block> WHITE_PACKED_ICE = register("white_packed_ice", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).friction(0.98F).strength(0.5F)
                    .sound(SoundType.GLASS));

    private EcoBlocks() {}

    // 1.21.2+ wants the properties late since they carry the block id
    private static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, B> factory,
                                                             Supplier<BlockBehaviour.Properties> properties) {
        //? if >=1.21.2 {
        return BLOCKS.registerBlock(name, factory, properties);
        //?} else {
        /*return BLOCKS.registerBlock(name, factory, properties.get());
        *///?}
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
