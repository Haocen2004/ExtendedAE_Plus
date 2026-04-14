package com.extendedae_plus.content.matrix.gui;

import appeng.api.inventories.InternalInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A virtual slot used by AssemblerMatrixScreen to display pattern items.
 * Not backed by a real container slot - interactions are handled via InventoryAction packets.
 */
public class AssemblerMatrixSlot extends Slot {

    private static final EmptyContainer EMPTY = new EmptyContainer();
    private final InternalInventory inv;
    private final int actualSlot;
    private final int offset;
    private final long id;

    public AssemblerMatrixSlot(InternalInventory inv, int col, int offset, long id, int x, int y) {
        super(EMPTY, 0, x, y);
        this.inv = inv;
        this.actualSlot = col + offset;
        this.offset = offset;
        this.id = id;
    }

    @Override
    public ItemStack getItem() {
        return this.inv.getStackInSlot(this.actualSlot - this.offset);
    }

    @Override
    public void set(ItemStack stack) {
        // NO-OP: handled by server via doAction
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    public int getActuallySlot() {
        return this.actualSlot;
    }

    public long getID() {
        return this.id;
    }

    private static class EmptyContainer implements net.minecraft.world.Container {
        @Override
        public int getContainerSize() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }

        @Override
        public void clearContent() {
        }
    }
}
