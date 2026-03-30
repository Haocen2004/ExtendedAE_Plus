package com.extendedae_plus.mixin.ae2.menu;

import appeng.helpers.iface.PatternProviderLogic;
import appeng.helpers.iface.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.PatternProviderMenu;
import com.extendedae_plus.api.advancedBlocking.IAdvancedBlocking;
import com.extendedae_plus.api.advancedBlocking.IPatternProviderMenuAdvancedSync;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.extendedae_plus.util.Logger.EAP$LOGGER;

@Mixin(PatternProviderMenu.class)
public abstract class PatternProviderMenuAdvancedMixin implements IPatternProviderMenuAdvancedSync {
    @Final
    @Shadow(remap = false)
    protected PatternProviderLogic logic;

    // 选择一个未占用�?GUI 同步 id（AE2 已用�?7），这里使用 20 以避冲突
    @Unique
    @GuiSync(20)
    public boolean eap$AdvancedBlocking = false;

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void eap$syncAdvancedBlocking(CallbackInfo ci) {
        // 避免@Shadow父类方法，改用公共API：AEBaseMenu#isClientSide()
        if (!((AEBaseMenu) (Object) this).isClientSide()) {
            var l = this.logic;
            if (l instanceof IAdvancedBlocking holder) {
                this.eap$AdvancedBlocking = holder.eap$getAdvancedBlocking();
            }
        }
    }

    // 构造器尾注入（public ctor�?
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lappeng/helpers/iface/PatternProviderLogicHost;)V", at = @At("TAIL"), remap = false)
    private void eap$initAdvancedSync_Public(int id, Inventory playerInventory, PatternProviderLogicHost host, CallbackInfo ci) {
        try {
            var l = this.logic;
            if (l instanceof IAdvancedBlocking holder) {
                this.eap$AdvancedBlocking = holder.eap$getAdvancedBlocking();
            }
        } catch (Throwable ignored) {}
    }

    // 构造器尾注入（protected ctor with MenuType）- AE2 12.9.12 无此构造函数，已移除

    @Override
    public boolean eap$getAdvancedBlockingSynced() {
        return this.eap$AdvancedBlocking;
    }
}
