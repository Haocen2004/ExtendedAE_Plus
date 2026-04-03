package com.extendedae_plus.mixin.ae2.compat;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.helpers.iface.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.PatternProviderMenu;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.extendedae_plus.util.Logger.EAP$LOGGER;

/**
 * PatternProviderMenu的兼容性Mixin
 * 优先级设置为500，低于appflux的默认优先级，避免冲�?
 */
@Mixin(value = PatternProviderMenu.class, priority = 500, remap = false)
public abstract class PatternProviderCompatMixin extends AEBaseMenu {

    public PatternProviderCompatMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lappeng/helpers/iface/PatternProviderLogicHost;)V",
            at = @At("TAIL"))
    private void eap$initCompatUpgrades(int id, Inventory playerInventory, PatternProviderLogicHost host, CallbackInfo ci) {
        eap$setupCompatUpgrades(host);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lappeng/helpers/iface/PatternProviderLogicHost;)V",
            at = @At("TAIL"))
    private void eap$initCompatUpgradesWithMenuType(MenuType<?> menuType, int id, Inventory playerInventory, PatternProviderLogicHost host, CallbackInfo ci) {
        eap$setupCompatUpgrades(host);
    }

    @Unique
    private void eap$setupCompatUpgrades(PatternProviderLogicHost host) {
        try {
            if (UpgradeSlotCompat.shouldManageLocalUpgradeInventory()) {
                if (host.getLogic() instanceof IUpgradeableObject upgradeableLogic) {
                    IUpgradeInventory upgrades = upgradeableLogic.getUpgrades();
                    this.setupUpgrades(upgrades);
                }
            }
        } catch (Exception e) {
            // 静默处理异常，确保不会因为升级功能导致崩�?
            EAP$LOGGER.error("PatternProviderMenu兼容性升级初始化失败", e);
        }
    }
}
