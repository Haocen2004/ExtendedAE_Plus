package com.extendedae_plus.hooks;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 复刻 AE2 的内置模型注册能力。
 * 1.19.2: 使用 UnbakedModel（与 AE2 12.9.12 的 BuiltInModelHooks 相同）。
 */
public final class BuiltInModelHooks {
    private static final Map<ResourceLocation, UnbakedModel> BUILTIN_MODELS = new HashMap<>();

    private BuiltInModelHooks() {}

    public static void addBuiltInModel(ResourceLocation id, UnbakedModel model) {
        if (BUILTIN_MODELS.put(id, model) != null) {
            throw new IllegalStateException("Duplicate built-in model ID: " + id);
        }
    }

    @Nullable
    public static UnbakedModel getBuiltInModel(ResourceLocation id) {
        return BUILTIN_MODELS.get(id);
    }
}
