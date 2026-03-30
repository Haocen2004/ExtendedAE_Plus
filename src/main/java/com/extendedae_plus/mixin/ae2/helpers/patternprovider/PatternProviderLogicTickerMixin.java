package com.extendedae_plus.mixin.ae2.helpers.patternprovider;

import appeng.api.networking.ticking.TickRateModulation;
import appeng.helpers.iface.PatternProviderLogic;
import com.extendedae_plus.api.bridge.IInterfaceWirelessLinkBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * Inject into PatternProviderLogic.Ticker per-tick callback to drive wireless link state updates.
 */
@Mixin(targets = "appeng.helpers.iface.PatternProviderLogic$Ticker", remap = false)
public abstract class PatternProviderLogicTickerMixin {

    @Unique
    private static Field extendedae_plus$outerField;

    @Unique
    private PatternProviderLogic extendedae_plus$getOuterLogic() {
        try {
            if (extendedae_plus$outerField == null) {
                extendedae_plus$outerField = this.getClass().getDeclaredField("this$0");
                extendedae_plus$outerField.setAccessible(true);
            }
            return (PatternProviderLogic) extendedae_plus$outerField.get(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to access PatternProviderLogic outer instance", e);
        }
    }

    @Inject(method = "tickingRequest", at = @At("HEAD"))
    private void eap$tickHead(appeng.api.networking.IGridNode node, int ticksSinceLastCall,
                              CallbackInfoReturnable<appeng.api.networking.ticking.TickRateModulation> cir) {
        // ä»å¨æå¡ç«¯å¤çå»¶è¿åå§å
        if (node != null && node.getLevel() != null && node.getLevel().isClientSide) {
            return;
        }
        if (this.extendedae_plus$getOuterLogic() instanceof IInterfaceWirelessLinkBridge bridge) {
            bridge.eap$handleDelayedInit();
        }
    }

    @Inject(method = "tickingRequest", at = @At("TAIL"), cancellable = true)
    private void eap$tickTail(appeng.api.networking.IGridNode node, int ticksSinceLastCall,
                                          CallbackInfoReturnable<appeng.api.networking.ticking.TickRateModulation> cir) {
        if (node != null && node.getLevel() != null && node.getLevel().isClientSide) {
            return;
        }
        if (this.extendedae_plus$getOuterLogic() instanceof IInterfaceWirelessLinkBridge bridge) {
            bridge.eap$updateWirelessLink();
            if (bridge.eap$shouldKeepTicking() && cir.getReturnValue() == TickRateModulation.SLEEP) {
                cir.setReturnValue(TickRateModulation.SLOWER);
            }
        }
    }
}
