package com.extendedae_plus.content.matrix;

import appeng.blockentity.AEBaseBlockEntity;
import com.extendedae_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PatternCorePlusBlockEntity extends AEBaseBlockEntity {
    public PatternCorePlusBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PatternCorePlusBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ASSEMBLER_MATRIX_PATTERN_PLUS_BE.get(), pos, state);
    }
}
