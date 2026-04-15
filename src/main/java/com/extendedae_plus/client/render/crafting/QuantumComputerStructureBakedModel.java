package com.extendedae_plus.client.render.crafting;

import com.extendedae_plus.content.quantum.QuantumCraftingUnitType;
import com.extendedae_plus.content.quantum.block.QuantumCraftingUnitBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class QuantumComputerStructureBakedModel extends ConnectedTexturesBaseBakedModel {

    public QuantumComputerStructureBakedModel(
            TextureAtlasSprite face,
            TextureAtlasSprite sides,
            TextureAtlasSprite poweredSides) {
        super(RenderType.translucent(), RenderType.cutout(), face, sides, poweredSides);
        setSideEmissive(true);
        setRenderOppositeSide(true);
    }

    @Override
    protected boolean shouldConnect(Block block) {
        return block instanceof QuantumCraftingUnitBlock unit && unit.type == QuantumCraftingUnitType.STRUCTURE;
    }

    @Override
    protected boolean shouldBeEmissive(BlockState state) {
        return state.hasProperty(QuantumCraftingUnitBlock.POWERED)
                && state.getValue(QuantumCraftingUnitBlock.POWERED);
    }
}
