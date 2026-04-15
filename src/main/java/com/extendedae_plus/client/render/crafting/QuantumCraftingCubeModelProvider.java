package com.extendedae_plus.client.render.crafting;

import appeng.client.render.crafting.AbstractCraftingUnitModelProvider;
import com.extendedae_plus.ExtendedAEPlus;
import com.extendedae_plus.content.quantum.QuantumCraftingUnitType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class QuantumCraftingCubeModelProvider extends AbstractCraftingUnitModelProvider<QuantumCraftingUnitType> {

    private static final List<Material> MATERIALS = new ArrayList<>();

    protected static final Material STRUCTURE_FORMED_FACE = texture("quantum_structure_formed_face");
    protected static final Material STRUCTURE_FORMED_SIDES = texture("quantum_structure_formed_sides");
    protected static final Material STRUCTURE_ANIMATION_SIDES = texture("quantum_structure_powered_sides");

    protected static final Material INTERNAL_FORMED_FACE = texture("quantum_internal_formed_face");
    protected static final Material INTERNAL_FORMED_SIDES = texture("quantum_internal_formed_sides");
    protected static final Material INTERNAL_ANIMATION_SIDES = texture("quantum_internal_powered_sides");

    protected static final Material INTERNAL_ANIMATION_FACE = texture("quantum_internal_powered_animation");
    protected static final Material INTERNAL_ANIMATION_FACE_TB = texture("quantum_internal_powered_animation_tb");

    private static final HashMap<QuantumCraftingUnitType, Material> NON_STRUCTURE_BASE = new HashMap<>();

    static {
        for (var type : QuantumCraftingUnitType.values()) {
            if (type != QuantumCraftingUnitType.STRUCTURE) {
                NON_STRUCTURE_BASE.put(type, texture(type.getRegistryName()));
            }
        }
    }

    public QuantumCraftingCubeModelProvider(QuantumCraftingUnitType type) {
        super(type);
    }

    @Override
    public List<Material> getMaterials() {
        return Collections.unmodifiableList(MATERIALS);
    }

    @Override
    public BakedModel getBakedModel(Function<Material, TextureAtlasSprite> spriteGetter) {
        if (type == QuantumCraftingUnitType.STRUCTURE) {
            return new QuantumComputerStructureBakedModel(
                    spriteGetter.apply(STRUCTURE_FORMED_FACE),
                    spriteGetter.apply(STRUCTURE_FORMED_SIDES),
                    spriteGetter.apply(STRUCTURE_ANIMATION_SIDES));
        }

        var baseFace = spriteGetter.apply(NON_STRUCTURE_BASE.get(type));
        return new QuantumComputerInternalBakedModel(
                baseFace,
                spriteGetter.apply(INTERNAL_FORMED_SIDES),
                spriteGetter.apply(INTERNAL_ANIMATION_SIDES),
                spriteGetter.apply(INTERNAL_ANIMATION_FACE),
                spriteGetter.apply(INTERNAL_ANIMATION_FACE_TB),
                spriteGetter.apply(INTERNAL_ANIMATION_FACE_TB));
    }

    private static Material texture(String name) {
        var material = new Material(TextureAtlas.LOCATION_BLOCKS,
                new ResourceLocation(ExtendedAEPlus.MODID, "block/crafting/" + name));
        MATERIALS.add(material);
        return material;
    }
}
