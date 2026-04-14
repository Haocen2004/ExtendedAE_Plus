package com.extendedae_plus.content.matrix.block;

import com.extendedae_plus.content.matrix.entity.AssemblerMatrixWallEntity;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.world.item.Item;

public class AssemblerMatrixWallBlock extends AssemblerMatrixBaseBlock<AssemblerMatrixWallEntity> {

    @Override
    public Item getPresentItem() {
        return ModBlocks.ASSEMBLER_MATRIX_WALL.get().asItem();
    }
}
