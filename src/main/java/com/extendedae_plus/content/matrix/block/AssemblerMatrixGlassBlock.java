package com.extendedae_plus.content.matrix.block;

import com.extendedae_plus.content.matrix.entity.AssemblerMatrixGlassEntity;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AssemblerMatrixGlassBlock extends AssemblerMatrixBaseBlock<AssemblerMatrixGlassEntity> {

    public AssemblerMatrixGlassBlock() {
        super(Properties.of(net.minecraft.world.level.material.Material.METAL)
                .strength(2F, 6.0F).requiresCorrectToolForDrops().noOcclusion());
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
