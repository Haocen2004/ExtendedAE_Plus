package com.extendedae_plus.client.gui.widgets;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ScaledTextureButton extends Button {
    private final OnPress delegateOnPress;
    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int srcX;
    private final int srcY;
    private final int srcWidth;
    private final int srcHeight;
    private final float scale;

    public ScaledTextureButton(ResourceLocation texture, int textureWidth, int textureHeight,
            int srcX, int srcY, int srcWidth, int srcHeight, float scale,
            Component tooltipText, OnPress onPress) {
        super(0, 0, Math.round(srcWidth * scale), Math.round(srcHeight * scale),
                Component.empty(), btn -> {});
        this.delegateOnPress = onPress;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.srcX = srcX;
        this.srcY = srcY;
        this.srcWidth = srcWidth;
        this.srcHeight = srcHeight;
        this.scale = scale;
        // Tooltip.create() requires 1.20+; tooltip display not supported in 1.19.2
    }

    public void setVisibility(boolean visible) {
        this.visible = visible;
        this.active = visible;
    }

    @Override
    public void onPress() {
        this.delegateOnPress.onPress(this);
        this.setFocused(false);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        this.setFocused(false);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
        this.setFocused(false);
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        this.width = Math.round(this.srcWidth * this.scale);
        this.height = Math.round(this.srcHeight * this.scale);

        int yOffset = this.isHovered ? 1 : 0;

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        poseStack.pushPose();
        poseStack.translate(this.x, this.y + yOffset, 0.0F);
        poseStack.scale(scale, scale, 1.0F);

        Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(0, 0).blit(poseStack, 0);

        if (!this.active) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        }
        Blitter.texture(this.texture, this.textureWidth, this.textureHeight)
                .src(this.srcX, this.srcY, this.srcWidth, this.srcHeight)
                .dest(0, 0)
                .blit(poseStack, 0);
        if (!this.active) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        poseStack.popPose();
        RenderSystem.enableDepthTest();
    }
}
