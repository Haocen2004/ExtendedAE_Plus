package com.extendedae_plus.mixin.ae2.accessor;

import appeng.crafting.execution.ElapsedTimeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ElapsedTimeTracker.class, remap = false)
public interface ElapsedTimeTrackerAccessor {

    @Invoker("decrementItems")
    void extendedae_plus$invokeDecrementItems(long items);
}
