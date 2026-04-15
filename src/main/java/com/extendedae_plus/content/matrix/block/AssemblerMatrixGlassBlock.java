package com.extendedae_plus.content.matrix.block;

import com.extendedae_plus.content.matrix.entity.AssemblerMatrixGlassEntity;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class AssemblerMatrixGlassBlock extends AssemblerMatrixBaseBlock<AssemblerMatrixGlassEntity> {

    public AssemblerMatrixGlassBlock() {
        super(Properties.of(net.minecraft.world.level.material.Material.METAL)
                .strength(2F, 6.0F).requiresCorrectToolForDrops().noOcclusion()
                .isViewBlocking((a, b, c) -> false));
    }

    @Override
    public float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull net.minecraft.core.BlockPos pos) {
        return 1.0f;
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull net.minecraft.core.BlockPos pos) {
        return true;
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter level,
            @NotNull net.minecraft.core.BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public Item getPresentItem() {
        return ModBlocks.ASSEMBLER_MATRIX_GLASS.get().asItem();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean skipRendering(@NotNull BlockState state, @NotNull BlockState adjacentState, @NotNull Direction direction) {
        return adjacentState.getBlock() instanceof AssemblerMatrixGlassBlock || super.skipRendering(state, adjacentState, direction);
    }
}
