package com.extendedae_plus.mixin.ae2.quantum;

import java.util.*;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.extendedae_plus.content.quantum.cluster.QuantumCPUCluster;
import com.extendedae_plus.content.quantum.cluster.QuantumCraftingCPU;
import com.extendedae_plus.content.quantum.entity.QuantumCraftingBlockEntity;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.events.GridCraftingCpuChange;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingLink;
import appeng.crafting.execution.CraftingSubmitResult;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;

/**
 * Mixin to integrate Quantum Computer clusters into AE2's CraftingService.
 * Allows quantum CPUs to be discovered, submit jobs, and participate in crafting operations.
 */
@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceQuantumMixin {

    @Unique
    private static final Comparator<QuantumCPUCluster> extendedae_plus$FAST_FIRST_COMPARATOR = Comparator
            .comparingInt(QuantumCPUCluster::getCoProcessors)
            .reversed()
            .thenComparingLong(QuantumCPUCluster::getAvailableStorage);

    @Unique
    private final Set<QuantumCPUCluster> extendedae_plus$quantumClusters = new HashSet<>();

    @Final
    @Shadow
    private IGrid grid;

    @Final
    @Shadow
    private IEnergyService energyGrid;

    @Final
    @Shadow
    private Set<AEKey> currentlyCrafting;

    @Shadow
    public abstract void addLink(CraftingLink link);

    // ==================== onServerEndTick ====================

    /**
     * Tick quantum clusters at the start of onServerEndTick.
     */
    @Inject(method = "onServerEndTick", at = @At("HEAD"))
    private void extendedae_plus$tickQuantumClustersHead(CallbackInfo ci) {
        for (var cluster : this.extendedae_plus$quantumClusters) {
            if (cluster != null) {
                for (var cpu : cluster.getActiveCPUs()) {
                    cpu.craftingLogic.tickCraftingLogic(energyGrid, (CraftingService) (Object) this);
                }
            }
        }
    }

    /**
     * Add quantum clusters' waiting items to currentlyCrafting set.
     */
    @Inject(method = "onServerEndTick", at = @At("RETURN"))
    private void extendedae_plus$tickQuantumClustersTail(CallbackInfo ci) {
        for (var cluster : this.extendedae_plus$quantumClusters) {
            if (cluster != null) {
                for (var cpu : cluster.getActiveCPUs()) {
                    cpu.craftingLogic.getAllWaitingFor(this.currentlyCrafting);
                }
            }
        }
    }

    // ==================== Node Add/Remove ====================

    @Inject(method = "removeNode", at = @At("TAIL"))
    private void extendedae_plus$onRemoveNode(IGridNode gridNode, CallbackInfo ci) {
        if (gridNode.getOwner() instanceof QuantumCraftingBlockEntity) {
            extendedae_plus$markCpuListDirty(gridNode);
        }
    }

    @Inject(method = "addNode", at = @At("TAIL"))
    private void extendedae_plus$onAddNode(IGridNode gridNode, CallbackInfo ci) {
        if (gridNode.getOwner() instanceof QuantumCraftingBlockEntity) {
            extendedae_plus$markCpuListDirty(gridNode);
        }
    }

    @Unique
    private void extendedae_plus$markCpuListDirty(IGridNode node) {
        if (node != null && node.getGrid() != null) {
            node.getGrid().postEvent(new GridCraftingCpuChange(node));
        }
    }

    // ==================== updateCPUClusters ====================

    @Inject(method = "updateCPUClusters", at = @At("TAIL"))
    private void extendedae_plus$onUpdateCPUClusters(CallbackInfo ci) {
        this.extendedae_plus$quantumClusters.clear();

        // Primary path: use getMachines
        for (var blockEntity : this.grid.getMachines(QuantumCraftingBlockEntity.class)) {
            extendedae_plus$addClusterIfValid(blockEntity);
        }

        // Backup path: if getMachines returns empty due to grid caching issues,
        // directly iterate all nodes to find quantum block entities
        if (this.extendedae_plus$quantumClusters.isEmpty()) {
            for (var node : this.grid.getNodes()) {
                var owner = node.getOwner();
                if (owner instanceof QuantumCraftingBlockEntity blockEntity) {
                    extendedae_plus$addClusterIfValid(blockEntity);
                }
            }
        }
    }

    @Unique
    private void extendedae_plus$addClusterIfValid(QuantumCraftingBlockEntity blockEntity) {
        final QuantumCPUCluster cluster = blockEntity.getCluster();
        if (cluster != null && !this.extendedae_plus$quantumClusters.contains(cluster)) {
            this.extendedae_plus$quantumClusters.add(cluster);

            for (var cpu : cluster.getActiveCPUs()) {
                ICraftingLink maybeLink = cpu.craftingLogic.getLastLink();
                if (maybeLink != null) {
                    this.addLink((CraftingLink) maybeLink);
                }
            }
        }
    }

    // ==================== submitJob ====================

    /**
     * Intercept job submission to handle quantum CPU target or find suitable quantum cluster.
     */
    @Inject(method = "submitJob", at = @At("HEAD"), cancellable = true)
    private void extendedae_plus$onSubmitJob(
            ICraftingPlan job,
            ICraftingRequester requestingMachine,
            ICraftingCPU target,
            boolean prioritizePower,
            IActionSource src,
            CallbackInfoReturnable<ICraftingSubmitResult> cir) {

        // If target is explicitly a quantum cluster, use it
        if (target instanceof QuantumCPUCluster quantumCluster) {
            cir.setReturnValue(quantumCluster.submitJob(this.grid, job, src, requestingMachine));
            return;
        }

        // If target points to a specific quantum CPU instance, submit via its owning cluster
        if (target instanceof QuantumCraftingCPU quantumCpu) {
            cir.setReturnValue(quantumCpu.getCluster().submitJob(this.grid, job, src, requestingMachine));
            return;
        }

        // If no specific target, try to find a suitable quantum cluster first
        if (target == null) {
            var unsuitableCpusResult = new MutableObject<UnsuitableCpus>();
            var quantumCluster = extendedae_plus$findSuitableQuantumCPU(job, src, unsuitableCpusResult);
            if (quantumCluster != null) {
                var node = quantumCluster.getNode();
                if (node != null) {
                    extendedae_plus$markCpuListDirty(node);
                }
                cir.setReturnValue(quantumCluster.submitJob(this.grid, job, src, requestingMachine));
            }
            // Otherwise fall through to vanilla AE2 logic
        }
    }

    @Unique
    private QuantumCPUCluster extendedae_plus$findSuitableQuantumCPU(
            ICraftingPlan job, IActionSource src, MutableObject<UnsuitableCpus> unsuitableCpusResult) {
        var validCpusClusters = new ArrayList<QuantumCPUCluster>(this.extendedae_plus$quantumClusters.size());
        int offline = 0;
        int tooSmall = 0;
        int excluded = 0;

        for (var cluster : this.extendedae_plus$quantumClusters) {
            if (!cluster.isActive()) {
                offline++;
                continue;
            }
            if (cluster.getAvailableStorage() < job.bytes()) {
                tooSmall++;
                continue;
            }
            if (!cluster.canBeAutoSelectedFor(src)) {
                excluded++;
                continue;
            }
            validCpusClusters.add(cluster);
        }

        if (validCpusClusters.isEmpty()) {
            if (offline > 0 || tooSmall > 0 || excluded > 0) {
                unsuitableCpusResult.setValue(new UnsuitableCpus(offline, 0, tooSmall, excluded));
            }
            return null;
        }

        validCpusClusters.sort((a, b) -> {
            // Prioritize sorting by selected mode
            var firstPreferred = a.isPreferredFor(src);
            var secondPreferred = b.isPreferredFor(src);
            if (firstPreferred != secondPreferred) {
                return Boolean.compare(secondPreferred, firstPreferred);
            }
            return extendedae_plus$FAST_FIRST_COMPARATOR.compare(a, b);
        });

        return validCpusClusters.get(0);
    }

    // ==================== insertIntoCpus ====================

    @Inject(method = "insertIntoCpus", at = @At("RETURN"), cancellable = true)
    private void extendedae_plus$onInsertIntoCpus(
            AEKey what,
            long amount,
            Actionable type,
            CallbackInfoReturnable<Long> cir) {
        long inserted = cir.getReturnValue();

        for (var cluster : this.extendedae_plus$quantumClusters) {
            if (cluster != null) {
                for (var cpu : cluster.getActiveCPUs()) {
                    inserted += cpu.craftingLogic.insert(what, amount - inserted, type);
                }
            }
        }

        cir.setReturnValue(inserted);
    }

    // ==================== getCpus ====================

    @Inject(method = "getCpus", at = @At("RETURN"), cancellable = true)
    private void extendedae_plus$onGetCpus(CallbackInfoReturnable<ImmutableSet<ICraftingCPU>> cir) {
        var builder = ImmutableSet.<ICraftingCPU>builder();
        builder.addAll(cir.getReturnValue());

        for (var cluster : this.extendedae_plus$quantumClusters) {
            for (var cpu : cluster.getActiveCPUs()) {
                builder.add(cpu);
            }
            builder.add(cluster.getRemainingCapacityCPU());
        }

        cir.setReturnValue(builder.build());
    }

    // ==================== getRequestedAmount ====================

    @Inject(method = "getRequestedAmount", at = @At("RETURN"), cancellable = true)
    private void extendedae_plus$onGetRequestedAmount(AEKey what, CallbackInfoReturnable<Long> cir) {
        long requested = cir.getReturnValue();

        for (var cluster : this.extendedae_plus$quantumClusters) {
            for (var cpu : cluster.getActiveCPUs()) {
                requested += cpu.craftingLogic.getWaitingFor(what);
            }
        }

        cir.setReturnValue(requested);
    }

    // ==================== hasCpu ====================

    @Inject(method = "hasCpu", at = @At("HEAD"), cancellable = true)
    private void extendedae_plus$onHasCpu(ICraftingCPU cpu, CallbackInfoReturnable<Boolean> cir) {
        for (var cluster : this.extendedae_plus$quantumClusters) {
            for (var activeCpu : cluster.getActiveCPUs()) {
                if (activeCpu == cpu) {
                    cir.setReturnValue(true);
                    return;
                }
            }
            if (cluster.getRemainingCapacityCPU() == cpu) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
