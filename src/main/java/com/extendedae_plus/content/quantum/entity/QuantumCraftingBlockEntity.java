package com.extendedae_plus.content.quantum.entity;

import com.extendedae_plus.content.quantum.QuantumCraftingUnitType;
import com.extendedae_plus.content.quantum.block.QuantumCraftingUnitBlock;
import com.extendedae_plus.content.quantum.cluster.QuantumCPUCalculator;
import com.extendedae_plus.content.quantum.cluster.QuantumCPUCluster;
import com.extendedae_plus.init.ModBlocks;

import appeng.api.implementations.IPowerChannelState;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridMultiblock;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.util.IConfigManager;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.cluster.IAEMultiBlock;
import appeng.util.NullConfigManager;
import appeng.util.Platform;
import appeng.util.iterators.ChainedIterator;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Block entity for all Quantum Computer multiblock blocks.
 * Backported from AdvancedAE's AdvCraftingBlockEntity.
 */
public class QuantumCraftingBlockEntity extends AENetworkBlockEntity
        implements IAEMultiBlock<QuantumCPUCluster>, IPowerChannelState {

    private final QuantumCPUCalculator calc = new QuantumCPUCalculator(this);
    private CompoundTag previousState = null;
    private boolean isCoreBlock = false;
    private QuantumCPUCluster cluster;

    public QuantumCraftingBlockEntity(BlockPos pos, BlockState blockState) {
        super(com.extendedae_plus.init.ModBlockEntities.QUANTUM_CRAFTING_UNIT.get(), pos, blockState);
        this.getMainNode()
                .setFlags(GridFlags.MULTIBLOCK, GridFlags.REQUIRE_CHANNEL)
                .setExposedOnSides(EnumSet.noneOf(Direction.class))
                .addService(IGridMultiblock.class, this::getMultiblockNodes);
    }

    @Override
    protected Item getItemFromBlockEntity() {
        if (this.level == null) {
            return Items.AIR;
        }
        return getUnitBlock().type.getItemFromType();
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        if (this.cluster != null) {
            this.cluster.updateName();
        }
    }

    public QuantumCraftingUnitBlock getUnitBlock() {
        if (this.level == null || this.notLoaded() || this.isRemoved()) {
            return (QuantumCraftingUnitBlock) ModBlocks.QUANTUM_UNIT.get();
        }
        var block = this.level.getBlockState(this.worldPosition).getBlock();
        return block instanceof QuantumCraftingUnitBlock qb
                ? qb
                : (QuantumCraftingUnitBlock) ModBlocks.QUANTUM_UNIT.get();
    }

    public long getStorageBytes() {
        return getUnitBlock().type.getStorageBytes();
    }

    public int getStorageMultiplier() {
        return getUnitBlock().type.getStorageMultiplier();
    }

    public int getAcceleratorThreads() {
        return getUnitBlock().type.getAcceleratorThreads();
    }

    public int getAccelerationMultiplier() {
        return getUnitBlock().type.getAccelerationMultiplier();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.getMainNode().setVisualRepresentation(this.getItemFromBlockEntity());
        if (level instanceof ServerLevel serverLevel) {
            this.calc.calculateMultiblock(serverLevel, worldPosition);
        }
    }

    public void updateMultiBlock(BlockPos changedPos) {
        if (level instanceof ServerLevel serverLevel) {
            this.calc.updateMultiblockAfterNeighborUpdate(serverLevel, worldPosition, changedPos);
        }
    }

    public void updateStatus(QuantumCPUCluster c) {
        if (this.cluster != null && this.cluster != c) {
            this.cluster.breakCluster();
        }
        this.cluster = c;
        this.updateSubType(true);
    }

    public void updateSubType(boolean updateFormed) {
        if (this.level == null || this.notLoaded() || this.isRemoved()) {
            return;
        }

        final boolean formed = this.isFormed();
        boolean power = this.getMainNode().isOnline();

        final BlockState current = this.level.getBlockState(this.worldPosition);

        if (current.getBlock() instanceof QuantumCraftingUnitBlock) {
            final BlockState newState = current
                    .setValue(QuantumCraftingUnitBlock.POWERED, power)
                    .setValue(QuantumCraftingUnitBlock.FORMED, formed);

            if (current != newState) {
                this.level.setBlock(this.worldPosition, newState, Block.UPDATE_CLIENTS);
            }
        }

        if (updateFormed) {
            updateExposedSides();
        }
    }

    private void updateExposedSides() {
        if (isFormed()) {
            this.getMainNode().setExposedOnSides(EnumSet.allOf(Direction.class));
        } else {
            this.getMainNode().setExposedOnSides(EnumSet.noneOf(Direction.class));
        }
    }

    public boolean isFormed() {
        if (isClientSide()) {
            return getBlockState().getValue(QuantumCraftingUnitBlock.FORMED);
        }
        return this.cluster != null;
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putBoolean("core", this.isCoreBlock());
        if (this.isCoreBlock() && this.cluster != null) {
            this.cluster.writeToNBT(data);
        }
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        this.setCoreBlock(data.getBoolean("core"));
        if (this.isCoreBlock()) {
            if (this.cluster != null) {
                this.cluster.readFromNBT(data);
            } else {
                this.setPreviousState(data.copy());
            }
        }
    }

    @Override
    public void disconnect(boolean update) {
        if (this.cluster != null) {
            this.cluster.destroy();
            if (update) {
                this.updateSubType(true);
            }
        }
    }

    @Override
    public QuantumCPUCluster getCluster() {
        return this.cluster;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (reason != IGridNodeListener.State.GRID_BOOT) {
            this.updateSubType(false);
        }
    }

    public void breakCluster() {
        if (this.cluster != null) {
            this.cluster.cancelJobs();
            var inventories = this.cluster.getInventories();

            var places = new ArrayList<BlockPos>();
            for (var blockEntity : (Iterable<QuantumCraftingBlockEntity>) this.cluster::getBlockEntities) {
                if (this == blockEntity) {
                    places.add(worldPosition);
                } else {
                    for (var d : Direction.values()) {
                        var p = blockEntity.worldPosition.relative(d);
                        if (this.level.isEmptyBlock(p)) {
                            places.add(p);
                        }
                    }
                }
            }

            if (places.isEmpty()) {
                throw new IllegalStateException(
                        this.cluster + " does not contain any blocks which were destroyed.");
            }

            for (var inv : inventories) {
                for (var entry : inv.list) {
                    var position = Util.getRandom(places, level.getRandom());
                    var stacks = new ArrayList<ItemStack>();
                    entry.getKey().addDrops(entry.getLongValue(), stacks, this.level, position);
                    Platform.spawnDrops(this.level, position, stacks);
                }
                inv.clear();
            }

            this.cluster.destroy();
        }
    }

    @Override
    public boolean isPowered() {
        if (isClientSide()) {
            return this.level.getBlockState(this.worldPosition).getValue(QuantumCraftingUnitBlock.POWERED);
        }
        return this.getMainNode().isActive();
    }

    @Override
    public boolean isActive() {
        if (!isClientSide()) {
            return this.getMainNode().isActive();
        }
        return this.isPowered() && this.isFormed();
    }

    public boolean isCoreBlock() {
        return this.isCoreBlock;
    }

    public void setCoreBlock(boolean isCoreBlock) {
        this.isCoreBlock = isCoreBlock;
    }

    public CompoundTag getPreviousState() {
        return this.previousState;
    }

    public void setPreviousState(CompoundTag previousState) {
        this.previousState = previousState;
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        requestModelDataUpdate();
    }

    protected EnumSet<Direction> getConnections() {
        if (level == null) {
            return EnumSet.noneOf(Direction.class);
        }
        EnumSet<Direction> connections = EnumSet.noneOf(Direction.class);
        for (Direction facing : Direction.values()) {
            if (this.isConnected(level, worldPosition, facing)) {
                connections.add(facing);
            }
        }
        return connections;
    }

    private boolean isConnected(BlockGetter level, BlockPos pos, Direction side) {
        BlockPos adjacentPos = pos.relative(side);
        return level.getBlockState(adjacentPos).getBlock() instanceof QuantumCraftingUnitBlock;
    }

    private Iterator<IGridNode> getMultiblockNodes() {
        if (this.getCluster() == null) {
            return new ChainedIterator<>();
        }
        var nodes = new ArrayList<IGridNode>();
        var it = this.getCluster().getBlockEntities();
        while (it.hasNext()) {
            var node = it.next().getGridNode();
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes.iterator();
    }

    public IConfigManager getConfigManager() {
        var cluster = this.getCluster();
        if (cluster != null) {
            return this.getCluster().getConfigManager();
        } else {
            return NullConfigManager.INSTANCE;
        }
    }
}
