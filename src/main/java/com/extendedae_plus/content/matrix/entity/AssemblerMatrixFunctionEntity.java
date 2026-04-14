package com.extendedae_plus.content.matrix.entity;

import com.extendedae_plus.content.matrix.cluster.AssemblerMatrixCluster;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AssemblerMatrixFunctionEntity extends AssemblerMatrixBaseEntity {

    public AssemblerMatrixFunctionEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.getMainNode().setIdlePowerUsage(1);
    }

    public abstract void add(AssemblerMatrixCluster c);
}
