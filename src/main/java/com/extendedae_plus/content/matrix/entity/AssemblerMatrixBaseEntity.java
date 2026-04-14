package com.extendedae_plus.content.matrix.entity;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridMultiblock;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.util.IConfigManager;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.cluster.IAEMultiBlock;
import appeng.util.ConfigManager;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.iterators.ChainedIterator;
import com.extendedae_plus.content.matrix.block.AssemblerMatrixBaseBlock;
import com.extendedae_plus.content.matrix.cluster.AssemblerMatrixCalculator;
import com.extendedae_plus.content.matrix.cluster.AssemblerMatrixCluster;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AssemblerMatrixBaseEntity extends AENetworkBlockEntity implements IAEMultiBlock<AssemblerMatrixCluster>, IPowerChannelState {

    protected final AssemblerMatrixCalculator calc = new AssemblerMatrixCalculator(this);
    protected final ConfigManager manager;
    protected boolean isCore = false;
    protected CompoundTag previousState = null;
    protected AssemblerMatrixCluster cluster;

    public AssemblerMatrixBaseEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.getMainNode().setFlags(GridFlags.MULTIBLOCK, GridFlags.REQUIRE_CHANNEL)
                .setExposedOnSides(EnumSet.noneOf(Direction.class))
                .addService(IGridMultiblock.class, this::getMultiblockNodes);
        this.getMainNode().setIdlePowerUsage(0.5);
        this.manager = new ConfigManager(this::saveChanges);
        this.manager.registerSetting(Settings.PATTERN_ACCESS_TERMINAL, YesNo.YES);
    }

    public IConfigManager getConfigManager() {
        return this.manager;
    }

    public CompoundTag getPreviousState() {
        return this.previousState;
    }

    public void setPreviousState(CompoundTag previousState) {
        this.previousState = previousState;
    }

    public boolean isCore() {
        return this.isCore;
    }

    public void setCore(boolean core) {
        this.isCore = core;
    }

    @Override
    protected Item getItemFromBlockEntity() {
        if (this.level == null) {
            return Items.AIR;
        }
        return getMatrixBlock().getPresentItem();
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        if (this.cluster != null) {
            this.cluster.updateName();
        }
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

    public void breakCluster() {
        if (this.cluster != null) {
            this.cluster.destroy();
        }
    }

    public boolean isFormed() {
        if (isClientSide()) {
            return getBlockState().getValue(AssemblerMatrixBaseBlock.FORMED);
        }
        return this.cluster != null;
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putBoolean("core", this.isCore);
        this.manager.writeToNBT(data);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        this.setCore(data.getBoolean("core"));
        this.manager.readFromNBT(data);
        if (this.isCore) {
            this.setPreviousState(data.copy());
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
    public AssemblerMatrixCluster getCluster() {
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

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        requestModelDataUpdate();
    }

    @Override
    public boolean isActive() {
        if (!isClientSide()) {
            return this.getMainNode().isActive();
        }
        return this.isPowered() && this.isFormed();
    }

    @Nullable
    public IItemHandler getPatternInv(Direction ignored) {
        if (this.cluster == null) {
            return null;
        }
        var inv = new ArrayList<InternalInventory>();
        for (var pc : this.cluster.getPatterns()) {
            inv.add(pc.getExposedInventory());
        }
        return new CombinedInternalInventory(inv.toArray(new InternalInventory[0])).toItemHandler();
    }

    public void updateStatus(AssemblerMatrixCluster c) {
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
        final boolean power = formed & this.getMainNode().isOnline();

        final BlockState current = this.level.getBlockState(this.worldPosition);

        if (current.getBlock() instanceof AssemblerMatrixBaseBlock<?>) {
            final BlockState newState = current
                    .setValue(AssemblerMatrixBaseBlock.POWERED, power)
                    .setValue(AssemblerMatrixBaseBlock.FORMED, formed);
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

    @Override
    public boolean isPowered() {
        if (isClientSide()) {
            return this.level.getBlockState(this.worldPosition).getValue(AssemblerMatrixBaseBlock.POWERED);
        }
        return this.getMainNode().isActive();
    }

    public AssemblerMatrixBaseBlock<?> getMatrixBlock() {
        if (this.level == null || this.notLoaded() || this.isRemoved()) {
            return (AssemblerMatrixBaseBlock<?>) ModBlocks.ASSEMBLER_MATRIX_FRAME.get();
        }
        return (AssemblerMatrixBaseBlock<?>) this.level.getBlockState(this.worldPosition).getBlock();
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
}
