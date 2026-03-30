package com.extendedae_plus.mixin.minecraft;

import com.extendedae_plus.hooks.BuiltInModelHooks;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 与 AE2 12.9.12 相同做法：拦截 getModel 方法，
 * 若内置模型表命中则直接返回，阻止继续查找 JSON 模型。
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {
    @Inject(method = "getModel", at = @At("HEAD"), cancellable = true)
    private void eap$getModelHook(ResourceLocation id, CallbackInfoReturnable<UnbakedModel> cir) {
        var model = BuiltInModelHooks.getBuiltInModel(id);
        if (model != null) {
            cir.setReturnValue(model);
        }
    }
}
