package com.extendedae_plus.content.quantum.screen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.client.gui.Icon;

public class InfoBar {
    private final List<Widget> widgets = new ArrayList<>();

    public void render(PoseStack poseStack, int x, int y) {
        for (var widget : widgets) {
            widget.render(poseStack, x, y);
        }
    }

    interface Widget {
        void render(PoseStack poseStack, int baseX, int baseY);
    }

    void add(Icon icon, float scale, int xPos, int yPos) {
        widgets.add(new IconWidget(icon, scale, xPos, yPos));
    }

    void add(String text, int color, float scale, int xPos, int yPos) {
        widgets.add(new TextWidget(Component.literal(text), color, scale, xPos, yPos));
    }

    void add(Component text, int color, float scale, int xPos, int yPos) {
        widgets.add(new TextWidget(text, color, scale, xPos, yPos));
    }

    void add(AEKey what, float scale, int xPos, int yPos) {
        widgets.add(new SpaceWidget(Math.round(16 * scale)));
    }

    void add(ItemLike what, float scale, int xPos, int yPos) {
        widgets.add(new StackWidget(AEItemKey.of(what), scale, xPos, yPos));
    }

    void addSpace(int width) {
        widgets.add(new SpaceWidget(width));
    }

    private record StackWidget(AEKey what, float scale, int xPos, int yPos) implements Widget {
        @Override
        public void render(PoseStack poseStack, int baseX, int baseY) {
            poseStack.pushPose();
            poseStack.translate(baseX + xPos, baseY + yPos, 0);
            poseStack.scale(scale, scale, 1);
            GuiComponent.fill(poseStack, 0, 0, Math.round(16 * scale), Math.round(16 * scale), 0x00000000);
            poseStack.popPose();
        }
    }

    private record IconWidget(Icon icon, float scale, int xPos, int yPos) implements Widget {
        @Override
        public void render(PoseStack poseStack, int baseX, int baseY) {
            poseStack.pushPose();
            poseStack.translate(baseX + xPos, baseY + yPos, 0);
            poseStack.scale(scale, scale, 1);
            icon.getBlitter().dest(0, 0).blit(poseStack, 0);
            poseStack.popPose();
        }
    }

    private static final class TextWidget implements Widget {
        private final Component text;
        private final int color;
        private final float scale;
        private final int xPos;
        private final int yPos;
        private final int width;
        private final int height;

        public TextWidget(Component text, int color, float scale, int xPos, int yPos) {
            this.text = text;
            this.color = color;
            this.scale = scale;
            this.xPos = xPos;
            this.yPos = yPos;
            var font = Minecraft.getInstance().font;
            this.width = Math.round(font.width(text) * scale);
            this.height = Math.round(font.lineHeight * scale);
        }

        @Override
        public void render(PoseStack poseStack, int baseX, int baseY) {
            var font = Minecraft.getInstance().font;
            poseStack.pushPose();
            poseStack.translate(baseX + xPos, baseY + yPos, 0);
            poseStack.scale(scale, scale, 1);
            font.draw(poseStack, text, 0, 0, color);
            poseStack.popPose();
        }
    }

    private static final class SpaceWidget implements Widget {
        private final int width;

        public SpaceWidget(int width) {
            this.width = width;
        }

        @Override
        public void render(PoseStack poseStack, int baseX, int baseY) {
        }
    }
}
