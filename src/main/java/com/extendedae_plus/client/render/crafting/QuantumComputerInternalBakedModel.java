package com.extendedae_plus.client.render.crafting;

import com.extendedae_plus.content.quantum.QuantumCraftingUnitType;
import com.extendedae_plus.content.quantum.block.QuantumCraftingUnitBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;

public class QuantumComputerInternalBakedModel extends ConnectedTexturesBaseBakedModel {

    public QuantumComputerInternalBakedModel(
            TextureAtlasSprite face,
            TextureAtlasSprite side,
            TextureAtlasSprite poweredSides,
            TextureAtlasSprite faceAnimation,
            TextureAtlasSprite faceTopAnimation,
            TextureAtlasSprite faceBottomAnimation) {
        super(RenderType.cutout(), face, side, poweredSides);

        setSideEmissive(true);

        HashMap<Direction, TextureAtlasSprite> animationMap = new HashMap<>();
        animationMap.put(Direction.UP, faceTopAnimation);
        animationMap.put(Direction.DOWN, faceBottomAnimation);
        animationMap.put(Direction.NORTH, faceAnimation);
        animationMap.put(Direction.SOUTH, faceAnimation);
        animationMap.put(Direction.WEST, faceAnimation);
        animationMap.put(Direction.EAST, faceAnimation);
        setFaceAnimation(animationMap, true);
    }

    @Override
    protected boolean shouldConnect(Block block) {
        return block instanceof QuantumCraftingUnitBlock unit && unit.type != QuantumCraftingUnitType.STRUCTURE;
    }

    @Override
    protected boolean shouldBeEmissive(BlockState state) {
        return state.hasProperty(QuantumCraftingUnitBlock.POWERED)
                && state.getValue(QuantumCraftingUnitBlock.POWERED);
    }
}
