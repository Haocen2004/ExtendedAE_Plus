package com.extendedae_plus.mixin.ae2.autopattern;

import appeng.api.crafting.IPatternDetails;
import appeng.helpers.iface.PatternProviderLogic;
import com.extendedae_plus.api.crafting.ScaledProcessingPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**适配
 * Redirect PatternProviderLogic.pushPattern 中对 List.contains 的调用，
 * Fallback to match original pattern instance when encountering scaled patterns.
 */
@Mixin(value = PatternProviderLogic.class, remap = false)
public class PatternProviderLogicContainsRedirectMixin {

    @Redirect(method = "pushPattern",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;contains(Ljava/lang/Object;)Z")
    )
    private boolean eap$patternsContains(List<?> list, Object o) {
        try {
            if (o instanceof ScaledProcessingPattern scaled) {
                IPatternDetails base = scaled.getOriginal();
                if (base != null && list.indexOf(base) != -1) {
                    return true;
                }
            }
            // Use indexOf to avoid re-triggering List.contains redirect (prevent recursion)
            return list.indexOf(o) != -1;
        } catch (Throwable t) {
            return list.indexOf(o) != -1;
        }
    }
}