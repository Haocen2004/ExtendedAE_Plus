package com.extendedae_plus.content.matrix.entity;

import com.extendedae_plus.content.matrix.cluster.AssemblerMatrixCluster;
import com.extendedae_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AssemblerMatrixSpeedEntity extends AssemblerMatrixFunctionEntity {
    public AssemblerMatrixSpeedEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ASSEMBLER_MATRIX_SPEED.get(), pos, blockState);
    }

    @Override
    public void add(AssemblerMatrixCluster c) {
        c.addSpeedCore();
    }
}
