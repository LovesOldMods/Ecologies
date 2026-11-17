package com.ecologies.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
//? if >=1.21.2 {
import net.minecraft.world.level.ScheduledTickAccess;
//?}

public class SkinnyLogBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock, Strippable {
    public enum Arm implements StringRepresentable {
        NONE, END, JOINED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    // keep these relative to the axis, six direction properties was 24k states and froze world loading
    public static final EnumProperty<Arm> PLUS = EnumProperty.create("plus", Arm.class, Arm.NONE, Arm.END, Arm.JOINED);
    public static final EnumProperty<Arm> MINUS = EnumProperty.create("minus", Arm.class, Arm.NONE, Arm.END, Arm.JOINED);
    @SuppressWarnings("unchecked")
    public static final EnumProperty<Arm>[] SIDES = new EnumProperty[] {
            EnumProperty.create("side_1", Arm.class, Arm.NONE, Arm.JOINED),
            EnumProperty.create("side_2", Arm.class, Arm.NONE, Arm.JOINED),
            EnumProperty.create("side_3", Arm.class, Arm.NONE, Arm.JOINED),
            EnumProperty.create("side_4", Arm.class, Arm.NONE, Arm.JOINED)};
    // goes leafy when 3+ neighbours are leaves
    public static final BooleanProperty LEAFY = BooleanProperty.create("leafy");
    private static final Direction[] Y_SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final Direction[] X_SIDES = {Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH};
    private static final Direction[] Z_SIDES = {Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST};
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // keep this matching tools/gen_tundra.py
    public static Direction[] sides(Direction.Axis axis) {
        return axis == Direction.Axis.Y ? Y_SIDES : axis == Direction.Axis.X ? X_SIDES : Z_SIDES;
    }

    public static EnumProperty<Arm> property(Direction.Axis axis, Direction side) {
        if (side.getAxis() == axis) return side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? PLUS : MINUS;
        Direction[] around = sides(axis);
        for (int i = 0; i < 4; i++) if (around[i] == side) return SIDES[i];
        throw new IllegalArgumentException(side + " on " + axis);
    }

    public static Arm arm(BlockState state, Direction side) {
        return state.getValue(property(state.getValue(AXIS), side));
    }

    private final int width;
    private final Supplier<? extends Block> stripped;
    private final VoxelShape core;
    private final Map<Direction, VoxelShape> armShapes = new EnumMap<>(Direction.class);
    // Cached per arm combo (64).
    private final VoxelShape[] shapes = new VoxelShape[64];

    public SkinnyLogBlock(Properties properties, int width, Supplier<? extends Block> stripped) {
        super(properties);
        this.width = width;
        this.stripped = stripped;
        double a = 8 - width / 2.0, b = 8 + width / 2.0;
        core = Block.box(a, a, a, b, b, b);
        armShapes.put(Direction.DOWN, Block.box(a, 0, a, b, a, b));
        armShapes.put(Direction.UP, Block.box(a, b, a, b, 16, b));
        armShapes.put(Direction.NORTH, Block.box(a, a, 0, b, b, a));
        armShapes.put(Direction.SOUTH, Block.box(a, a, b, b, b, 16));
        armShapes.put(Direction.WEST, Block.box(0, a, a, a, b, b));
        armShapes.put(Direction.EAST, Block.box(b, a, a, 16, b, b));
        BlockState d = stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(WATERLOGGED, false);
        for (EnumProperty<Arm> p : SIDES) d = d.setValue(p, Arm.NONE);
        registerDefaultState(d.setValue(PLUS, Arm.END).setValue(MINUS, Arm.END).setValue(LEAFY, false));
    }

    public int width() {
        return width;
    }

    @Override
    public Block stripped() {
        return stripped == null ? null : stripped.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLUS, MINUS, SIDES[0], SIDES[1], SIDES[2], SIDES[3], WATERLOGGED, LEAFY);
    }

    public static BlockState shape(BlockGetter level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof SkinnyLogBlock)) return state;
        Map<Direction, Arm> arms = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) {
            arms.put(d, level.getBlockState(pos.relative(d)).getBlock() instanceof SkinnyLogBlock ? Arm.JOINED : Arm.NONE);
        }
        Direction.Axis axis = state.getValue(AXIS);
        Direction plus = Direction.get(Direction.AxisDirection.POSITIVE, axis), minus = plus.getOpposite();
        boolean sideJoins = false;
        for (Direction d : Direction.values()) if (d.getAxis() != axis && arms.get(d) == Arm.JOINED) sideJoins = true;
        boolean a = arms.get(plus) == Arm.JOINED, b = arms.get(minus) == Arm.JOINED;
        if ((!a && !b) || ((a ^ b) && !sideJoins)) {
            if (!a) arms.put(plus, Arm.END);
            if (!b) arms.put(minus, Arm.END);
        }
        int leaves = 0;
        for (Direction d : Direction.values()) {
            state = state.setValue(property(axis, d), arms.get(d));
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.is(BlockTags.LEAVES) || (n.getBlock() instanceof SkinnyLogBlock && n.getValue(LEAFY))) leaves++;
        }
        return state.setValue(LEAFY, leaves > 2);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        return shape(context.getLevel(), context.getClickedPos(), state);
    }

    //? if >=1.21.2 {
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return shape(level, pos, state);
    }
    //?} else {
    /*@Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return shape(level, pos, state);
    }
    *///?}

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction d : Direction.values()) {
            Arm kind = arm(state, d);
            if (kind == Arm.END || kind == Arm.JOINED) mask |= 1 << d.ordinal();
        }
        VoxelShape shape = shapes[mask];
        if (shape == null) {
            shape = core;
            for (Direction d : Direction.values()) if ((mask & 1 << d.ordinal()) != 0) shape = Shapes.or(shape, armShapes.get(d));
            shapes[mask] = shape.optimize();
        }
        return shapes[mask];
    }
}
