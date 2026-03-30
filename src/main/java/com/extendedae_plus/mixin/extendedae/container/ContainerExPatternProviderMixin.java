package com.extendedae_plus.mixin.extendedae.container;

import appeng.helpers.iface.PatternProviderLogicHost;
import appeng.menu.implementations.PatternProviderMenu;
import com.github.glodblock.epp.container.ContainerExPatternProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin into EPP's ContainerExPatternProvider.
 * In 1.19.2, the multiply/divide logic is handled by PatternScaleC2SPacket
 * instead of the glodium IActionHolder/CGenericPacket system.
 */
@Mixin(value = ContainerExPatternProvider.class, priority = 3000)
public abstract class ContainerExPatternProviderMixin extends PatternProviderMenu {

    public ContainerExPatternProviderMixin(MenuType<? extends PatternProviderMenu> menuType, int id, Inventory playerInventory, PatternProviderLogicHost host) {
        super(menuType, id, playerInventory, host);
    }
}
