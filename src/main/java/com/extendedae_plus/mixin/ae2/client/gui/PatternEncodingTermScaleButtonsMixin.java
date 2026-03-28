package com.extendedae_plus.mixin.ae2.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.parts.encoding.EncodingMode;
import com.google.gson.JsonObject;
import com.extendedae_plus.ExtendedAEPlus;
import com.extendedae_plus.client.gui.widgets.ScaledTextureButton;
import com.extendedae_plus.init.ModNetwork;
import com.extendedae_plus.mixin.ae2.accessor.AEBaseScreenAccessor;
import com.extendedae_plus.mixin.minecraft.accessor.AbstractContainerScreenAccessor;
import com.extendedae_plus.mixin.minecraft.accessor.ScreenAccessor;
import com.extendedae_plus.network.ScaleEncodingPatternC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"AddedMixinMembersNamePattern"})
@Mixin(PatternEncodingTermScreen.class)
public abstract class PatternEncodingTermScaleButtonsMixin {
    @Unique
    private static final ResourceLocation EAP$SCALE_BUTTON_TEXTURE =
            new ResourceLocation(ExtendedAEPlus.MODID, "textures/gui/beizeng.png");
    @Unique
    private static final ResourceLocation EAP$SWAP_OUTPUT_TEXTURE =
            new ResourceLocation(ExtendedAEPlus.MODID, "textures/gui/zhu_fu_qie_huan.png");
    @Unique
    private static final ResourceLocation EAP$RESTORE_RATIO_TEXTURE =
            new ResourceLocation(ExtendedAEPlus.MODID, "textures/gui/huanyuan.png");
    @Unique
    private static final ResourceLocation EAP$LAYOUT_RESOURCE =
            new ResourceLocation("ae2", "screens/terminals/encoding/eaep_pattern_terminals.json");

    @Unique private ScaledTextureButton eap$mul2Button;
    @Unique private ScaledTextureButton eap$mul3Button;
    @Unique private ScaledTextureButton eap$mul5Button;
    @Unique private ScaledTextureButton eap$div2Button;
    @Unique private ScaledTextureButton eap$div3Button;
    @Unique private ScaledTextureButton eap$div5Button;
    @Unique private ScaledTextureButton eap$swapOutputsButton;
    @Unique private ScaledTextureButton eap$restoreRatioButton;
    @Unique private static JsonObject eap$layoutWidgets;
    @Unique private static boolean eap$layoutWidgetsLoaded;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void eap$createScaleButtons(CallbackInfo ci) {
        if (this.eap$mul2Button == null) {
            this.eap$mul2Button = eap$createScaleButton(0, 0, "x2", ScaleEncodingPatternC2SPacket.Operation.MUL2);
            this.eap$mul3Button = eap$createScaleButton(16, 0, "x3", ScaleEncodingPatternC2SPacket.Operation.MUL3);
            this.eap$mul5Button = eap$createScaleButton(32, 0, "x5", ScaleEncodingPatternC2SPacket.Operation.MUL5);
            this.eap$div2Button = eap$createScaleButton(0, 16, "/2", ScaleEncodingPatternC2SPacket.Operation.DIV2);
            this.eap$div3Button = eap$createScaleButton(16, 16, "/3", ScaleEncodingPatternC2SPacket.Operation.DIV3);
            this.eap$div5Button = eap$createScaleButton(32, 16, "/5", ScaleEncodingPatternC2SPacket.Operation.DIV5);
            this.eap$swapOutputsButton = eap$createStandaloneButton(
                    EAP$SWAP_OUTPUT_TEXTURE,
                    Component.translatable("extendedae_plus.tooltip.swap_processing_outputs"),
                    ScaleEncodingPatternC2SPacket.Operation.SWAP_OUTPUTS);
            this.eap$restoreRatioButton = eap$createStandaloneButton(
                    EAP$RESTORE_RATIO_TEXTURE,
                    Component.translatable("extendedae_plus.tooltip.restore_processing_ratio"),
                    ScaleEncodingPatternC2SPacket.Operation.RESTORE_RATIO);
        }

        eap$ensureAdded(this.eap$mul2Button);
        eap$ensureAdded(this.eap$mul3Button);
        eap$ensureAdded(this.eap$mul5Button);
        eap$ensureAdded(this.eap$div2Button);
        eap$ensureAdded(this.eap$div3Button);
        eap$ensureAdded(this.eap$div5Button);
        eap$ensureAdded(this.eap$swapOutputsButton);
        eap$ensureAdded(this.eap$restoreRatioButton);
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"), remap = false)
    private void eap$updateScaleButtons(CallbackInfo ci) {
        if (this.eap$mul2Button == null) {
            return;
        }

        var screen = (PatternEncodingTermScreen<?>) (Object) this;

        eap$ensureAdded(this.eap$mul2Button);
        eap$ensureAdded(this.eap$mul3Button);
        eap$ensureAdded(this.eap$mul5Button);
        eap$ensureAdded(this.eap$div2Button);
        eap$ensureAdded(this.eap$div3Button);
        eap$ensureAdded(this.eap$div5Button);
        eap$ensureAdded(this.eap$swapOutputsButton);
        eap$ensureAdded(this.eap$restoreRatioButton);

        boolean visible = screen.getMenu().getMode() == EncodingMode.PROCESSING;
        this.eap$mul2Button.setVisibility(visible);
        this.eap$mul3Button.setVisibility(visible);
        this.eap$mul5Button.setVisibility(visible);
        this.eap$div2Button.setVisibility(visible);
        this.eap$div3Button.setVisibility(visible);
        this.eap$div5Button.setVisibility(visible);
        this.eap$swapOutputsButton.setVisibility(visible);
        this.eap$restoreRatioButton.setVisibility(visible);

        if (!visible) {
            return;
        }

        Rect2i bounds = eap$getScreenBounds();
        eap$placeButton(this.eap$swapOutputsButton, "zhu_fu_qie_huan", bounds);
        eap$placeButton(this.eap$restoreRatioButton, "huan_yuan_mo_ren", bounds);
        eap$placeButton(this.eap$mul2Button, "cheng_2", bounds);
        eap$placeButton(this.eap$mul3Button, "cheng_3", bounds);
        eap$placeButton(this.eap$mul5Button, "cheng_5", bounds);
        eap$placeButton(this.eap$div2Button, "chu_2", bounds);
        eap$placeButton(this.eap$div3Button, "chu_3", bounds);
        eap$placeButton(this.eap$div5Button, "chu_5", bounds);
    }

    @Unique
    private ScaledTextureButton eap$createScaleButton(int srcX, int srcY, String tooltipText,
            ScaleEncodingPatternC2SPacket.Operation op) {
        return new ScaledTextureButton(
                EAP$SCALE_BUTTON_TEXTURE,
                48,
                32,
                srcX,
                srcY,
                16,
                16,
                0.5f,
                Component.literal(tooltipText),
                btn -> ModNetwork.CHANNEL.sendToServer(new ScaleEncodingPatternC2SPacket(op))
        );
    }

    @Unique
    private ScaledTextureButton eap$createStandaloneButton(ResourceLocation texture, Component tooltipText,
            ScaleEncodingPatternC2SPacket.Operation op) {
        return new ScaledTextureButton(
                texture,
                16,
                16,
                0,
                0,
                16,
                16,
                0.5f,
                tooltipText,
                btn -> ModNetwork.CHANNEL.sendToServer(new ScaleEncodingPatternC2SPacket(op))
        );
    }

    @Unique
    private Rect2i eap$getScreenBounds() {
        var screen = (AbstractContainerScreenAccessor<?>) this;
        return new Rect2i(screen.eap$getLeftPos(), screen.eap$getTopPos(),
                screen.eap$getImageWidth(), screen.eap$getImageHeight());
    }

    @Unique
    private void eap$placeButton(ScaledTextureButton button, String widgetId, Rect2i bounds) {
        try {
            ScreenStyle style = ((AEBaseScreenAccessor<?>) this).eap$getStyle();
            WidgetStyle widgetStyle = style.getWidget(widgetId);
            var pos = widgetStyle.resolve(bounds);
            button.setX(pos.getX());
            button.setY(pos.getY());
        } catch (IllegalStateException ignored) {
            int left = eap$getFallbackWidgetValue(widgetId, "left");
            int bottom = eap$getFallbackWidgetValue(widgetId, "bottom");
            button.setX(bounds.getX() + left);
            button.setY(bounds.getY() + bounds.getHeight() - bottom);
        }
    }

    @Unique
    private int eap$getFallbackWidgetValue(String widgetId, String key) {
        JsonObject widget = eap$getFallbackWidget(widgetId);
        if (widget != null && widget.has(key)) {
            return widget.get(key).getAsInt();
        }

        return switch (widgetId + "#" + key) {
            case "zhu_fu_qie_huan#left", "cheng_2#left", "cheng_3#left", "cheng_5#left" -> 125;
            case "huan_yuan_mo_ren#left", "chu_2#left", "chu_3#left", "chu_5#left" -> 101;
            case "zhu_fu_qie_huan#bottom", "huan_yuan_mo_ren#bottom" -> 159;
            case "cheng_2#bottom", "chu_2#bottom" -> 149;
            case "cheng_3#bottom", "chu_3#bottom" -> 138;
            case "cheng_5#bottom", "chu_5#bottom" -> 127;
            default -> 0;
        };
    }

    @Unique
    private JsonObject eap$getFallbackWidget(String widgetId) {
        if (!eap$layoutWidgetsLoaded) {
            eap$layoutWidgetsLoaded = true;
            try {
                var resourceManager = Minecraft.getInstance().getResourceManager();
                try (var reader = resourceManager.openAsReader(EAP$LAYOUT_RESOURCE)) {
                    JsonObject root = ScreenStyle.GSON.fromJson(reader, JsonObject.class);
                    if (root != null && root.has("widgets") && root.get("widgets").isJsonObject()) {
                        eap$layoutWidgets = root.getAsJsonObject("widgets");
                    }
                }
            } catch (Exception ignored) {
                eap$layoutWidgets = null;
            }
        }

        if (eap$layoutWidgets == null || !eap$layoutWidgets.has(widgetId)) {
            return null;
        }

        var widget = eap$layoutWidgets.get(widgetId);
        return widget != null && widget.isJsonObject() ? widget.getAsJsonObject() : null;
    }

    @Unique
    private void eap$ensureAdded(ScaledTextureButton button) {
        var accessor = (ScreenAccessor) this;
        var renderables = accessor.eap$getRenderables();
        var children = accessor.eap$getChildren();
        if (!renderables.contains(button)) {
            renderables.add(button);
        }
        if (!children.contains(button)) {
            children.add(button);
        }
    }
}
