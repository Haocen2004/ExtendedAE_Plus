package com.extendedae_plus.mixin.ae2.client.gui;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.TextOverride;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.Text;
import appeng.menu.slot.AppEngSlot;
import com.extendedae_plus.api.IExPatternPage;
import com.extendedae_plus.content.ClientPatternHighlightStore;
import com.extendedae_plus.init.ModNetwork;
import com.extendedae_plus.mixin.ae2.accessor.AEBaseScreenAccessor;
import com.extendedae_plus.mixin.minecraft.accessor.AbstractContainerScreenAccessor;
import com.extendedae_plus.mixin.minecraft.accessor.ScreenAccessor;
import com.extendedae_plus.network.crafting.CraftingMonitorJumpC2SPacket;
import com.extendedae_plus.network.crafting.CraftingMonitorOpenProviderC2SPacket;
import com.extendedae_plus.util.GuiUtil;
import com.github.glodblock.epp.client.gui.GuiExPatternProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AEBaseScreen.class)
public abstract class AEBaseScreenMixin {
    /**
     * Intercept CraftingCPUScreen Shift+click: jump or open provider UI.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void eap$craftingCpuShiftClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (!(self instanceof CraftingCPUScreen<?> screen)) {
            return;
        }

        if (!Screen.hasShiftDown() || (button != 0 && button != 1)) {
            return;
        }

        try {
            GenericStack hovered = screen.getStackUnderMouse(mouseX, mouseY);
            if (hovered == null) {
                return;
            }

            AEKey key = hovered.what();
            if (key == null) {
                return;
            }

            if (button == 0) {
                ModNetwork.CHANNEL.sendToServer(new CraftingMonitorJumpC2SPacket(key));
            } else {
                ModNetwork.CHANNEL.sendToServer(new CraftingMonitorOpenProviderC2SPacket(key));
            }

            cir.setReturnValue(true);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Render pattern output amounts on visible pattern slots.
     */
    @Inject(method = "renderAppEngSlot", at = @At("TAIL"), remap = false)
    private void eap$renderSlotAmounts(PoseStack poseStack, AppEngSlot appEngSlot, CallbackInfo ci) {
        if (!appEngSlot.isActive() || !appEngSlot.isSlotEnabled()) {
            return;
        }

        var itemStack = appEngSlot.getItem();
        if (itemStack.isEmpty()) {
            return;
        }

        String amountText = GuiUtil.getPatternOutputText(itemStack);
        if (amountText.isEmpty()) {
            return;
        }

        Font font = ((ScreenAccessor) this).eap$getFont();
        GuiUtil.drawAmountText(poseStack, font, amountText, appEngSlot.x, appEngSlot.y, 0.6f);

        try {
            var details = PatternDetailsHelper.decodePattern(itemStack, Minecraft.getInstance().level, false);
            if (details != null && details.getOutputs() != null && details.getOutputs().length > 0) {
                AEKey key = details.getOutputs()[0].what();
                if (key != null && ClientPatternHighlightStore.hasHighlight(key)) {
                    GuiUtil.drawSlotRainbowHighlight(poseStack, appEngSlot.x, appEngSlot.y);
                }
            }
        } catch (Throwable ignore) {}
    }

    /**
     * After drawing the "Patterns" label text, append page info.
     */
    @Inject(method = "drawText", at = @At("TAIL"), remap = false)
    private void eap$appendPageAfterPatternsLabel(PoseStack poseStack,
                                                  Text text,
                                                  @Nullable TextOverride override,
                                                  CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof GuiExPatternProvider)) return;

        Component content = text.getText();
        if (!"gui.ae2.Patterns".equals(content.getContents() instanceof TranslatableContents tc ? tc.getKey() : null)) {
            return;
        }

        try {
            int cur = 1;
            int max = 1;
            if (self instanceof IExPatternPage accessor) {
                cur = Math.max(0, accessor.eap$getCurrentPage()) + 1;
                max = Math.max(max, accessor.eap$getMaxPageLocal());
            }

            Component pageText = Component.translatable("gui.extendedae.pattern_page", cur, max);

            AbstractContainerScreenAccessor<?> screen = (AbstractContainerScreenAccessor<?>) this;
            int imageWidth = screen.eap$getImageWidth();
            int imageHeight = screen.eap$getImageHeight();
            Point pos = text.getPosition().resolve(
                    new Rect2i(0, 0, imageWidth, imageHeight)
            );

            Font font = ((ScreenAccessor) this).eap$getFont();
            float scale = text.getScale();
            int lineWidth = font.width(content.getVisualOrderText());

            int x = pos.getX() + lineWidth + 4;
            int y = pos.getY();

            int color = 0xFFFFFFFF;
            ScreenStyle style = ((AEBaseScreenAccessor<?>) this).eap$getStyle();
            if (style != null) {
                color = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
            }

            poseStack.pushPose();
            poseStack.translate(x, y, 1);
            if (scale != 1.0f) poseStack.scale(scale, scale, 1);
            font.draw(poseStack, pageText, 0, 0, color);
            poseStack.popPose();
        } catch (Throwable ignored) {}
    }
}
