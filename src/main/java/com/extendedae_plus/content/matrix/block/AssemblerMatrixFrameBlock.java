package com.extendedae_plus.content.matrix.block;

import com.extendedae_plus.content.matrix.entity.AssemblerMatrixFrameEntity;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssemblerMatrixFrameBlock extends AssemblerMatrixBaseBlock<AssemblerMatrixFrameEntity> {

    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

    public AssemblerMatrixFrameBlock() {
        super();
        this.registerDefaultState(defaultBlockState().setValue(SHAPE, Shape.BLOCK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SHAPE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getShapeType(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos facingPos) {
        var base = super.updateShape(state, facing, facingState, level, pos, facingPos);
        return getShapeType(base, level, pos);
    }

    @Override
    public Item getPresentItem() {
        return ModBlocks.ASSEMBLER_MATRIX_FRAME.get().asItem();
    }

    private BlockState getShapeType(BlockState baseState, LevelAccessor level, BlockPos pos) {
        var type = Shape.BLOCK;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        final boolean xx = isFrame(level, x - 1, y, z) && isFrame(level, x + 1, y, z);
        final boolean yy = isFrame(level, x, y - 1, z) && isFrame(level, x, y + 1, z);
        final boolean zz = isFrame(level, x, y, z - 1) && isFrame(level, x, y, z + 1);

        if (xx && !yy && !zz) {
            type = Shape.COLUMN_X;
        } else if (!xx && yy && !zz) {
            type = Shape.COLUMN_Y;
        } else if (!xx && !yy && zz) {
            type = Shape.COLUMN_Z;
        }
        return baseState.setValue(SHAPE, type);
    }

    private static boolean isFrame(LevelAccessor level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof AssemblerMatrixFrameBlock;
    }

    public enum Shape implements StringRepresentable {
        BLOCK("block"),
        COLUMN_X("column_x"),
        COLUMN_Y("column_y"),
        COLUMN_Z("column_z");

        private final String name;

        Shape(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }
    }
}
