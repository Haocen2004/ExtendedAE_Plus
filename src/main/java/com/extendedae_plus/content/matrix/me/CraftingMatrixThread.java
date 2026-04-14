package com.extendedae_plus.content.matrix.me;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class CraftingMatrixThread extends CraftingThread {

    private static final int COOL_TIME = 5 * 20;
    private int blockCoolDown = 0;
    private final Supplier<IActionSource> sourceGetter;

    public CraftingMatrixThread(@NotNull AEBaseBlockEntity host, @NotNull Supplier<IActionSource> sourceGetter, SignalAccepter accepter) {
        super(host, accepter);
        this.sourceGetter = sourceGetter;
    }

    @Override
    protected boolean hasMats() {
        if (this.myPlan == null) {
            return false;
        }
        return !this.gridInv.isEmpty();
    }

    @Override
    protected void ejectHeldItems() {
        if (this.gridInv.getStackInSlot(9).isEmpty()) {
            for (int x = 0; x < 9; x++) {
                final ItemStack is = this.gridInv.getStackInSlot(x);
                if (!is.isEmpty()) {
                    this.gridInv.setItemDirect(9, is);
                    this.gridInv.setItemDirect(x, ItemStack.EMPTY);
                    this.saveChanges();
                    return;
                }
            }
        }
    }

    @Override
    public TickRateModulation tick(int cards, int ticksSinceLastCall) {
        if (this.blockCoolDown > 0) {
            this.blockCoolDown -= ticksSinceLastCall;
            return TickRateModulation.SAME;
        } else {
            return super.tick(cards, ticksSinceLastCall);
        }
    }

    @Override
    protected ItemStack assemblePattern(CraftingContainer container) {
        // In 1.19.2 we don't have Ae2Reflect, just use default assembly
        return super.assemblePattern(container);
    }

    @Override
    protected void pushOut(ItemStack output) {
        output = this.pushToNetwork(output);
        if (!output.isEmpty()) {
            this.blockCoolDown = COOL_TIME;
        }
        if (output.isEmpty() && this.forcePlan) {
            this.forcePlan = false;
            this.recalculatePlan();
        }
        this.gridInv.setItemDirect(9, output);
    }

    private ItemStack pushToNetwork(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var grid = this.gridHost.getMainNode().getGrid();
        if (grid != null) {
            var storage = grid.getService(IStorageService.class);
            var added = storage.getInventory().insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, this.sourceGetter.get());
            if (added == 0) {
                return stack;
            }
            this.saveChanges();
            if (added != stack.getCount()) {
                var copy = stack.copy();
                copy.setCount((int) (stack.getCount() - added));
                return copy;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }
}
