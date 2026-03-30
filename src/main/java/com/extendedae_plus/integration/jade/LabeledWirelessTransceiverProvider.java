package com.extendedae_plus.integration.jade;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import com.extendedae_plus.content.wireless.LabeledWirelessTransceiverBlockEntity;
import com.extendedae_plus.util.wireless.WirelessTeamUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 标签无线收发器：服务端数据同步（无主从）。
 */
public enum LabeledWirelessTransceiverProvider implements IServerDataProvider<BlockEntity> {
    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation("extendedae_plus", "labeled_wireless_info");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, ServerPlayer serverPlayer, Level level, BlockEntity blockEntity, boolean showDetails) {
        if (!(blockEntity instanceof LabeledWirelessTransceiverBlockEntity be)) return;
        String label = be.getLabelForDisplay();
        if (label != null) {
            data.putString("label", label);
        }
        // 所有者
        var placerId = be.getPlacerId();
        if (placerId != null && be.getServerLevel() != null) {
            data.putUUID("placerId", placerId);
            data.putString("ownerName", WirelessTeamUtil.getNetworkOwnerName(be.getServerLevel(), placerId).getString());
        }

        IGridNode node = be.getGridNode();
        IGrid grid = node == null ? null : node.getGrid();
        boolean networkUsable = false;
        int usedChannels = 0;
        int maxChannels = 0;
        if (node != null && node.isActive()) {
            for (var connection : node.getConnections()) {
                usedChannels = Math.max(connection.getUsedChannels(), usedChannels);
            }
            if (node instanceof appeng.me.GridNode gridNode) {
                var channelMode = gridNode.getGrid().getPathingService().getChannelMode();
                if (channelMode == appeng.api.networking.pathing.ChannelMode.INFINITE) {
                    maxChannels = -1;
                } else {
                    maxChannels = gridNode.getMaxChannels();
                }
            }
        }
        if (grid != null) {
            try {
                networkUsable = grid.getEnergyService().isNetworkPowered();
            } catch (Throwable ignored) {
                networkUsable = false;
            }
        }
        data.putInt("usedChannels", usedChannels);
        data.putInt("maxChannels", maxChannels);
        data.putBoolean("networkUsable", networkUsable);
    }
}
