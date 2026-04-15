package com.extendedae_plus.mixin.ae2.client.gui;

import appeng.api.config.ActionItems;
import appeng.client.gui.Icon;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.IconButton;
import com.extendedae_plus.init.ModNetwork;
import com.extendedae_plus.mixin.ae2.accessor.AEBaseScreenAccessor;
import com.extendedae_plus.mixin.minecraft.accessor.AbstractContainerScreenAccessor;
import com.extendedae_plus.mixin.minecraft.accessor.ScreenAccessor;
import com.extendedae_plus.network.provider.RequestProvidersListC2SPacket;
import com.extendedae_plus.network.upload.EncodeWithShiftFlagC2SPacket;
import com.extendedae_plus.util.uploadPattern.RecipeTypeNameConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import appeng.parts.encoding.EncodingMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Add an upload button to the Pattern Encoding Terminal.
 */
@SuppressWarnings({"AddedMixinMembersNamePattern"})
@Mixin(PatternEncodingTermScreen.class)
public abstract class PatternEncodingTermScreenMixin {

    @Unique private IconButton eap$uploadBtn;

    @ModifyVariable(method = "<init>", at = @At(value = "STORE"), name = "encodeBtn")
    private ActionButton eap$wrapEncodeButton(ActionButton original) {
        return new ActionButton(ActionItems.ENCODE, act -> {
            var screen = (PatternEncodingTermScreen<?>) (Object) this;
            eap$presetProviderSearchKeyByMode(screen.getMenu().getMode());
            ModNetwork.CHANNEL.sendToServer(new EncodeWithShiftFlagC2SPacket(Screen.hasShiftDown()));
            screen.getMenu().encode();
        });
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void eap$createUploadButton(CallbackInfo ci) {
        if (eap$uploadBtn == null) {
            eap$uploadBtn = createUploadButton();
        }
        addButtonToScreen();
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"), remap = false)
    private void eap$ensureUploadButton(CallbackInfo ci) {
        if (eap$uploadBtn == null) return;

        updateUploadButtonPosition();
        addButtonToScreen();
    }

    @Unique
    private IconButton createUploadButton() {
        IconButton btn = new IconButton(button -> {
                var screen = (PatternEncodingTermScreen<?>) (Object) this;
                eap$presetProviderSearchKeyByMode(screen.getMenu().getMode());
                ModNetwork.CHANNEL.sendToServer(new RequestProvidersListC2SPacket());
            }
        ) {
            private final float eap$scale = 0.75f;

            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partial) {
                if (!this.visible) return;

                var icon = this.getIcon();
                var blitter = icon.getBlitter();
                if (!this.active) blitter.opacity(0.5f);

                this.width = Math.round(16 * eap$scale);
                this.height = Math.round(16 * eap$scale);

                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();

                if (this.isFocused()) {
                    GuiComponent.fill(poseStack, this.x - 1, this.y - 1, this.x + width + 1, this.y, 0xFFFFFFFF);
                    GuiComponent.fill(poseStack, this.x - 1, this.y, this.x, this.y + height, 0xFFFFFFFF);
                    GuiComponent.fill(poseStack, this.x + width, this.y, this.x + width + 1, this.y + height, 0xFFFFFFFF);
                    GuiComponent.fill(poseStack, this.x - 1, this.y + height, this.x + width + 1, this.y + height + 1, 0xFFFFFFFF);
                }

                poseStack.pushPose();
                poseStack.translate(this.x, this.y, 0.0F);
                poseStack.scale(eap$scale, eap$scale, 1.f);
                if (!this.isDisableBackground()) {
                    Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter().dest(0, 0).blit(poseStack, 0);
                }
                blitter.dest(0, 0).blit(poseStack, 0);
                poseStack.popPose();

                RenderSystem.enableDepthTest();
            }

            @Override
            public Rect2i getTooltipArea() {
                return new Rect2i(this.x, this.y, Math.round(16 * eap$scale), Math.round(16 * eap$scale));
            }

            @Override
            protected Icon getIcon() {
                return Icon.ARROW_UP;
            }
        };
        btn.setMessage(Component.translatable("extendedae_plus.button.choose_provider"));
        return btn;
    }

    @Unique
    private static void eap$presetProviderSearchKeyByMode(EncodingMode mode) {
        if (mode == EncodingMode.CRAFTING
                || mode == EncodingMode.SMITHING_TABLE
                || mode == EncodingMode.STONECUTTING) {
            // Use configurable alias mapping for crafting default keyword.
            RecipeTypeNameConfig.presetCraftingProviderSearchKey();
        }
    }

    @Unique
    private void updateUploadButtonPosition() {
        if (eap$uploadBtn == null) return;

        AbstractContainerScreenAccessor<?> screen = (AbstractContainerScreenAccessor<?>) this;
        try {
            ScreenStyle style = ((AEBaseScreenAccessor<?>) this).eap$getStyle();
            WidgetStyle ws = style.getWidget("encodePattern");

            var bounds = new Rect2i(
                    screen.eap$getLeftPos(),
                    screen.eap$getTopPos(),
                    screen.eap$getImageWidth(),
                    screen.eap$getImageHeight()
            );

            var pos = ws.resolve(bounds);
            int baseW = ws.getWidth() > 0 ? ws.getWidth() : 16;
            int baseH = ws.getHeight() > 0 ? ws.getHeight() : 16;

            int targetW = Math.max(10, Math.round(baseW * 0.75f));
            int targetH = Math.max(10, Math.round(baseH * 0.75f));

            eap$uploadBtn.setWidth(targetW);
            eap$uploadBtn.setHeight(targetH);
            eap$uploadBtn.x = pos.getX() - targetW;
            eap$uploadBtn.y = pos.getY();
        } catch (Throwable t) {
            int leftPos = screen.eap$getLeftPos();
            int topPos = screen.eap$getTopPos();
            int imageWidth = screen.eap$getImageWidth();
            eap$uploadBtn.setWidth(12);
            eap$uploadBtn.setHeight(12);
            eap$uploadBtn.x = leftPos + imageWidth - 14;
            eap$uploadBtn.y = topPos + 88;
        }
    }

    @Unique
    private void addButtonToScreen() {
        var accessor = (ScreenAccessor) this;
        var renderables = accessor.eap$getRenderables();
        var children = accessor.eap$getChildren();
        if (!renderables.contains(eap$uploadBtn)) renderables.add(eap$uploadBtn);
        if (!children.contains(eap$uploadBtn)) children.add(eap$uploadBtn);
    }
}
