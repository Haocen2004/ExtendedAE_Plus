package com.extendedae_plus.mixin.ae2.quantum;

import java.util.function.Consumer;

import com.extendedae_plus.content.quantum.cluster.QuantumCraftingCPU;
import com.extendedae_plus.content.quantum.cluster.QuantumCraftingCPULogic;
import com.extendedae_plus.content.quantum.entity.QuantumCraftingBlockEntity;
import com.google.common.collect.ImmutableList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.sync.packets.CraftingStatusPacket;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.common.IncrementalUpdateHelper;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.menu.me.crafting.CraftingStatus;
import appeng.menu.me.crafting.CraftingStatusEntry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

@Mixin(value = CraftingCPUMenu.class, remap = false)
public abstract class CraftingCPUMenuQuantumMixin extends AEBaseMenu {

    @Final
    @Shadow
    private IncrementalUpdateHelper incrementalUpdateHelper;

    @Shadow
    private CraftingCPUCluster cpu;

    @Final
    @Shadow
    private Consumer<AEKey> cpuChangeListener;

    @Unique
    private QuantumCraftingCPU extendedae_plus$quantumCpu = null;

    @Shadow
    protected abstract void setCPU(ICraftingCPU cpu);

    protected CraftingCPUMenuQuantumMixin(MenuType<?> menuType, int id, Inventory playerInventory, Object host) {
        super(menuType, id, playerInventory, host);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Ljava/lang/Object;)V",
            at = @At("TAIL"))
    private void extendedae_plus$onInit(MenuType<?> menuType, int id, Inventory ip, Object te, CallbackInfo ci) {
        if (!(te instanceof QuantumCraftingBlockEntity quantumEntity)) {
            return;
        }

        var cluster = quantumEntity.getCluster();
        if (cluster == null) {
            return;
        }

        var active = cluster.getActiveCPUs();
        if (!active.isEmpty()) {
            this.setCPU(active.get(0));
        } else {
            this.setCPU(cluster.getRemainingCapacityCPU());
        }
    }

    @Inject(method = "setCPU(Lappeng/api/networking/crafting/ICraftingCPU;)V", at = @At("HEAD"), cancellable = true)
    private void extendedae_plus$onSetCPU(ICraftingCPU c, CallbackInfo ci) {
        if (this.extendedae_plus$quantumCpu != null) {
            this.extendedae_plus$quantumCpu.craftingLogic.removeListener(cpuChangeListener);
        }

        if (!(c instanceof QuantumCraftingCPU quantumCpu)) {
            this.extendedae_plus$quantumCpu = null;
            return;
        }

        if (this.cpu != null) {
            this.cpu.craftingLogic.removeListener(cpuChangeListener);
        }

        this.incrementalUpdateHelper.reset();
        this.extendedae_plus$quantumCpu = quantumCpu;

        // CPU switched: send full status payload once.
        var allItems = new KeyCounter();
        this.extendedae_plus$quantumCpu.craftingLogic.getAllItems(allItems);
        for (var entry : allItems) {
            incrementalUpdateHelper.addChange(entry.getKey());
        }

        this.extendedae_plus$quantumCpu.craftingLogic.addListener(cpuChangeListener);
        ci.cancel();
    }

    @Inject(method = "cancelCrafting", at = @At("TAIL"))
    private void extendedae_plus$onCancelCrafting(CallbackInfo ci) {
        if (isServerSide() && this.extendedae_plus$quantumCpu != null) {
            this.extendedae_plus$quantumCpu.cancelJob();
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void extendedae_plus$onRemoved(Player player, CallbackInfo ci) {
        if (this.extendedae_plus$quantumCpu != null) {
            this.extendedae_plus$quantumCpu.craftingLogic.removeListener(cpuChangeListener);
        }
    }

    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    private void extendedae_plus$onBroadcastChanges(CallbackInfo ci) {
        if (!isServerSide() || this.extendedae_plus$quantumCpu == null) {
            return;
        }

        ((CraftingCPUMenu) (Object) this).schedulingMode = this.extendedae_plus$quantumCpu.getSelectionMode();
        ((CraftingCPUMenu) (Object) this).cantStoreItems = this.extendedae_plus$quantumCpu.craftingLogic.isCantStoreItems();

        if (this.incrementalUpdateHelper.hasChanges()) {
            var status = extendedae_plus$create(this.incrementalUpdateHelper, this.extendedae_plus$quantumCpu.craftingLogic);
            this.incrementalUpdateHelper.commitChanges();
            sendPacketToClient(new CraftingStatusPacket(status));
        }
    }

    @Unique
    private static CraftingStatus extendedae_plus$create(IncrementalUpdateHelper changes, QuantumCraftingCPULogic logic) {
        boolean full = changes.isFullUpdate();

        ImmutableList.Builder<CraftingStatusEntry> newEntries = ImmutableList.builder();
        for (var what : changes) {
            long storedCount = logic.getStored(what);
            long activeCount = logic.getWaitingFor(what);
            long pendingCount = logic.getPendingOutputs(what);

            var sentStack = what;
            if (!full && changes.getSerial(what) != null) {
                sentStack = null;
            }

            var entry = new CraftingStatusEntry(
                    changes.getOrAssignSerial(what),
                    sentStack,
                    storedCount,
                    activeCount,
                    pendingCount);
            newEntries.add(entry);

            if (entry.isDeleted()) {
                changes.removeSerial(what);
            }
        }

        var elapsed = logic.getElapsedTimeTracker();
        return new CraftingStatus(
                full,
                elapsed.getElapsedTime(),
                elapsed.getRemainingItemCount(),
                elapsed.getStartItemCount(),
                newEntries.build());
    }
}