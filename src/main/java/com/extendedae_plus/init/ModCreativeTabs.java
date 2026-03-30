package com.extendedae_plus.init;

import com.extendedae_plus.ExtendedAEPlus;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * MC 1.19.2 风格的创造模式标签页。
 * 物品由 {@link ModItems} 通过 {@code Item.Properties().tab(MAIN_TAB)} 加入。
 */
public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final CreativeModeTab MAIN_TAB = new CreativeModeTab(ExtendedAEPlus.MODID + ".main") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.WIRELESS_TRANSCEIVER.get());
        }
    };
}
