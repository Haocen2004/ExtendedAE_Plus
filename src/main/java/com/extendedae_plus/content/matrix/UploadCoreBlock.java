package com.extendedae_plus.content.matrix;

import appeng.block.AEBaseEntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public class UploadCoreBlock extends AEBaseEntityBlock<UploadCoreBlockEntity> {
    public UploadCoreBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL).strength(2F, 6.0F));
    }
}
