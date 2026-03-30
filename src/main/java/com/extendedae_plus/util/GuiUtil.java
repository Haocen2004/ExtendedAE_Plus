package com.extendedae_plus.util;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.patternaccess.PatternContainerRecord;
import appeng.client.gui.me.patternaccess.PatternSlot;
import appeng.client.gui.widgets.SettingToggleButton;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.IntConsumer;

/**
 * GUI utility: pattern fetching, rendering helpers.
 */
public class GuiUtil {
    private GuiUtil() {throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");}

    public static String getPatternOutputText(ItemStack pattern) {
        if (pattern.isEmpty()) {
            return "";
        }

        var details = PatternDetailsHelper.decodePattern(pattern, Minecraft.getInstance().level, false);
        if (details == null || details.getOutputs().length == 0) {
            return "";
        }

        GenericStack out = details.getOutputs()[0];
        long amount = out.amount();
        long perUnit = out.what().getAmountPerUnit();
        if (amount <= 0 || perUnit <= 0) {
            return "";
        }

        double units = (double) amount / perUnit;
        if (units <= 0) {
            return "";
        }

        String autoSuffix = "";
        if (perUnit > 1) {
            autoSuffix = "B";
        }
        return NumberFormatUtil.formatNumberWithDecimal(units) + autoSuffix;
    }

    /**
     * Draw amount text at bottom-right of a slot.
     */
    public static void drawAmountText(PoseStack poseStack, Font font, String text, int slotX, int slotY, float scale) {
        if (text.isEmpty()) {
            return;
        }

        int scaledWidth = (int)(font.width(text) * scale);
        int textX = slotX + 16 - scaledWidth;
        int textY = slotY + 11;

        poseStack.pushPose();
        poseStack.translate(0, 0, 300);
        poseStack.scale(scale, scale, 1.0f);
        font.drawShadow(poseStack, text, textX / scale, textY / scale, 0xFFFFFFFF);
        poseStack.popPose();
    }

    private static int withAlpha(int rgb, int alpha255) {
        return ((alpha255 & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int hsvToRgb(float h, float s, float v) {
        if (s <= 0.0f) {
            int g = Math.round(v * 255.0f);
            return (g << 16) | (g << 8) | g;
        }
        float hh = (h - (float) Math.floor(h)) * 6.0f;
        int sector = (int) Math.floor(hh);
        float f = hh - sector;
        float p = v * (1.0f - s);
        float q = v * (1.0f - s * f);
        float t = v * (1.0f - s * (1.0f - f));
        float r, g, b;
        switch (sector) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        int ri = Math.round(r * 255.0f);
        int gi = Math.round(g * 255.0f);
        int bi = Math.round(b * 255.0f);
        return (ri << 16) | (gi << 8) | bi;
    }

    private static int getRainbowRgb() {
        long now = System.currentTimeMillis();
        final long rainbowPeriodMs = 4000L;
        float hue = (now % rainbowPeriodMs) / (float) rainbowPeriodMs;
        return hsvToRgb(hue, 1.0f, 1.0f);
    }

    private static void drawSlotBox(PoseStack poseStack, int sx, int sy, int borderColor, int backgroundColor) {
        GuiComponent.fill(poseStack, sx - 1, sy - 1, sx + 17, sy, borderColor);
        GuiComponent.fill(poseStack, sx - 1, sy + 16, sx + 17, sy + 17, borderColor);
        GuiComponent.fill(poseStack, sx - 1, sy, sx, sy + 16, borderColor);
        GuiComponent.fill(poseStack, sx + 16, sy, sx + 17, sy + 16, borderColor);
        GuiComponent.fill(poseStack, sx, sy, sx + 16, sy + 16, backgroundColor);
    }

    public static void drawPatternSlotHighlights(PoseStack poseStack, List<Slot> slots, Set<ItemStack> matchedStack, Set<PatternContainerRecord> matchedProvider) {
        if (slots == null) return;

        int rainbowRgb = getRainbowRgb();

        for (Slot slot : slots) {
            if (!(slot instanceof PatternSlot ps)) {
                continue;
            }

            int sx = slot.x;
            int sy = slot.y;

            boolean isMatchedSlot = matchedStack != null && matchedStack.contains(slot.getItem());
            boolean isMatchedProvider = false;
            try {
                PatternContainerRecord container = ps.getMachineInv();
                isMatchedProvider = matchedProvider != null && matchedProvider.contains(container);
            } catch (Throwable ignored) {
            }

            int borderColor;
            int backgroundColor;

            if (isMatchedSlot) {
                borderColor = withAlpha(rainbowRgb, 0xA0);
                backgroundColor = withAlpha(rainbowRgb, 0x3C);
            } else if (!isMatchedProvider) {
                borderColor = withAlpha(0xFFFFFF, 0x40);
                backgroundColor = withAlpha(0x000000, 0x18);
            } else {
                borderColor = withAlpha(0xFFFFFF, 0x30);
                backgroundColor = withAlpha(0xFFFFFF, 0x14);
            }

            drawSlotBox(poseStack, sx, sy, borderColor, backgroundColor);
        }
    }

    public static void drawSlotRainbowHighlight(PoseStack poseStack, int sx, int sy) {
        int rainbowRgb = getRainbowRgb();
        int borderColor = withAlpha(rainbowRgb, 0xA0);
        int backgroundColor = withAlpha(rainbowRgb, 0x3C);
        drawSlotBox(poseStack, sx, sy, borderColor, backgroundColor);
    }

    public static SettingToggleButton<YesNo> createToggle(boolean initial,
                                                          Runnable onClick,
                                                          Supplier<List<Component>> tooltipSupplier) {
        return new SettingToggleButton<>(
                Settings.BLOCKING_MODE,
                initial ? YesNo.YES : YesNo.NO,
                (btn, backwards) -> onClick.run()
        ) {
            @Override
            public List<Component> getTooltipMessage() {
                return tooltipSupplier.get();
            }
        };
    }

    public static EditBox createPerProviderLimitInput(Font font, int initialValue, IntConsumer onCommit) {
        EditBox input = new EditBox(font, 0, 0, 28, 12, Component.literal("Limit"));
        input.setMaxLength(6);
        input.setValue(String.valueOf(initialValue));
        input.setResponder(s -> {
            try {
                String sValue = (s == null || s.isBlank()) ? "0" : s.replaceFirst("^0+(?=.)", "");
                if (!sValue.equals(s)) {
                    input.setValue(sValue);
                }
                int limit = Integer.parseInt(sValue);
                onCommit.accept(limit);
            } catch (Throwable ignored) {}
        });
        return input;
    }
}
