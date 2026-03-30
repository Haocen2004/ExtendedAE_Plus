package com.extendedae_plus.mixin.ae2.client.gui.patternProvider;

import appeng.api.config.YesNo;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.menu.implementations.PatternProviderMenu;
import com.extendedae_plus.api.IExPatternButton;
import com.extendedae_plus.api.advancedBlocking.IPatternProviderMenuAdvancedSync;
import com.extendedae_plus.api.smartDoubling.IPatternProviderMenuDoublingSync;
import com.extendedae_plus.init.ModNetwork;
import com.extendedae_plus.network.provider.SetPerProviderScalingLimitC2SPacket;
import com.extendedae_plus.network.provider.ToggleAdvancedBlockingC2SPacket;
import com.extendedae_plus.network.provider.ToggleSmartDoublingC2SPacket;
import com.extendedae_plus.util.GuiUtil;
import com.github.glodblock.epp.client.gui.GuiExPatternProvider;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static com.extendedae_plus.util.GuiUtil.createToggle;
import static com.extendedae_plus.util.Logger.EAP$LOGGER;

/**
 * Add "smart features" buttons to AE2's pattern provider screen:
 * - Advanced blocking mode toggle
 * - Smart doubling toggle
 * - Per-provider scaling limit input
 */
@Mixin(PatternProviderScreen.class)
public abstract class PatternProviderSmartFeaturesMixin<C extends PatternProviderMenu> extends AEBaseScreen<C> {

    @Unique private SettingToggleButton<YesNo> eap$AdvancedBlockingToggle;
    @Unique private SettingToggleButton<YesNo> eap$SmartDoublingToggle;
    @Unique private EditBox eap$PerProviderLimitInput;

    @Unique private boolean eap$AdvancedBlockingEnabled = false;
    @Unique private boolean eap$SmartDoublingEnabled = false;
    @Unique private int eap$PerProviderScalingLimit = 0;

    public PatternProviderSmartFeaturesMixin(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Unique
    private void eap$syncInitialState(C menu) {
        try {
            if (menu instanceof IPatternProviderMenuAdvancedSync sync) {
                this.eap$AdvancedBlockingEnabled = sync.eap$getAdvancedBlockingSynced();
            }
            if (menu instanceof IPatternProviderMenuDoublingSync sync) {
                this.eap$SmartDoublingEnabled = sync.eap$getSmartDoublingSynced();
                this.eap$PerProviderScalingLimit = sync.eap$getScalingLimit();
            }
        } catch (Throwable t) {
            EAP$LOGGER.error("Error initializing sync", t);
        }
    }

    @Unique
    private void eap$createWidgets() {
        this.eap$AdvancedBlockingToggle = createToggle(
                eap$AdvancedBlockingEnabled,
                () -> ModNetwork.CHANNEL.sendToServer(new ToggleAdvancedBlockingC2SPacket()),
                () -> {
                    var t = Component.translatable("extendedae_plus.gui.advanced_blocking.title");
                    var line = eap$AdvancedBlockingEnabled
                            ? Component.translatable("extendedae_plus.gui.advanced_blocking.enabled_desc")
                            : Component.translatable("extendedae_plus.gui.advanced_blocking.disabled_desc");
                    return List.of(t, line);
                }
        );
        this.eap$AdvancedBlockingToggle.set(eap$AdvancedBlockingEnabled ? YesNo.YES : YesNo.NO);
        this.addToLeftToolbar(this.eap$AdvancedBlockingToggle);

        this.eap$SmartDoublingToggle = createToggle(
                eap$SmartDoublingEnabled,
                () -> ModNetwork.CHANNEL.sendToServer(new ToggleSmartDoublingC2SPacket()),
                () -> {
                    var t = Component.translatable("extendedae_plus.gui.smart_doubling.title");
                    var line = eap$SmartDoublingEnabled
                            ? Component.translatable("extendedae_plus.gui.smart_doubling.enabled_desc")
                            : Component.translatable("extendedae_plus.gui.smart_doubling.disabled_desc");
                    return List.of(t, line);
                }
        );
        this.eap$SmartDoublingToggle.set(eap$SmartDoublingEnabled ? YesNo.YES : YesNo.NO);
        this.addToLeftToolbar(this.eap$SmartDoublingToggle);

        this.eap$PerProviderLimitInput = GuiUtil.createPerProviderLimitInput(this.font, this.eap$PerProviderScalingLimit, limit -> {
            this.eap$PerProviderScalingLimit = limit;
            ModNetwork.CHANNEL.sendToServer(new SetPerProviderScalingLimitC2SPacket(limit));
        });
        this.addRenderableWidget(this.eap$PerProviderLimitInput);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void eap$onInit(C menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        eap$syncInitialState(menu);
        eap$createWidgets();
    }

    @Inject(method = "updateBeforeRender", at = @At("HEAD"), remap = false)
    private void eap$updateBeforeRender(CallbackInfo ci) {
        eap$updateToggles();
        eap$updateLimitInput();
        eap$updateExGuiLayout();
    }

    @Unique
    private void eap$updateToggles() {
        if (this.eap$AdvancedBlockingToggle != null && this.menu instanceof IPatternProviderMenuAdvancedSync sync) {
            this.eap$AdvancedBlockingEnabled = sync.eap$getAdvancedBlockingSynced();
            this.eap$AdvancedBlockingToggle.set(eap$AdvancedBlockingEnabled ? YesNo.YES : YesNo.NO);
        }
        if (this.eap$SmartDoublingToggle != null && this.menu instanceof IPatternProviderMenuDoublingSync sync) {
            this.eap$SmartDoublingEnabled = sync.eap$getSmartDoublingSynced();
            this.eap$SmartDoublingToggle.set(eap$SmartDoublingEnabled ? YesNo.YES : YesNo.NO);
        }
    }

    @Unique
    private void eap$updateLimitInput() {
        if (this.eap$PerProviderLimitInput == null) return;

        int remoteLimit = this.eap$PerProviderScalingLimit;
        if (this.menu instanceof IPatternProviderMenuDoublingSync sync) {
            remoteLimit = sync.eap$getScalingLimit();
        }

        boolean focused = this.eap$PerProviderLimitInput.isFocused();
        if (!focused && remoteLimit != this.eap$PerProviderScalingLimit) {
            this.eap$PerProviderScalingLimit = remoteLimit;
            this.eap$PerProviderLimitInput.setValue(String.valueOf(remoteLimit));
        }

        if (this.eap$SmartDoublingEnabled) {
            if (!this.renderables.contains(this.eap$PerProviderLimitInput)) {
                this.addRenderableWidget(this.eap$PerProviderLimitInput);
            }
            if (!focused && this.eap$PerProviderLimitInput.getValue().trim().isEmpty()) {
                this.eap$PerProviderLimitInput.setValue("0");
            }

            // Position input box to the left of the smart doubling toggle
            if (eap$SmartDoublingToggle != null) {
                int ex = eap$SmartDoublingToggle.x - this.eap$PerProviderLimitInput.getWidth() - 5;
                int ey = eap$SmartDoublingToggle.y + 2;
                this.eap$PerProviderLimitInput.x = ex;
                this.eap$PerProviderLimitInput.y = ey;
            }
        } else {
            this.removeWidget(this.eap$PerProviderLimitInput);
        }
    }

    @Unique
    private void eap$updateExGuiLayout() {
        if ((Object) this instanceof GuiExPatternProvider) {
            try {
                ((IExPatternButton) this).eap$updateButtonsLayout();
            } catch (Throwable t) {
                EAP$LOGGER.debug("[EAP] updateButtonsLayout skipped: {}", t.toString());
            }
        }
    }

    @Inject(method = "updateBeforeRender", at = @At("RETURN"), remap = false)
    private void onUpdateBeforeRender(CallbackInfo ci) {
        Component t = this.getTitle();
        if (!t.getString().isEmpty()) {
            this.setTextContent(AEBaseScreen.TEXT_ID_DIALOG_TITLE, t);
        }
    }
}
