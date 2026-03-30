package com.extendedae_plus.mixin.ae2.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.helpers.iface.PatternProviderLogic;
import appeng.helpers.iface.PatternProviderLogicHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.extendedae_plus.ae.wireless.WirelessSlaveLink;
import com.extendedae_plus.ae.wireless.endpoint.GenericNodeEndpointImpl;
import com.extendedae_plus.api.bridge.IInterfaceWirelessLinkBridge;
import com.extendedae_plus.compat.PatternProviderLogicVirtualCompatBridge;
import com.extendedae_plus.compat.UpgradeSlotCompat;
import com.extendedae_plus.init.ModItems;
import com.extendedae_plus.mixin.ae2.accessor.CraftingCpuLogicAccessor;
import com.extendedae_plus.mixin.ae2.accessor.ExecutingCraftingJobAccessor;
import com.extendedae_plus.util.Logger;
import com.extendedae_plus.util.wireless.ChannelCardLinkHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/**
 * PatternProviderLogic的兼容性Mixin
 * 优先级设置为500，在appflux之前应用
 * 根据appflux是否存在来决定是否实现IUpgradeableObject接口
 */
@Mixin(value = PatternProviderLogic.class, priority = 500, remap = false)
public abstract class PatternProviderLogicCompatMixin implements IUpgradeableObject, IInterfaceWirelessLinkBridge, PatternProviderLogicVirtualCompatBridge {
    
    @Unique
    private IUpgradeInventory eap$compatUpgrades = UpgradeInventories.empty();

    @Unique
    private WirelessSlaveLink eap$compatLink;

    @Unique
    private long eap$compatLastChannel = -1;

    @Unique
    private UUID eap$compatLastOwner;

    @Unique
    private boolean eap$compatClientConnected = false;

    @Unique
    private boolean eap$compatHasInitialized = false;

    @Unique
    private int eap$compatDelayedInitTicks = 0;

    @Final
    @Shadow
    private PatternProviderLogicHost host;

    @Final
    @Shadow
    private IManagedGridNode mainNode;

    @Final
    @Shadow
    private IActionSource actionSource;

    @Unique
    private boolean eap$compatVirtualCraftingEnabled = false;

    @Shadow
    public abstract IGrid getGrid();

    @Unique
    private void eap$compatOnUpgradesChanged() {
        try {
            this.eap$compatNotifyHostChanged();
            eap$compatSyncVirtualCraftingState();
            if (UpgradeSlotCompat.shouldEnableChannelCard()) {
                eap$compatLastChannel = -1;
                eap$compatLastOwner = null;
                eap$compatHasInitialized = false;
                eap$compatInitializeChannelLink();
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性升级变更处理失败", e);
        }
    }

    @Unique
    private void eap$compatSyncVirtualCraftingState() {
        boolean hasCard = false;
        var inventory = eap$compatGetEffectiveUpgradeInventory();
        if (inventory != null) {
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty() && stack.getItem() == ModItems.VIRTUAL_CRAFTING_CARD.get()) {
                    hasCard = true;
                    break;
                }
            }
        }
        eap$compatVirtualCraftingEnabled = hasCard;
    }

    @Unique
    private void eap$compatTryVirtualCompletion(IPatternDetails patternDetails) {
        if (!eap$compatVirtualCraftingEnabled) {
            return;
        }

        var be = this.host.getBlockEntity();
        if (be == null || be.getLevel() == null || be.getLevel().isClientSide) {
            return;
        }

        var grid = getGrid();
        if (grid == null) {
            return;
        }

        var craftingService = grid.getCraftingService();
        if (craftingService == null) {
            return;
        }

        for (ICraftingCPU cpu : craftingService.getCpus()) {
            if (!cpu.isBusy()) {
                continue;
            }
            if (cpu instanceof CraftingCPUCluster cluster) {
                if (cluster.craftingLogic instanceof CraftingCpuLogicAccessor logicAccessor) {
                    var job = logicAccessor.extendedae_plus$getJob();
                    if (job instanceof ExecutingCraftingJobAccessor accessor) {
                        var tasks = accessor.extendedae_plus$getTasks();
                        var progress = tasks.get(patternDetails);
                        if (progress != null && progress.extendedae_plus$getValue() <= 1) {
                            cluster.updateOutput(null);
                            try {
                                logicAccessor.extendedae_plus$invokeFinishJob(true);
                            } catch (Throwable ignored) {
                                cluster.cancel();
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
    
    @Unique
    private void eap$compatOnExternalUpgradesChanged() {
        try {
            eap$compatSyncVirtualCraftingState();
            if (UpgradeSlotCompat.shouldEnableChannelCard()) {
                eap$compatLastChannel = -1;
                eap$compatLastOwner = null;
                eap$compatHasInitialized = false;
                eap$compatInitializeChannelLink();
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("监听appflux升级变化失败", e);
        }
    }

    // 兼容较新的 appflux 升级变化回调命名
    @Inject(method = "af_onUpgradesChanged", at = @At("TAIL"), remap = false, require = 0)
    private void eap$onAppfluxUpgradesChanged(CallbackInfo ci) {
        eap$compatOnExternalUpgradesChanged();
    }

    // 兼容旧命名，避免不同 appflux 版本导致注入失效
    @Inject(method = "af_$onUpgradesChanged", at = @At("TAIL"), remap = false, require = 0)
    private void eap$onLegacyAppfluxUpgradesChanged(CallbackInfo ci) {
        eap$compatOnExternalUpgradesChanged();
    }

    @Inject(method = "<init>(Lappeng/api/networking/IManagedGridNode;Lappeng/helpers/iface/PatternProviderLogicHost;)V",
            at = @At("TAIL"))
    private void eap$compatInitUpgrades(IManagedGridNode mainNode, PatternProviderLogicHost host, CallbackInfo ci) {
        try {

            boolean upgradeSlots = UpgradeSlotCompat.shouldManageLocalUpgradeInventory();
            boolean channelCard = UpgradeSlotCompat.shouldEnableChannelCard();

            if (upgradeSlots) {
                this.eap$compatUpgrades = UpgradeInventories.forMachine(
                        host.getTerminalIcon().getItem(),
                        UpgradeSlotCompat.getPatternProviderLocalUpgradeSlots(),
                        this::eap$compatOnUpgradesChanged
                );
            } else if (!channelCard) {
                this.eap$compatUpgrades = UpgradeInventories.empty();
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性升级初始化失败", e);
        }
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void eap$compatSaveUpgrades(CompoundTag tag, CallbackInfo ci) {
        try {
            if (UpgradeSlotCompat.shouldManageLocalUpgradeInventory()) {
                this.eap$compatUpgrades.writeToNBT(tag, "compat_upgrades");
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性升级保存失败", e);
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void eap$compatLoadUpgrades(CompoundTag tag, CallbackInfo ci) {
        try {
            if (UpgradeSlotCompat.shouldManageLocalUpgradeInventory()) {
                this.eap$compatUpgrades.readFromNBT(tag, "compat_upgrades");
            }

            if (UpgradeSlotCompat.shouldEnableChannelCard()) {
                eap$compatLastChannel = -1;
                eap$compatLastOwner = null;
                eap$compatHasInitialized = false;
                eap$compatInitializeChannelLink();
            }
            eap$compatSyncVirtualCraftingState();
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性升级加载失败", e);
        }
    }

    @Inject(method = "addDrops", at = @At("TAIL"))
    private void eap$compatDropUpgrades(List<ItemStack> drops, CallbackInfo ci) {
        try {
            if (UpgradeSlotCompat.shouldManageLocalUpgradeInventory()) {
                for (var stack : this.eap$compatUpgrades) {
                    if (!stack.isEmpty()) {
                        drops.add(stack);
                    }
                }
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性升级掉落失败", e);
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void eap$compatClearUpgrades(CallbackInfo ci) {
        try {
            if (UpgradeSlotCompat.shouldManageLocalUpgradeInventory()) {
                for (int i = 0; i < this.eap$compatUpgrades.size(); i++) {
                    this.eap$compatUpgrades.setItemDirect(i, net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
            if (UpgradeSlotCompat.shouldEnableChannelCard()) {
                eap$compatVirtualCraftingEnabled = false;
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性升级清理失败", e);
        }
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        if (UpgradeSlotCompat.shouldManageLocalUpgradeInventory()) {
            return this.eap$compatUpgrades != null ? this.eap$compatUpgrades : UpgradeInventories.empty();
        } else {
            return eap$compatGetEffectiveUpgradeInventory();
        }
    }

    @Override
    public boolean eap$compatIsVirtualCraftingEnabled() {
        return this.eap$compatVirtualCraftingEnabled;
    }

    @Override
    public IGrid eap$compatGetGrid() {
        return this.getGrid();
    }

    @Override
    public IManagedGridNode eap$compatGetMainNode() {
        return this.mainNode;
    }

    @Inject(method = "pushPattern", at = @At("HEAD"))
    private void eap$compatOnPushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, CallbackInfoReturnable<Boolean> cir) {
        eap$compatTryVirtualCompletion(patternDetails);
    }

    @Override
    public void eap$updateWirelessLink() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return;
        }
        
        try {
            if (eap$compatLink != null) {
                boolean wasConnected = eap$compatLink.isConnected();
                eap$compatLink.updateStatus();
                if (wasConnected != eap$compatLink.isConnected()) {
                    this.eap$compatNotifyHostChanged();
                }
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性无线链接更新失败", e);
        }
    }

    @Unique
    private void eap$compatInitializeChannelLink() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return;
        }
        
        try {
            // 客户端早退
            if (host.getBlockEntity() != null && host.getBlockEntity().getLevel() != null && host.getBlockEntity().getLevel().isClientSide) {
                return;
            }

            // 避免重复初始化
            if (eap$compatHasInitialized) {
                return;
            }

            // 等待网格完成引导
            if (!mainNode.hasGridBooted()) {
                eap$compatDelayedInitTicks = Math.max(eap$compatDelayedInitTicks, 5);
                try {
                    mainNode.ifPresent((grid, node) -> {
                        try { grid.getTickManager().wakeDevice(node); } catch (Throwable ignored) {}
                    });
                } catch (Throwable ignored) {}
                return;
            }

            long channel = 0L;
            boolean found = false;
            UUID ownerUUID = null;
            
            // 获取升级槽 - 如果装了appflux则从appflux获取，否则从我们自己的获取
            IUpgradeInventory upgrades = eap$compatGetEffectiveUpgradeInventory();

            var boundChannel = ChannelCardLinkHelper.findBoundChannel(upgrades, this::eap$getFallbackOwner);
            if (boundChannel != null) {
                channel = boundChannel.channel();
                ownerUUID = boundChannel.owner();
                found = true;
            }

            if (!found) {
                // 无频道卡：断开并视为初始化完成
                ChannelCardLinkHelper.disconnect(eap$compatLink);
                eap$compatHasInitialized = true;
                eap$compatLastChannel = 0L;
                eap$compatLastOwner = null;
                this.eap$compatNotifyHostChanged();
                return;
            }

            if (eap$compatLink != null
                    && ChannelCardLinkHelper.sameTarget(eap$compatLastChannel, eap$compatLastOwner, boundChannel)) {
                if (eap$compatLink.isConnected()) {
                    eap$compatHasInitialized = true;
                }
                return;
            }

            if (eap$compatLink == null) {
                var endpoint = new GenericNodeEndpointImpl(() -> host.getBlockEntity(), () -> this.mainNode.getNode());
                eap$compatLink = new WirelessSlaveLink(endpoint);
            }

            eap$compatLink.setPlacerId(ownerUUID);
            eap$compatLink.setFrequency(channel);
            eap$compatLink.updateStatus();
            eap$compatLastChannel = channel;
            eap$compatLastOwner = ownerUUID;
            this.eap$compatNotifyHostChanged();

            if (eap$compatLink.isConnected()) {
                eap$compatHasInitialized = true;
            } else {
                eap$compatHasInitialized = false;
                eap$compatDelayedInitTicks = Math.max(eap$compatDelayedInitTicks, 5);
                try {
                    mainNode.ifPresent((grid, node) -> {
                        try { grid.getTickManager().wakeDevice(node); } catch (Throwable ignored) {}
                    });
                } catch (Throwable ignored) {}
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性频道链接初始化失败", e);
        }
    }

    @Unique
    private IUpgradeInventory eap$compatGetEffectiveUpgradeInventory() {
        if (UpgradeSlotCompat.shouldManageLocalUpgradeInventory()) {
            return this.eap$compatUpgrades;
        }

        if (!UpgradeSlotCompat.shouldListenToAppfluxUpgrades()) {
            return null;
        }

        IUpgradeInventory inventory = UpgradeSlotCompat.getPatternProviderAppfluxUpgrades(this);
        if (inventory != null) {
            return inventory;
        }

        return UpgradeInventories.empty();
    }

    @Override
    public boolean eap$shouldKeepTicking() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return false;
        }

        try {
            if (host.getBlockEntity() == null || host.getBlockEntity().getLevel() == null
                    || host.getBlockEntity().getLevel().isClientSide) {
                return false;
            }
            return ChannelCardLinkHelper.shouldKeepTicking(
                    eap$compatGetEffectiveUpgradeInventory(),
                    eap$compatLink,
                    eap$compatHasInitialized);
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public void eap$setClientWirelessState(boolean connected) {
        if (UpgradeSlotCompat.shouldEnableChannelCard()) {
            eap$compatClientConnected = connected;
        }
    }

    @Override
    public boolean eap$isWirelessConnected() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return false;
        }
        
        try {
            if (host.getBlockEntity() != null && host.getBlockEntity().getLevel() != null && host.getBlockEntity().getLevel().isClientSide) {
                return eap$compatClientConnected;
            } else {
                return eap$compatLink != null && eap$compatLink.isConnected();
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("检查兼容性无线连接状态失败", e);
            return false;
        }
    }

    @Override
    public boolean eap$hasTickInitialized() {
        if (UpgradeSlotCompat.shouldEnableChannelCard()) {
            return eap$compatHasInitialized;
        }
        return true;
    }

    @Override
    public void eap$setTickInitialized(boolean initialized) {
        if (UpgradeSlotCompat.shouldEnableChannelCard()) {
            eap$compatHasInitialized = initialized;
        }
    }

    @Override
    public void eap$handleDelayedInit() {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return;
        }
        
        try {
            // 仅服务端
            if (host.getBlockEntity() != null && host.getBlockEntity().getLevel() != null && host.getBlockEntity().getLevel().isClientSide) {
                return;
            }
            if (!eap$compatHasInitialized) {
                if (!mainNode.hasGridBooted()) {
                    if (eap$compatDelayedInitTicks > 0) {
                        eap$compatDelayedInitTicks--;
                    }
                    if (eap$compatDelayedInitTicks == 0) {
                        eap$compatDelayedInitTicks = 5;
                        try {
                            mainNode.ifPresent((grid, node) -> {
                                try { grid.getTickManager().wakeDevice(node); } catch (Throwable ignored) {}
                            });
                        } catch (Throwable ignored) {}
                    }
                } else {
                    eap$compatInitializeChannelLink();
                    eap$compatSyncVirtualCraftingState();
                }
            }
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性延迟初始化失败", e);
        }
    }

    @Inject(method = "onMainNodeStateChanged", at = @At("TAIL"))
    private void eap$compatOnMainNodeStateChangedTail(CallbackInfo ci) {
        if (!UpgradeSlotCompat.shouldEnableChannelCard()) {
            return;
        }
        
        try {
            eap$compatLastChannel = -1;
            eap$compatLastOwner = null;
            eap$compatHasInitialized = false;
            eap$compatDelayedInitTicks = 10;
            try {
                mainNode.ifPresent((grid, node) -> {
                    try { grid.getTickManager().wakeDevice(node); } catch (Throwable ignored) {}
                });
            } catch (Throwable ignored) {}
        } catch (Exception e) {
            Logger.EAP$LOGGER.error("兼容性主节点状态变更处理失败", e);
        }
    }

    @Unique
    private UUID eap$getFallbackOwner() {
        if (this.mainNode != null && this.mainNode.getNode() != null) {
            var node = this.mainNode.getNode();
            return appeng.api.features.IPlayerRegistry.getMapping(node.getLevel()).getProfileId(node.getOwningPlayerId());
        }
        return null;
    }

    @Unique
    private void eap$compatNotifyHostChanged() {
        try {
            this.host.saveChanges();
        } catch (Throwable ignored) {
        }

        try {
            if (this.host.getBlockEntity() instanceof AEBaseBlockEntity blockEntity) {
                blockEntity.markForUpdate();
            }
        } catch (Throwable ignored) {
        }
    }
}
