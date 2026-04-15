package com.extendedae_plus.content.quantum.screen;

import com.extendedae_plus.content.quantum.menu.QuantumComputerMenu;
import com.extendedae_plus.util.Logger;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.config.CpuSelectionMode;
import appeng.api.config.Settings;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;

/**
 * Screen for Quantum Computer - displays CPU crafting status.
 * Extends CraftingCPUScreen to reuse AE2's UI logic.
 */
public class QuantumComputerScreen extends CraftingCPUScreen<QuantumComputerMenu> {

    private final SettingToggleButton<CpuSelectionMode> selectionMode;

    public QuantumComputerScreen(QuantumComputerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.selectionMode = new ServerSettingToggleButton<>(Settings.CPU_SELECTION_MODE, CpuSelectionMode.ANY);
        addToLeftToolbar(this.selectionMode);

        try {
            var scrollbar = widgets.addScrollBar("selectCpuScrollbar");
            widgets.add("selectCpuList", new AdvCpuSelectionList(menu, scrollbar, style));
        } catch (RuntimeException e) {
            // Fallback to vanilla CraftingCPUScreen layout if custom widget/style cannot be created.
            Logger.EAP$LOGGER.error("QuantumComputerScreen custom CPU list init failed, fallback to base screen", e);
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.selectionMode.set(this.menu.getSelectionMode());
    }

    @Override
    protected Component getGuiDisplayName(Component in) {
        return in;
    }
}
