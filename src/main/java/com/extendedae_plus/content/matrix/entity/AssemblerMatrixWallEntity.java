package com.extendedae_plus.content.matrix.entity;

import com.extendedae_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AssemblerMatrixWallEntity extends AssemblerMatrixBaseEntity {
    public AssemblerMatrixWallEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ASSEMBLER_MATRIX_WALL.get(), pos, blockState);
    }

    protected AssemblerMatrixWallEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }
}
