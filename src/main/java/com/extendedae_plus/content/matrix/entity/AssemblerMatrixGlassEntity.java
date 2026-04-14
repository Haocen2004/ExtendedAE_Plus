package com.extendedae_plus.content.matrix.entity;

import com.extendedae_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AssemblerMatrixGlassEntity extends AssemblerMatrixWallEntity {
    public AssemblerMatrixGlassEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ASSEMBLER_MATRIX_GLASS.get(), pos, blockState);
    }
}
