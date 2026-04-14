package com.extendedae_plus.content.matrix.block;

import com.extendedae_plus.content.matrix.entity.AssemblerMatrixPatternEntity;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.world.item.Item;

public class AssemblerMatrixPatternBlock extends AssemblerMatrixBaseBlock<AssemblerMatrixPatternEntity> {

    @Override
    public Item getPresentItem() {
        return ModBlocks.ASSEMBLER_MATRIX_PATTERN.get().asItem();
    }
}
