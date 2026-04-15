package com.extendedae_plus.content.quantum.screen;

import com.extendedae_plus.content.quantum.menu.QuantumComputerMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.style.ScreenStyle;

/**
 * Screen for Quantum Computer - displays CPU crafting status.
 * Extends CraftingCPUScreen to reuse AE2's UI logic.
 */
public class QuantumComputerScreen extends CraftingCPUScreen<QuantumComputerMenu> {

    public QuantumComputerScreen(QuantumComputerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}
