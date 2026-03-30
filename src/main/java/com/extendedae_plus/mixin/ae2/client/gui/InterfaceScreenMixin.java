package com.extendedae_plus.mixin.ae2.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.InterfaceScreen;
import appeng.menu.AEBaseMenu;
import com.extendedae_plus.init.ModNetwork;
import com.extendedae_plus.mixin.minecraft.accessor.AbstractContainerScreenAccessor;
import com.extendedae_plus.mixin.minecraft.accessor.ScreenAccessor;
import com.extendedae_plus.network.meInterface.InterfaceAdjustConfigAmountC2SPacket;
import com.extendedae_plus.util.ScaleButtonHelper;
import com.extendedae_plus.client.widget.BlitterIconButton;
import com.github.glodblock.epp.client.gui.GuiExInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inject multiply/divide buttons into AE2 ME Interface screen and EPP Ex Interface screen.
 */
@Mixin(AEBaseScreen.class)
public abstract class InterfaceScreenMixin<T extends AEBaseMenu> {

    @Unique private ScaleButtonHelper.ScaleButtonSet eap$scaleButtons;

    @Unique private int eap$lastLeftPos = -1;
    @Unique private int eap$lastTopPos = -1;
    @Unique private int eap$lastImageWidth = -1;
    @Unique private int eap$lastImageHeight = -1;

    @Inject(method = "init", at = @At("TAIL"))
    private void eap$addScaleButtons(CallbackInfo ci) {
        if (!eap$isSupportedInterfaceScreen()) return;

        if (eap$scaleButtons == null) {
            eap$scaleButtons = ScaleButtonHelper.createButtons((divide, factor) -> {
                ModNetwork.CHANNEL.sendToServer(new InterfaceAdjustConfigAmountC2SPacket(-1, divide, factor));
            });
        }

        var accessor = (ScreenAccessor) this;
        for (BlitterIconButton b : ScaleButtonHelper.all(eap$scaleButtons)) {
            if (!accessor.eap$getRenderables().contains(b)) accessor.eap$getRenderables().add(b);
            if (!accessor.eap$getChildren().contains(b)) accessor.eap$getChildren().add(b);
            b.setVisibility(true);
        }

        eap$relayoutButtons();
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void eap$ensureButtons(CallbackInfo ci) {
        if (!eap$isSupportedInterfaceScreen()) return;

        var accessor = (ScreenAccessor) this;
        for (BlitterIconButton b : ScaleButtonHelper.all(eap$scaleButtons)) {
            if (b != null) {
                if (!accessor.eap$getRenderables().contains(b)) accessor.eap$getRenderables().add(b);
                if (!accessor.eap$getChildren().contains(b)) accessor.eap$getChildren().add(b);
            }
        }

        AbstractContainerScreenAccessor<?> screen = (AbstractContainerScreenAccessor<?>) this;
        int curLeft = screen.eap$getLeftPos();
        int curTop = screen.eap$getTopPos();
        int curImgW = screen.eap$getImageWidth();
        int curImgH = screen.eap$getImageHeight();
        if (curLeft != eap$lastLeftPos ||
                curTop != eap$lastTopPos ||
                curImgW != eap$lastImageWidth ||
                curImgH != eap$lastImageHeight) {
            eap$lastLeftPos = curLeft;
            eap$lastTopPos = curTop;
            eap$lastImageWidth = curImgW;
            eap$lastImageHeight = curImgH;
            eap$relayoutButtons();
        }
    }

    @Unique
    private boolean eap$isSupportedInterfaceScreen() {
        if (((Object) this) instanceof InterfaceScreen) return true;
        if (((Object) this) instanceof GuiExInterface) return true;
        return false;
    }

    @Unique
    private void eap$relayoutButtons() {
        if (eap$scaleButtons == null) return;
        AbstractContainerScreenAccessor<?> screen = (AbstractContainerScreenAccessor<?>) this;
        int leftPos = screen.eap$getLeftPos();
        int topPos = screen.eap$getTopPos();

        ScaleButtonHelper.layoutButtons(
                eap$scaleButtons,
                leftPos - eap$scaleButtons.divide2().getWidth(),
                topPos + eap$scaleButtons.divide2().getWidth() + 30,
                22,
                ScaleButtonHelper.Side.LEFT
        );
    }
}
