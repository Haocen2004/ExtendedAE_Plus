package com.extendedae_plus.content.quantum.cluster;

import com.extendedae_plus.content.quantum.QuantumCraftingUnitType;
import com.extendedae_plus.content.quantum.entity.QuantumCraftingBlockEntity;
import com.extendedae_plus.config.ModConfig;

import appeng.api.networking.IGrid;
import appeng.api.networking.events.GridCraftingCpuChange;
import appeng.me.cluster.IAEMultiBlock;
import appeng.me.cluster.MBCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Iterator;

/**
 * Multiblock calculator for the Quantum Computer.
 * Validates structure, creates cluster on success.
 * Backported from AdvancedAE's AdvCraftingCPUCalculator.
 */
public class QuantumCPUCalculator extends MBCalculator<QuantumCraftingBlockEntity, QuantumCPUCluster> {

    public QuantumCPUCalculator(QuantumCraftingBlockEntity t) {
        super(t);
    }

    @Override
    public boolean checkMultiblockScale(BlockPos min, BlockPos max) {
        var maxSize = ModConfig.getQuantumComputerMaxSize() - 1;
        if (max.getX() - min.getX() > maxSize) return false;
        if (max.getY() - min.getY() > maxSize) return false;
        return max.getZ() - min.getZ() <= maxSize;
    }

    @Override
    public QuantumCPUCluster createCluster(ServerLevel level, BlockPos min, BlockPos max) {
        return new QuantumCPUCluster(min, max);
    }

    @Override
    public boolean verifyInternalStructure(ServerLevel level, BlockPos min, BlockPos max) {
        boolean core = false;
        boolean storage = false;
        int entangler = 0;
        int entanglerLimit = ModConfig.getQuantumMaxDataEntanglers();
        int multi = 0;
        int multiLimit = ModConfig.getQuantumMaxMultiThreaders();

        for (BlockPos blockPos : BlockPos.betweenClosed(min, max)) {
            final IAEMultiBlock<?> te = (IAEMultiBlock<?>) level.getBlockEntity(blockPos);

            if (te == null || !te.isValid()) {
                return false;
            }

            if (te instanceof QuantumCraftingBlockEntity advEntity) {
                boolean isBoundary = blockPos.getX() == min.getX()
                        || blockPos.getY() == min.getY()
                        || blockPos.getZ() == min.getZ()
                        || blockPos.getX() == max.getX()
                        || blockPos.getY() == max.getY()
                        || blockPos.getZ() == max.getZ();

                switch (advEntity.getUnitBlock().type) {
                    case QUANTUM_CORE -> {
                        if (min.equals(max)) {
                            return true; // Single-block Quantum Core is valid
                        }
                        if (!isBoundary && !core) {
                            core = true;
                        } else {
                            return false;
                        }
                    }
                    case STRUCTURE -> {
                        if (!isBoundary) {
                            return false; // Structure blocks only on boundary
                        }
                    }
                    case DATA_ENTANGLER -> {
                        if (!isBoundary && entangler < entanglerLimit) {
                            entangler++;
                        } else {
                            return false;
                        }
                    }
                    case MULTI_THREADER -> {
                        if (!isBoundary && multi < multiLimit) {
                            multi++;
                        } else {
                            return false;
                        }
                    }
                    default -> {
                        if (isBoundary) {
                            return false; // Other blocks must be internal
                        }
                    }
                }

                if (!storage) {
                    storage = advEntity.getStorageBytes() > 0;
                }
            } else {
                return false;
            }
        }
        return storage && core;
    }

    @Override
    public void updateBlockEntities(QuantumCPUCluster c, ServerLevel level, BlockPos min, BlockPos max) {
        for (BlockPos blockPos : BlockPos.betweenClosed(min, max)) {
            final QuantumCraftingBlockEntity te = (QuantumCraftingBlockEntity) level.getBlockEntity(blockPos);
            te.updateStatus(c);
            c.addBlockEntity(te);
        }

        c.done();

        final Iterator<QuantumCraftingBlockEntity> i = c.getBlockEntities();
        while (i.hasNext()) {
            var gh = i.next();
            var n = gh.getGridNode();
            if (n != null) {
                final IGrid g = n.getGrid();
                g.postEvent(new GridCraftingCpuChange(n));
                return;
            }
        }
    }

    @Override
    public boolean isValidBlockEntity(BlockEntity te) {
        return te instanceof QuantumCraftingBlockEntity;
    }
}
