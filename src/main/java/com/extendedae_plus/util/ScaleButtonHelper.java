package com.extendedae_plus.util;

import appeng.client.gui.style.Blitter;
import com.extendedae_plus.client.widget.BlitterIconButton;
import com.extendedae_plus.gui.NewIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Utility: unified creation, registration & layout for "multiply/divide" buttons.
 */
public final class ScaleButtonHelper {
    private ScaleButtonHelper() {}

    private static BlitterIconButton createIconButton(Blitter icon, java.util.function.Consumer<net.minecraft.client.gui.components.Button> onPress) {
        return new BlitterIconButton(onPress::accept, icon);
    }

    public static ScaleButtonSet createButtons(BiConsumer<Boolean, Integer> handler) {
        BlitterIconButton div2 = createIconButton(NewIcon.DIVIDE2, b -> handler.accept(true, 2));
        BlitterIconButton x2 = createIconButton(NewIcon.MULTIPLY2, b -> handler.accept(false, 2));
        BlitterIconButton div5 = createIconButton(NewIcon.DIVIDE5, b -> handler.accept(true, 5));
        BlitterIconButton x5 = createIconButton(NewIcon.MULTIPLY5, b -> handler.accept(false, 5));
        BlitterIconButton div10 = createIconButton(NewIcon.DIVIDE10, b -> handler.accept(true, 10));
        BlitterIconButton x10 = createIconButton(NewIcon.MULTIPLY10, b -> handler.accept(false, 10));

        for (var b : List.of(x2, div2, x5, div5, x10, div10)) {
            b.setVisibility(true);
        }

        return new ScaleButtonSet(x2, div2, x5, div5, x10, div10);
    }

    public static List<BlitterIconButton> all(ScaleButtonSet set) {
        return new ArrayList<>(List.of(
                set.divide2(), set.multiply2(),
                set.divide5(), set.multiply5(),
                set.divide10(), set.multiply10()
        ));
    }

    public static void layoutButtons(ScaleButtonSet set, int baseX, int baseY, int spacing, Side side) {
        int bx = baseX + (side == Side.LEFT ? -1 : 1);
        int by = baseY;

        set.divide2().x = bx;
        set.divide2().y = by;
        set.multiply2().x = bx;
        set.multiply2().y = by + spacing;
        set.divide5().x = bx;
        set.divide5().y = by + spacing * 2;
        set.multiply5().x = bx;
        set.multiply5().y = by + spacing * 3;
        set.divide10().x = bx;
        set.divide10().y = by + spacing * 4;
        set.multiply10().x = bx;
        set.multiply10().y = by + spacing * 5;
    }

    public static List<BlitterIconButton> createAndLayout(int baseX, int baseY, int spacing, Side side, BiConsumer<Boolean, Integer> handler) {
        ScaleButtonSet set = createButtons(handler);
        layoutButtons(set, baseX, baseY, spacing, side);
        return all(set);
    }

    public enum Side {LEFT, RIGHT}

    public record ScaleButtonSet(
            BlitterIconButton multiply2,
            BlitterIconButton divide2,
            BlitterIconButton multiply5,
            BlitterIconButton divide5,
            BlitterIconButton multiply10,
            BlitterIconButton divide10
    ) {
    }
}
