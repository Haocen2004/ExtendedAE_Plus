package com.extendedae_plus.content.matrix.menu;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixBaseEntity;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixCrafterEntity;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixPatternEntity;
import com.extendedae_plus.content.matrix.network.SAssemblerMatrixUpdatePacket;
import com.extendedae_plus.init.ModMenuTypes;
import com.extendedae_plus.init.ModNetwork;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AssemblerMatrixMenu extends AEBaseMenu {

    private final List<PatternSlotTracker> trackers = new ArrayList<>();
    private final Long2ReferenceMap<PatternSlotTracker> trackerMap = new Long2ReferenceOpenHashMap<>();
    private final AssemblerMatrixBaseEntity host;
    private int runningThreads = 0;

    // Client-side tracking
    private int clientRunningThreads = 0;

    public AssemblerMatrixMenu(int id, Inventory playerInventory, AssemblerMatrixBaseEntity host) {
        super(ModMenuTypes.ASSEMBLER_MATRIX.get(), id, playerInventory, host);
        this.host = host;
        this.setupPatternInventory();
        this.createPlayerInventorySlots(playerInventory);
    }

    public void cancelAllCrafting() {
        var cluster = this.host.getCluster();
        if (cluster != null && !cluster.isDestroyed()) {
            cluster.getBlockEntities().forEachRemaining(te -> {
                if (te instanceof AssemblerMatrixCrafterEntity crafter) {
                    crafter.stop();
                }
            });
        }
    }

    private int runningThreads() {
        var c = this.host.getCluster();
        if (c == null) {
            return 0;
        }
        return c.getBusyCrafterAmount();
    }

    @Override
    protected ItemStack transferStackToMenu(ItemStack input) {
        if (isDuplicatePattern(input)) {
            return input;
        }
        var slot = this.getAvailableSlot();
        if (slot != null) {
            return slot.addItems(input);
        }
        return input;
    }

    private boolean isDuplicatePattern(ItemStack input) {
        var level = this.getHost().getLevel();
        var inputDetails = PatternDetailsHelper.decodePattern(input, level);
        if (inputDetails == null) {
            return false;
        }
        for (long id : this.getSortedInfo()) {
            var tr = this.trackerMap.get(id);
            for (int x = 0; x < tr.server.size(); x++) {
                var existing = tr.server.getStackInSlot(x);
                if (existing.isEmpty()) continue;
                var existingDetails = PatternDetailsHelper.decodePattern(existing, level);
                if (existingDetails != null && patternsAreEquivalent(inputDetails, existingDetails)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean patternsAreEquivalent(IPatternDetails a, IPatternDetails b) {
        var outA = a.getOutputs();
        var outB = b.getOutputs();
        if (outA.length != outB.length) return false;
        for (int i = 0; i < outA.length; i++) {
            if (outA[i].amount() != outB[i].amount()) return false;
            if (!outA[i].what().equals(outB[i].what())) return false;
        }
        var inA = a.getInputs();
        var inB = b.getInputs();
        if (inA.length != inB.length) return false;
        for (int i = 0; i < inA.length; i++) {
            var possA = inA[i].getPossibleInputs();
            var possB = inB[i].getPossibleInputs();
            if (possA.length != possB.length) return false;
            for (int j = 0; j < possA.length; j++) {
                if (possA[j].amount() != possB[j].amount()) return false;
                if (!possA[j].what().equals(possB[j].what())) return false;
            }
        }
        return true;
    }

    @Nullable
    private appeng.api.inventories.InternalInventory getAvailableSlot() {
        for (long id : this.getSortedInfo()) {
            var tr = this.trackerMap.get(id);
            for (int x = 0; x < tr.server.size(); x++) {
                if (tr.server.getStackInSlot(x).isEmpty()) {
                    return new FilteredInternalInventory(tr.server.getSlotInv(x), new AssemblerMatrixPatternEntity.Filter(() -> this.getHost().getLevel()));
                }
            }
        }
        return null;
    }

    private long[] getSortedInfo() {
        return this.trackerMap.keySet().longStream().sorted().toArray();
    }

    @Override
    public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        var inv = this.trackerMap.get(id);
        if (inv == null) {
            return;
        }
        if (slot < 0 || slot >= inv.server.size()) {
            return;
        }

        final ItemStack is = inv.server.getStackInSlot(slot);
        var patternSlot = new FilteredInternalInventory(inv.server.getSlotInv(slot), new AssemblerMatrixPatternEntity.Filter(() -> this.getHost().getLevel()));
        var carried = getCarried();

        switch (action) {
            case PICKUP_OR_SET_DOWN -> {
                if (!carried.isEmpty()) {
                    ItemStack inSlot = patternSlot.getStackInSlot(0);
                    if (inSlot.isEmpty()) {
                        setCarried(patternSlot.addItems(carried));
                    } else {
                        inSlot = inSlot.copy();
                        final ItemStack inHand = carried.copy();
                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                        setCarried(ItemStack.EMPTY);
                        setCarried(patternSlot.addItems(inHand.copy()));
                        if (getCarried().isEmpty()) {
                            setCarried(inSlot);
                        } else {
                            setCarried(inHand);
                            patternSlot.setItemDirect(0, inSlot);
                        }
                    }
                } else {
                    setCarried(patternSlot.getStackInSlot(0));
                    patternSlot.setItemDirect(0, ItemStack.EMPTY);
                }
            }
            case SPLIT_OR_PLACE_SINGLE -> {
                if (!carried.isEmpty()) {
                    ItemStack extra = carried.split(1);
                    if (!extra.isEmpty()) {
                        extra = patternSlot.addItems(extra);
                    }
                    if (!extra.isEmpty()) {
                        carried.grow(extra.getCount());
                    }
                } else if (!is.isEmpty()) {
                    setCarried(patternSlot.extractItem(0, (is.getCount() + 1) / 2, false));
                }
            }
            case SHIFT_CLICK -> {
                var stack = patternSlot.getStackInSlot(0).copy();
                if (!player.getInventory().add(stack)) {
                    patternSlot.setItemDirect(0, stack);
                } else {
                    patternSlot.setItemDirect(0, ItemStack.EMPTY);
                }
            }
            case MOVE_REGION -> {
                for (int x = 0; x < inv.server.size(); x++) {
                    var moveSlot = new FilteredInternalInventory(inv.server.getSlotInv(x), new AssemblerMatrixPatternEntity.Filter(() -> this.getHost().getLevel()));
                    var stack = moveSlot.getStackInSlot(0).copy();
                    if (!stack.isEmpty()) {
                        if (player.getInventory().add(stack)) {
                            moveSlot.setItemDirect(0, ItemStack.EMPTY);
                        }
                    }
                }
            }
            case CREATIVE_DUPLICATE -> {
                if (player.getAbilities().instabuild && carried.isEmpty()) {
                    setCarried(is.isEmpty() ? ItemStack.EMPTY : is.copy());
                }
            }
        }
    }

    @Override
    public void broadcastChanges() {
        if (isClientSide()) {
            return;
        }
        super.broadcastChanges();
        if (this.getPlayer() instanceof ServerPlayer player) {
            for (var tracker : this.trackers) {
                if (tracker.init) {
                    var ptk = tracker.createPacket();
                    if (ptk != null) {
                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), ptk);
                    }
                } else {
                    tracker.init = true;
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), tracker.fullPacket());
                }
            }
            int newRunningThreads = this.runningThreads();
            if (this.runningThreads != newRunningThreads) {
                this.runningThreads = newRunningThreads;
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SAssemblerMatrixUpdatePacket(0L, new Int2ObjectOpenHashMap<>(), newRunningThreads));
            }
        }
    }

    private void setupPatternInventory() {
        if (isClientSide()) {
            return;
        }
        this.trackers.clear();
        this.trackerMap.clear();
        var cluster = this.host.getCluster();
        if (cluster != null && !cluster.isDestroyed()) {
            for (var pattern : cluster.getPatterns()) {
                var tracker = new PatternSlotTracker(pattern);
                this.trackers.add(tracker);
                this.trackerMap.put(pattern.getLocateID(), tracker);
            }
        }
    }

    public AssemblerMatrixBaseEntity getHost() {
        return this.host;
    }

    @OnlyIn(Dist.CLIENT)
    public int getClientRunningThreads() {
        return this.clientRunningThreads;
    }

    @OnlyIn(Dist.CLIENT)
    public void setClientRunningThreads(int threads) {
        this.clientRunningThreads = threads;
    }

    private static class PatternSlotTracker {

        private final AssemblerMatrixPatternEntity invHost;
        private final appeng.api.inventories.InternalInventory client;
        private final appeng.api.inventories.InternalInventory server;
        private final Int2ObjectMap<ItemStack> changedMap = new Int2ObjectOpenHashMap<>();
        private boolean init = false;

        public PatternSlotTracker(AssemblerMatrixPatternEntity host) {
            this.invHost = host;
            this.client = new AppEngInternalInventory(AssemblerMatrixPatternEntity.INV_SIZE);
            this.server = host.getPatternInventory();
        }

        private Int2ObjectMap<ItemStack> getChangedMap() {
            this.changedMap.clear();
            for (int x = 0; x < server.size(); x++) {
                var ss = server.getStackInSlot(x);
                var cs = client.getStackInSlot(x);
                if (!ItemStack.isSame(ss, cs) || !ItemStack.tagMatches(ss, cs)) {
                    this.changedMap.put(x, ss.copy());
                    client.setItemDirect(x, ss.copy());
                }
            }
            return this.changedMap;
        }

        private Int2ObjectMap<ItemStack> getFullMap() {
            this.changedMap.clear();
            for (int x = 0; x < server.size(); x++) {
                this.changedMap.put(x, server.getStackInSlot(x).copy());
            }
            return this.changedMap;
        }

        @Nullable
        public SAssemblerMatrixUpdatePacket createPacket() {
            var map = this.getChangedMap();
            if (map.isEmpty()) {
                return null;
            }
            return new SAssemblerMatrixUpdatePacket(invHost.getLocateID(), map, -1);
        }

        public SAssemblerMatrixUpdatePacket fullPacket() {
            return new SAssemblerMatrixUpdatePacket(invHost.getLocateID(), this.getFullMap(), -1);
        }
    }
}
