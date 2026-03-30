package com.extendedae_plus.client.widget;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;

/**
 * An IconButton that renders a custom Blitter icon.
 * Workaround for EPPButton.getBlitterIcon() being package-private.
 */
public class BlitterIconButton extends IconButton {

    private final Blitter blitter;

    public BlitterIconButton(Button.OnPress onPress, Blitter blitter) {
        super(onPress);
        this.blitter = blitter;
    }

    @Override
    protected Icon getIcon() {
        return null;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (this.visible) {
            if (!this.isDisableBackground()) {
                Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(this.x, this.y).blit(poseStack, 0);
            }
            blitter.dest(this.x, this.y).blit(poseStack, 0);
        }
    }
}
