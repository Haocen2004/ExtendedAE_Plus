package com.extendedae_plus.content.matrix;

import appeng.blockentity.AEBaseBlockEntity;
import com.extendedae_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class UploadCoreBlockEntity extends AEBaseBlockEntity {
    public UploadCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public UploadCoreBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.UPLOAD_CORE_BE.get(), pos, state);
    }
}
