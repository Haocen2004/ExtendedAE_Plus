package com.extendedae_plus.content.matrix.block;

import com.extendedae_plus.content.matrix.entity.AssemblerMatrixCrafterEntity;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.world.item.Item;

public class AssemblerMatrixCrafterBlock extends AssemblerMatrixBaseBlock<AssemblerMatrixCrafterEntity> {

    @Override
    public Item getPresentItem() {
        return ModBlocks.ASSEMBLER_MATRIX_CRAFTER.get().asItem();
    }
}
