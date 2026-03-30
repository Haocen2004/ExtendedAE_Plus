package com.extendedae_plus.hooks;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 复刻 Fabric 的内置模型注册能力（与 AE2/MAE2 相同实现）。
 * 1.19.2 Forge: 使用 IUnbakedGeometry 而非 UnbakedModel。
 */
public final class BuiltInModelHooks {
    private static final Map<ResourceLocation, IUnbakedGeometry<?>> BUILTIN_MODELS = new ConcurrentHashMap<>();

    private BuiltInModelHooks() {}

    public static void addBuiltInModel(ResourceLocation id, IUnbakedGeometry<?> model) {
        var prev = BUILTIN_MODELS.putIfAbsent(id, model);
        if (prev != null) {
            throw new IllegalStateException("Duplicate built-in model ID: " + id);
        }
    }

    public static IUnbakedGeometry<?> getBuiltInModel(ResourceLocation id) {
        return BUILTIN_MODELS.get(id);
    }
}
