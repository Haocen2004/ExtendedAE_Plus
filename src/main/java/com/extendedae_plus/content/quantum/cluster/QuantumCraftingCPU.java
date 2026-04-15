package com.extendedae_plus.content.quantum.cluster;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import appeng.api.config.CpuSelectionMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CraftingJobStatus;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.ElapsedTimeTracker;
import appeng.crafting.inv.ListCraftingInventory;

/**
 * Represents a single crafting job slot in the Quantum Computer.
 * Each QuantumCraftingCPU wraps a QuantumCraftingCPULogic and a portion of storage.
 * Implements ICraftingCPU so CraftingLink can use it.
 * Backported from AdvancedAE's AdvCraftingCPU.
 */
public class QuantumCraftingCPU implements ICraftingCPU {

    final UUID uniqueId;
    final long bytes;
    private final QuantumCPUCluster cluster;
    public final QuantumCraftingCPULogic craftingLogic = new QuantumCraftingCPULogic(this);
    public GenericStack finalOutput;
    private boolean markedForDeletion = false;

    public QuantumCraftingCPU(QuantumCPUCluster cluster, UUID uniqueId, long bytes) {
        this.uniqueId = uniqueId;
        this.cluster = cluster;
        this.bytes = bytes;
    }

    /** Constructor for remaining-capacity display CPU (no UUID) */
    protected QuantumCraftingCPU(QuantumCPUCluster cluster, long storage) {
        this.uniqueId = null;
        this.cluster = cluster;
        this.bytes = storage;
    }

    @Override
    public boolean isBusy() {
        return craftingLogic.hasJob();
    }

    @Override
    public @Nullable CraftingJobStatus getJobStatus() {
        var finalOutput = craftingLogic.getFinalJobOutput();
        if (finalOutput != null) {
            var elapsedTimeTracker = craftingLogic.getElapsedTimeTracker();
            var progress = Math.max(0,
                    elapsedTimeTracker.getStartItemCount() - elapsedTimeTracker.getRemainingItemCount());
            return new CraftingJobStatus(
                    finalOutput, elapsedTimeTracker.getStartItemCount(), progress, elapsedTimeTracker.getElapsedTime());
        } else {
            return null;
        }
    }

    public void cancelJob() {
        if (this.uniqueId == null) {
            return;
        }
        craftingLogic.cancel();
        this.cluster.cancelJob(uniqueId);
    }

    @Override
    public long getAvailableStorage() {
        return this.bytes;
    }

    @Override
    public int getCoProcessors() {
        return cluster.getCoProcessors();
    }

    @Override
    public @Nullable Component getName() {
        return cluster.getName();
    }

    @Override
    public CpuSelectionMode getSelectionMode() {
        return cluster.getSelectionMode();
    }

    public QuantumCPUCluster getCluster() {
        return this.cluster;
    }

    public void markDirty() {
        cluster.markDirty();
    }

    public boolean isActive() {
        return cluster.isActive();
    }

    public Level getLevel() {
        return cluster.getLevel();
    }

    public IGrid getGrid() {
        return cluster.getGrid();
    }

    public void updateOutput(GenericStack stack) {
        finalOutput = stack;
    }

    public ListCraftingInventory getInventory() {
        return craftingLogic.getInventory();
    }

    public void deactivate() {
        cluster.deactivate(uniqueId);
    }

    public IActionSource getSrc() {
        return cluster.getSrc();
    }

    public void markForDeletion() {
        this.markedForDeletion = true;
    }

    public boolean isMarkedForDeletion() {
        return this.markedForDeletion;
    }

    public void writeToNBT(CompoundTag data) {
        craftingLogic.writeToNBT(data);
    }

    public void readFromNBT(CompoundTag data) {
        craftingLogic.readFromNBT(data);
    }
}
