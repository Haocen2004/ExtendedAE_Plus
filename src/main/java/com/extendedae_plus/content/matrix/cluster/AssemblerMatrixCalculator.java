package com.extendedae_plus.content.matrix.cluster;

import appeng.me.cluster.IAEMultiBlock;
import appeng.me.cluster.MBCalculator;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixBaseEntity;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixCrafterEntity;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixFrameEntity;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixFunctionEntity;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixPatternEntity;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixWallEntity;
import com.extendedae_plus.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AssemblerMatrixCalculator extends MBCalculator<AssemblerMatrixBaseEntity, AssemblerMatrixCluster> {

    private static final int MIN_SIZE = 2;

    private static int getMaxSize() {
        return ModConfig.INSTANCE != null ? ModConfig.INSTANCE.assemblerMatrixMaxSize : 6;
    }

    public AssemblerMatrixCalculator(AssemblerMatrixBaseEntity t) {
        super(t);
    }

    @Override
    public boolean checkMultiblockScale(BlockPos min, BlockPos max) {
        int maxSize = getMaxSize();
        if (max.getX() - min.getX() > maxSize) return false;
        if (max.getY() - min.getY() > maxSize) return false;
        if (max.getZ() - min.getZ() > maxSize) return false;
        if (max.getX() - min.getX() < MIN_SIZE) return false;
        if (max.getY() - min.getY() < MIN_SIZE) return false;
        return max.getZ() - min.getZ() >= MIN_SIZE;
    }

    @Override
    public AssemblerMatrixCluster createCluster(ServerLevel level, BlockPos min, BlockPos max) {
        return new AssemblerMatrixCluster(min, max);
    }

    @Override
    public boolean verifyInternalStructure(ServerLevel level, BlockPos min, BlockPos max) {
        boolean anyPattern = false;
        boolean anyCrafter = false;
        for (var pos : BlockPos.betweenClosed(min, max)) {
            var te = level.getBlockEntity(pos);
            if (!(te instanceof IAEMultiBlock<?> mb) || !mb.isValid()) {
                return false;
            }
            if (te instanceof AssemblerMatrixPatternEntity) anyPattern = true;
            if (te instanceof AssemblerMatrixCrafterEntity) anyCrafter = true;
            if (isInternal(pos, min, max)) {
                if (!(te instanceof AssemblerMatrixFunctionEntity)) return false;
            } else if (isEdge(pos, min, max)) {
                if (!(te instanceof AssemblerMatrixFrameEntity)) return false;
            } else {
                if (!(te instanceof AssemblerMatrixWallEntity)) return false;
            }
        }
        return anyCrafter && anyPattern;
    }

    @Override
    public void updateBlockEntities(AssemblerMatrixCluster c, ServerLevel level, BlockPos min, BlockPos max) {
        for (var pos : BlockPos.betweenClosed(min, max)) {
            var te = (AssemblerMatrixBaseEntity) level.getBlockEntity(pos);
            if (te != null) {
                te.updateStatus(c);
                c.addTileEntity(te);
            }
        }
        c.done();
    }

    @Override
    public boolean isValidBlockEntity(BlockEntity te) {
        return te instanceof AssemblerMatrixBaseEntity;
    }

    private boolean isInternal(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() < max.getX() && pos.getX() > min.getX() &&
                pos.getY() < max.getY() && pos.getY() > min.getY() &&
                pos.getZ() < max.getZ() && pos.getZ() > min.getZ();
    }

    private boolean isEdge(BlockPos pos, BlockPos min, BlockPos max) {
        return (min.getX() == pos.getX() && min.getY() == pos.getY()) ||
                (min.getX() == pos.getX() && min.getZ() == pos.getZ()) ||
                (min.getY() == pos.getY() && min.getZ() == pos.getZ()) ||
                (max.getX() == pos.getX() && max.getY() == pos.getY()) ||
                (max.getX() == pos.getX() && max.getZ() == pos.getZ()) ||
                (max.getY() == pos.getY() && max.getZ() == pos.getZ()) ||
                (min.getX() == pos.getX() && max.getY() == pos.getY()) ||
                (min.getX() == pos.getX() && max.getZ() == pos.getZ()) ||
                (min.getY() == pos.getY() && max.getX() == pos.getX()) ||
                (min.getY() == pos.getY() && max.getZ() == pos.getZ()) ||
                (min.getZ() == pos.getZ() && max.getX() == pos.getX()) ||
                (min.getZ() == pos.getZ() && max.getY() == pos.getY());
    }
}
