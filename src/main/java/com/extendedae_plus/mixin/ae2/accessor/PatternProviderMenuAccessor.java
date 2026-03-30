package com.extendedae_plus.mixin.ae2.accessor;

import appeng.helpers.iface.PatternProviderLogic;
import appeng.menu.implementations.PatternProviderMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PatternProviderMenu.class)
public interface PatternProviderMenuAccessor {
    @Accessor(value = "logic", remap = false)
    PatternProviderLogic eap$logic();
}
