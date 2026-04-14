package com.extendedae_plus.content.matrix.block;

import com.extendedae_plus.content.matrix.entity.AssemblerMatrixSpeedEntity;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.world.item.Item;

public class AssemblerMatrixSpeedBlock extends AssemblerMatrixBaseBlock<AssemblerMatrixSpeedEntity> {

    @Override
    public Item getPresentItem() {
        return ModBlocks.ASSEMBLER_MATRIX_SPEED.get().asItem();
    }
}
