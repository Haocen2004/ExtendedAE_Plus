package com.extendedae_plus.mixin.extendedae.client.gui;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;
import appeng.menu.SlotSemantics;
import com.extendedae_plus.api.IExPatternButton;
import com.extendedae_plus.api.IExPatternPage;
import com.extendedae_plus.client.widget.BlitterIconButton;
import com.extendedae_plus.config.ModConfig;
import com.extendedae_plus.init.ModNetwork;
import com.extendedae_plus.network.provider.PatternScaleC2SPacket;
import com.extendedae_plus.util.ScaleButtonHelper;
import com.github.glodblock.epp.client.gui.GuiExPatternProvider;
import com.github.glodblock.epp.container.ContainerExPatternProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings({"AddedMixinMembersNamePattern"})
@Mixin(GuiExPatternProvider.class)
public abstract class GuiExPatternProviderMixin extends PatternProviderScreen<ContainerExPatternProvider> implements IExPatternButton, IExPatternPage {
    private static final int SLOTS_PER_PAGE = 36;
    @Unique public IconButton nextPage;
    @Unique public IconButton prevPage;
    @Unique private int eap$lastScreenWidth = -1;
    @Unique private int eap$lastScreenHeight = -1;
    @Unique private int eap$currentPage = 0;
    @Unique private int eap$maxPageLocal = 1;
    @Unique
    private List<BlitterIconButton> scaleButtons;

    public GuiExPatternProviderMixin(ContainerExPatternProvider menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Override
    public int eap$getCurrentPage() {
        return Math.max(0, eap$currentPage % Math.max(1, eap$maxPageLocal));
    }

    @Override
    public int eap$getMaxPageLocal() {
        return this.eap$maxPageLocal;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectInit(ContainerExPatternProvider menu, Inventory playerInventory, Component title, ScreenStyle style, CallbackInfo ci) {
        this.eap$maxPageLocal = ModConfig.INSTANCE.pageMultiplier;
        int slotPageSize = (this.menu.getSlots(SlotSemantics.ENCODED_PATTERN).size() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE;
        this.eap$maxPageLocal = Math.max(Math.max(1, slotPageSize), this.eap$maxPageLocal);

        if (eap$maxPageLocal > 1) {
            this.prevPage = new BlitterIconButton((b) -> {
                int currentPage = eap$getCurrentPage();
                int maxPage = this.eap$maxPageLocal;
                this.eap$currentPage = (currentPage - 1 + maxPage) % maxPage;
                this.repositionSlots(SlotSemantics.ENCODED_PATTERN);
                this.repositionSlots(SlotSemantics.STORAGE);
                this.hoveredSlot = null;
            }, Icon.ARROW_LEFT.getBlitter());

            this.nextPage = new BlitterIconButton((b) -> {
                int currentPage = eap$getCurrentPage();
                int maxPage = this.eap$maxPageLocal;
                this.eap$currentPage = (currentPage + 1) % maxPage;
                this.repositionSlots(SlotSemantics.ENCODED_PATTERN);
                this.repositionSlots(SlotSemantics.STORAGE);
                this.hoveredSlot = null;
            }, Icon.ARROW_RIGHT.getBlitter());

            this.addToLeftToolbar(this.nextPage);
            this.addToLeftToolbar(this.prevPage);
        }

        // Use ScaleButtonHelper to create and layout buttons; send our own packet
        this.scaleButtons = ScaleButtonHelper.createAndLayout(
                this.leftPos + this.imageWidth,
                this.topPos + 50,
                22,
                ScaleButtonHelper.Side.RIGHT,
                (divide, factor) -> {
                    ModNetwork.CHANNEL.sendToServer(new PatternScaleC2SPacket(factor, divide));
                }
        );

        this.scaleButtons.forEach(this::addRenderableWidget);
    }

    @Override
    public void eap$updateButtonsLayout() {
        for (BlitterIconButton b : scaleButtons) {
            if (b != null) {
                b.setVisibility(true);
                if (!this.renderables.contains(b)) this.addRenderableWidget(b);
            }
        }

        if (this.width != eap$lastScreenWidth || this.height != eap$lastScreenHeight) {
            eap$lastScreenWidth = this.width;
            eap$lastScreenHeight = this.height;
            for (BlitterIconButton b : scaleButtons) {
                if (b != null) {
                    this.removeWidget(b);
                    this.addRenderableWidget(b);
                }
            }
        }

        if (!scaleButtons.isEmpty()) {
            ScaleButtonHelper.layoutButtons(
                    new ScaleButtonHelper.ScaleButtonSet(
                            scaleButtons.get(1),
                            scaleButtons.get(0),
                            scaleButtons.get(3),
                            scaleButtons.get(2),
                            scaleButtons.get(5),
                            scaleButtons.get(4)
                    ),
                    this.leftPos + this.imageWidth,
                    this.topPos + 50,
                    22,
                    ScaleButtonHelper.Side.RIGHT
            );
        }
    }
}
