package com.extendedae_plus.content.quantum.screen;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import com.extendedae_plus.content.quantum.menu.QuantumComputerMenu;
import com.extendedae_plus.util.Logger;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import appeng.api.stacks.AmountFormat;
import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Icon;
import appeng.client.gui.Tooltip;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.Color;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;

public class AdvCpuSelectionList implements ICompositeWidget {
    private static final int ROWS = 6;

    private final Blitter background;
    private final Blitter buttonBg;
    private final Blitter buttonBgSelected;
    private final QuantumComputerMenu menu;
    private final Color textColor;
    private final int selectedColor;
    private final Scrollbar scrollbar;

    private Rect2i bounds = new Rect2i(0, 0, 0, 0);

    public AdvCpuSelectionList(QuantumComputerMenu menu, Scrollbar scrollbar, ScreenStyle style) {
        this.menu = menu;
        this.scrollbar = scrollbar;
        this.background = style.getImage("cpuList");
        this.buttonBg = style.getImage("cpuListButton");
        Blitter selected;
        try {
            selected = style.getImage("cpuListButtonSelected");
        } catch (IllegalStateException ignored) {
            // AE2 12.9.12 does not define a dedicated selected texture in crafting_status.json.
            Logger.EAP$LOGGER.debug("Screen style missing cpuListButtonSelected, fallback to cpuListButton");
            selected = this.buttonBg;
        }
        this.buttonBgSelected = selected;
        this.textColor = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR);
        this.selectedColor = style.getColor(PaletteColor.SELECTION_COLOR).toARGB();
        this.scrollbar.setCaptureMouseWheel(false);
    }

    @Override
    public void setPosition(Point position) {
        this.bounds = new Rect2i(position.getX(), position.getY(), bounds.getWidth(), bounds.getHeight());
    }

    @Override
    public void setSize(int width, int height) {
        this.bounds = new Rect2i(bounds.getX(), bounds.getY(), width, height);
    }

    @Override
    public Rect2i getBounds() {
        return bounds;
    }

    @Override
    public boolean onMouseWheel(Point mousePos, double delta) {
        scrollbar.onMouseWheel(mousePos, delta);
        return true;
    }

    @Nullable
    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        var cpu = hitTestCpu(new Point(mouseX, mouseY));
        if (cpu != null) {
            var tooltipLines = new ArrayList<Component>();
            tooltipLines.add(getCpuName(cpu));

            var coProcessors = cpu.coProcessors();
            if (coProcessors == 1) {
                tooltipLines.add(ButtonToolTips.CpuStatusCoProcessor.text(Tooltips.ofNumber(coProcessors))
                        .withStyle(ChatFormatting.GRAY));
            } else if (coProcessors > 1) {
                tooltipLines.add(ButtonToolTips.CpuStatusCoProcessors.text(Tooltips.ofNumber(coProcessors))
                        .withStyle(ChatFormatting.GRAY));
            }

            tooltipLines.add(ButtonToolTips.CpuStatusStorage.text(Tooltips.ofBytes(cpu.storage()))
                    .withStyle(ChatFormatting.GRAY));

            var modeText =
                    switch (cpu.mode()) {
                        case PLAYER_ONLY -> ButtonToolTips.CpuSelectionModePlayersOnly.text();
                        case MACHINE_ONLY -> ButtonToolTips.CpuSelectionModeAutomationOnly.text();
                        default -> null;
                    };
            if (modeText != null) {
                tooltipLines.add(modeText);
            }

            var currentJob = cpu.currentJob();
            if (currentJob != null) {
                tooltipLines.add(ButtonToolTips.CpuStatusCrafting.text(Tooltips.ofAmount(currentJob))
                        .append(" ")
                        .append(currentJob.what().getDisplayName()));
                tooltipLines.add(ButtonToolTips.CpuStatusCraftedIn.text(
                        Tooltips.ofPercent(cpu.progress()),
                        Tooltips.ofDuration(cpu.elapsedTimeNanos(), TimeUnit.NANOSECONDS)));
            }
            return new Tooltip(tooltipLines);
        }
        return null;
    }

    @Override
    public boolean onMouseUp(Point mousePos, int button) {
        var cpu = hitTestCpu(mousePos);
        if (cpu != null) {
            menu.selectCpu(cpu.serial());
            return true;
        }

        return false;
    }

    @Nullable
    private QuantumComputerMenu.CraftingCpuListEntry hitTestCpu(Point mousePos) {
        var relX = mousePos.getX() - bounds.getX();
        var relY = mousePos.getY() - bounds.getY();
        relX -= 8;
        if (relX < 0 || relX >= buttonBg.getSrcWidth()) {
            return null;
        }

        relY -= 19;
        var buttonIdx = scrollbar.getCurrentScroll() + relY / (buttonBg.getSrcHeight() + 1);
        if (relY % (buttonBg.getSrcHeight() + 1) == buttonBg.getSrcHeight()) {
            return null;
        }
        if (relY < 0 || buttonIdx >= menu.cpuList.cpus().size()) {
            return null;
        }

        var cpus = menu.cpuList.cpus();
        if (buttonIdx >= 0 && buttonIdx < cpus.size()) {
            return cpus.get(buttonIdx);
        }

        return null;
    }

    @Override
    public void updateBeforeRender() {
        var hiddenRows = Math.max(0, menu.cpuList.cpus().size() - ROWS);
        scrollbar.setRange(0, hiddenRows, ROWS / 3);
    }

    @Override
    public void drawBackgroundLayer(PoseStack poseStack, int zIndex, Rect2i bounds, Point mouse) {
        var x = bounds.getX() + this.bounds.getX();
        var y = bounds.getY() + this.bounds.getY();
        background.dest(x, y, this.bounds.getWidth(), this.bounds.getHeight()).blit(poseStack, 0);

        x += 8;
        y += 19;

        var font = Minecraft.getInstance().font;
        var cpus = menu.cpuList
                .cpus()
                .subList(
                        Mth.clamp(
                                scrollbar.getCurrentScroll(),
                                0,
                                menu.cpuList.cpus().size()),
                        Mth.clamp(
                                scrollbar.getCurrentScroll() + ROWS,
                                0,
                                menu.cpuList.cpus().size()));

        for (var cpu : cpus) {
            if (cpu.serial() == menu.getSelectedCpuSerial()) {
                buttonBgSelected.dest(x, y).blit(poseStack, 0);
            } else {
                buttonBg.dest(x, y).blit(poseStack, 0);
            }

            var name = getCpuName(cpu);
            poseStack.pushPose();
            poseStack.translate(x + 3, y + 2, 0);
            poseStack.scale(0.666f, 0.666f, 1);
            font.draw(poseStack, name, 0, 0, textColor.toARGB());
            poseStack.popPose();

            var infoBar = new InfoBar();
            var currentJob = cpu.currentJob();
            if (currentJob != null) {
                infoBar.add(Icon.ENTER, 1f, 2, 9);
                var craftAmt = currentJob.what().formatAmount(currentJob.amount(), AmountFormat.SLOT);
                infoBar.add(craftAmt, textColor.toARGB(), 0.666f, 14, 13);
                infoBar.add(currentJob.what(), 0.666f, 55, 9);

                var progress = (int) (cpu.progress() * (buttonBg.getSrcWidth() - 1));
                poseStack.pushPose();
                poseStack.translate(1, -1, 0);
                GuiComponent.fill(
                    poseStack,
                        x,
                        y + buttonBg.getSrcHeight() - 2,
                        x + progress,
                        y + buttonBg.getSrcHeight() - 1,
                        menu.getSelectedCpuSerial() == cpu.serial() ? 0xFF7da9d2 : selectedColor);
                poseStack.popPose();
            } else {
                infoBar.add(Icon.BACKGROUND_STORAGE_CELL, 1f, 32, 9);

                String storageAmount = formatStorage(cpu);
                infoBar.add(storageAmount, textColor.toARGB(), 0.666f, 44, 13);

                if (cpu.coProcessors() > 0) {
                    infoBar.add(Icon.LEVEL_ITEM, 1f, 2, 9);
                    String coProcessorCount = String.valueOf(cpu.coProcessors());
                    infoBar.add(coProcessorCount, textColor.toARGB(), 0.666f, 14, 13);
                }

                switch (cpu.mode()) {
                    case PLAYER_ONLY -> infoBar.add(Icon.TERMINAL_STYLE_SMALL, 1f, 55, 9);
                    case MACHINE_ONLY -> infoBar.add(Icon.WRENCH, 1f, 55, 9);
                    default -> {
                    }
                }
            }

            infoBar.render(poseStack, x, y);

            y += buttonBg.getSrcHeight() + 1;
        }
    }

    private String formatStorage(QuantumComputerMenu.CraftingCpuListEntry cpu) {
        var storage = cpu.storage();
        var unit = -1;

        while (storage > 1024) {
            storage /= 1024;
            unit++;
        }

        return storage
                + switch (unit) {
                    case 0 -> "k";
                    case 1 -> "M";
                    case 2 -> "G";
                    default -> "T";
                };
    }

    private Component getCpuName(QuantumComputerMenu.CraftingCpuListEntry cpu) {
        return cpu.name() != null ? cpu.name() : GuiText.CPUs.text().append(String.format(" #%d", cpu.serial()));
    }
}
