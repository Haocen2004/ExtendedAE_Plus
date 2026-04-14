package com.extendedae_plus.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

public class AssemblerGlassModel implements IUnbakedGeometry<AssemblerGlassModel> {

    @Override
    public @NotNull BakedModel bake(@NotNull IGeometryBakingContext context, @NotNull ModelBakery bakery, @NotNull Function<Material, TextureAtlasSprite> spriteGetter, @NotNull ModelState modelState, @NotNull ItemOverrides overrides, @NotNull ResourceLocation modelLocation) {
        return new AssemblerGlassBakedModel(spriteGetter);
    }

    @Override
    public Collection<Material> getMaterials(@NotNull IGeometryBakingContext context, @NotNull Function<ResourceLocation, UnbakedModel> modelGetter, @NotNull Set<Pair<String, String>> missingTextureErrors) {
        return AssemblerGlassBakedModel.getAllMaterials();
    }

    public static class Loader implements IGeometryLoader<AssemblerGlassModel> {

        @Override
        public @NotNull AssemblerGlassModel read(@NotNull JsonObject jsonObject, @NotNull JsonDeserializationContext deserializationContext) throws JsonParseException {
            return new AssemblerGlassModel();
        }
    }
}
