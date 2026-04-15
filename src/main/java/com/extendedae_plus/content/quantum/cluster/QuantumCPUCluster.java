package com.extendedae_plus.content.quantum.cluster;

import com.extendedae_plus.content.quantum.entity.QuantumCraftingBlockEntity;

import appeng.api.config.CpuSelectionMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.events.GridCraftingCpuChange;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.api.util.IConfigManager;
import appeng.crafting.execution.CraftingSubmitResult;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.util.ConfigManager;
import appeng.me.cluster.IAECluster;
import appeng.me.cluster.MBCalculator;
import appeng.me.helpers.MachineSource;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Cluster that manages the Quantum Computer multiblock.
 * Supports multiple concurrent crafting jobs via QuantumCraftingCPU instances.
 * Backported from AdvancedAE's AdvCraftingCPUCluster.
 */
public class QuantumCPUCluster implements IAECluster {

    private final BlockPos boundsMin;
    private final BlockPos boundsMax;

    private final HashMap<UUID, QuantumCraftingCPU> activeCpus = new HashMap<>();
    private QuantumCraftingCPU remainingStorageCpu;
    private final List<QuantumCraftingBlockEntity> blockEntities = new ArrayList<>();
    private final IConfigManager configManager;
    private Component myName = null;
    private boolean isDestroyed = false;
    private long storage = 0;
    private long storageMultiplier = 0;
    private long remainingStorage = 0;
    private MachineSource machineSrc = null;
    private int accelerator = 0;
    private int acceleratorMultiplier = 0;

    public QuantumCPUCluster(BlockPos boundsMin, BlockPos boundsMax) {
        this.boundsMin = boundsMin.immutable();
        this.boundsMax = boundsMax.immutable();
        var cm = new ConfigManager(this::markDirty);
        cm.registerSetting(Settings.CPU_SELECTION_MODE, CpuSelectionMode.ANY);
        this.configManager = cm;
    }

    @Override
    public boolean isDestroyed() {
        return this.isDestroyed;
    }

    @Override
    public BlockPos getBoundsMin() {
        return this.boundsMin;
    }

    @Override
    public BlockPos getBoundsMax() {
        return this.boundsMax;
    }

    @Override
    public void updateStatus(boolean b) {
        for (QuantumCraftingBlockEntity r : this.blockEntities) {
            r.updateSubType(true);
        }
    }

    @Override
    public void destroy() {
        if (this.isDestroyed) {
            return;
        }
        this.isDestroyed = true;

        boolean ownsModification = !MBCalculator.isModificationInProgress();
        if (ownsModification) {
            MBCalculator.setModificationInProgress(this);
        }
        try {
            updateGridForChangedCpu(null);
        } finally {
            if (ownsModification) {
                MBCalculator.setModificationInProgress(null);
            }
        }
    }

    private void updateGridForChangedCpu(QuantumCPUCluster cluster) {
        var posted = false;
        for (QuantumCraftingBlockEntity r : this.blockEntities) {
            final IGridNode n = r.getActionableNode();
            if (n != null && !posted) {
                n.getGrid().postEvent(new GridCraftingCpuChange(n));
                posted = true;
            }
            r.updateStatus(cluster);
        }
    }

    @Override
    public Iterator<QuantumCraftingBlockEntity> getBlockEntities() {
        return this.blockEntities.iterator();
    }

    public int numBlockEntities() {
        return this.blockEntities.size();
    }

    public List<ListCraftingInventory> getInventories() {
        List<ListCraftingInventory> list = new ArrayList<>();
        for (var cpu : this.activeCpus.values()) {
            list.add(cpu.getInventory());
        }
        return list;
    }

    void addBlockEntity(QuantumCraftingBlockEntity te) {
        if (this.machineSrc == null || te.isCoreBlock()) {
            this.machineSrc = new MachineSource(te);
        }

        te.setCoreBlock(false);
        te.saveChanges();
        this.blockEntities.add(0, te);

        if (te.getStorageBytes() > 0) {
            this.storage += te.getStorageBytes();
            recalculateRemainingStorage();
        }
        if (te.getStorageMultiplier() > 0) {
            this.storageMultiplier += te.getStorageMultiplier();
            recalculateRemainingStorage();
        }
        if (te.getAcceleratorThreads() > 0) {
            this.accelerator += te.getAcceleratorThreads();
        }
        if (te.getAccelerationMultiplier() > 0) {
            this.acceleratorMultiplier += te.getAccelerationMultiplier();
        }
    }

    public void recalculateRemainingStorage() {
        var totalStorage = this.storage;
        if (this.storageMultiplier > 0) totalStorage *= this.storageMultiplier;

        long usedStorage = 0;
        for (var cpu : this.activeCpus.values()) {
            usedStorage += cpu.getAvailableStorage();
        }

        this.remainingStorage = totalStorage - usedStorage;
    }

    public void markDirty() {
        var core = this.getCore();
        if (core != null) {
            core.saveChanges();
        }
    }

    public void updateOutput(GenericStack finalOutput) {
        // Quantum computer doesn't have a single monitor - each CPU tracks its own output
    }

    public IActionSource getSrc() {
        return Objects.requireNonNull(this.machineSrc);
    }

    @Nullable
    private QuantumCraftingBlockEntity getCore() {
        if (this.machineSrc == null) {
            return null;
        }
        return (QuantumCraftingBlockEntity) this.machineSrc.machine().orElse(null);
    }

    @Nullable
    public IGrid getGrid() {
        IGridNode node = getNode();
        return node != null ? node.getGrid() : null;
    }

    public void cancelJobs() {
        for (var id : new ArrayList<>(activeCpus.keySet())) {
            killCpu(id, false);
        }
    }

    public void cancelJob(UUID uniqueId) {
        var cpu = activeCpus.get(uniqueId);
        if (cpu != null) {
            killCpu(uniqueId);
        }
    }

    public ICraftingSubmitResult submitJob(
            IGrid grid, ICraftingPlan plan, IActionSource src, ICraftingRequester requestingMachine) {
        if (!isActive()) return CraftingSubmitResult.CPU_OFFLINE;
        if (getAvailableStorage() < plan.bytes()) return CraftingSubmitResult.CPU_TOO_SMALL;

        var uniqueId = UUID.randomUUID();
        var newCpu = new QuantumCraftingCPU(this, uniqueId, plan.bytes());

        var submitResult = newCpu.craftingLogic.trySubmitJob(grid, plan, src, requestingMachine);
        if (submitResult.successful()) {
            this.activeCpus.put(uniqueId, newCpu);
            recalculateRemainingStorage();
            updateGridForChangedCpu(this);
        }
        return submitResult;
    }

    private void killCpu(UUID id, boolean updateGrid) {
        var cpu = this.activeCpus.get(id);
        if (cpu == null) return;
        cpu.craftingLogic.cancel();
        cpu.craftingLogic.markForDeletion();
        recalculateRemainingStorage();
        if (updateGrid) {
            updateGridForChangedCpu(this);
        }
    }

    private void killCpu(UUID uniqueId) {
        killCpu(uniqueId, true);
    }

    protected void deactivate(UUID uniqueId) {
        this.activeCpus.remove(uniqueId);
        recalculateRemainingStorage();
        updateGridForChangedCpu(this);
    }

    public List<QuantumCraftingCPU> getActiveCPUs() {
        var list = new ArrayList<QuantumCraftingCPU>();
        var killList = new ArrayList<UUID>();
        for (var cpuEntry : activeCpus.entrySet()) {
            var cpu = cpuEntry.getValue();
            if (cpu.craftingLogic.hasJob() || cpu.craftingLogic.isMarkedForDeletion()) {
                list.add(cpu);
            } else {
                killList.add(cpuEntry.getKey());
            }
        }
        for (var cpuId : killList) {
            killCpu(cpuId);
        }
        return list;
    }

    public QuantumCraftingCPU getRemainingCapacityCPU() {
        if (this.remainingStorageCpu == null
                || this.remainingStorageCpu.getAvailableStorage() != this.remainingStorage) {
            this.remainingStorageCpu = new QuantumCraftingCPU(this, this.remainingStorage);
        }
        return this.remainingStorageCpu;
    }

    @Nullable
    public CraftingJobStatus getJobStatus(UUID uniqueId) {
        var cpu = activeCpus.get(uniqueId);
        if (cpu != null) {
            return cpu.getJobStatus();
        }
        return null;
    }

    public long getAvailableStorage() {
        return this.remainingStorage;
    }

    public int getCoProcessors() {
        var coprocessors = this.accelerator;
        if (this.acceleratorMultiplier > 0) coprocessors *= this.acceleratorMultiplier;
        return coprocessors;
    }

    public Component getName() {
        return this.myName;
    }

    @Nullable
    public IGridNode getNode() {
        QuantumCraftingBlockEntity core = getCore();
        return core != null ? core.getActionableNode() : null;
    }

    public boolean isActive() {
        IGridNode node = getNode();
        return node != null && node.isActive();
    }

    public void writeToNBT(CompoundTag data) {
        ListTag listCpus = new ListTag();
        for (var cpu : activeCpus.entrySet()) {
            if (cpu != null) {
                CompoundTag pair = new CompoundTag();
                pair.putString("key", cpu.getKey().toString());
                pair.putLong("bytes", cpu.getValue().getAvailableStorage());
                CompoundTag cpuTag = new CompoundTag();
                cpu.getValue().writeToNBT(cpuTag);
                pair.put("cpu", cpuTag);
                listCpus.add(pair);
            }
        }
        data.put("cpuList", listCpus);
        this.configManager.writeToNBT(data);
    }

    void done() {
        final QuantumCraftingBlockEntity core = this.getCore();
        if (core == null) return;

        core.setCoreBlock(true);

        if (core.getPreviousState() != null) {
            this.readFromNBT(core.getPreviousState());
            core.setPreviousState(null);
        }

        this.updateName();
    }

    public void readFromNBT(CompoundTag data) {
        ListTag cpuList = data.getList("cpuList", Tag.TAG_COMPOUND);
        for (var x = 0; x < cpuList.size(); x++) {
            CompoundTag pair = cpuList.getCompound(x);

            UUID id;
            long bytes;
            try {
                id = UUID.fromString(pair.getString("key"));
            } catch (IllegalArgumentException e) {
                id = UUID.randomUUID();
            }
            bytes = pair.getLong("bytes");

            var cpu = new QuantumCraftingCPU(this, id, bytes);
            this.activeCpus.put(id, cpu);
            cpu.readFromNBT(pair.getCompound("cpu"));
        }
        this.configManager.readFromNBT(data);
        recalculateRemainingStorage();
    }

    public void updateName() {
        this.myName = null;
        for (QuantumCraftingBlockEntity te : this.blockEntities) {
            if (te.hasCustomInventoryName()) {
                if (this.myName != null) {
                    this.myName = this.myName.copy().append(" ").append(te.getCustomInventoryName());
                } else {
                    this.myName = te.getCustomInventoryName().copy();
                }
            }
        }
    }

    public Level getLevel() {
        return this.getCore().getLevel();
    }

    public void breakCluster() {
        final QuantumCraftingBlockEntity t = this.getCore();
        if (t != null) {
            t.breakCluster();
        }
    }

    public CpuSelectionMode getSelectionMode() {
        return this.configManager.getSetting(Settings.CPU_SELECTION_MODE);
    }

    public IConfigManager getConfigManager() {
        return configManager;
    }

    public boolean canBeAutoSelectedFor(IActionSource source) {
        return switch (getSelectionMode()) {
            case ANY -> true;
            case PLAYER_ONLY -> source.player().isPresent();
            case MACHINE_ONLY -> source.player().isEmpty();
        };
    }

    public boolean isPreferredFor(IActionSource source) {
        return switch (getSelectionMode()) {
            case ANY -> false;
            case PLAYER_ONLY -> source.player().isPresent();
            case MACHINE_ONLY -> source.player().isEmpty();
        };
    }
}
