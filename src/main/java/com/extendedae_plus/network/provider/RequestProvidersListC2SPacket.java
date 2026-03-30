package com.extendedae_plus.network.provider;

import appeng.helpers.iface.PatternContainer;
import appeng.menu.implementations.PatternAccessTermMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.extendedae_plus.init.ModNetwork;
import com.extendedae_plus.util.PatternProviderDataUtil;
import com.extendedae_plus.util.PatternTerminalUtil;
import com.extendedae_plus.util.uploadPattern.ProviderUploadUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * C2S: 请求当前终端可见的样板供应器列表（用于弹窗选择）�?
 */
public class RequestProvidersListC2SPacket {
    public RequestProvidersListC2SPacket() {}

    public static void encode(RequestProvidersListC2SPacket msg, FriendlyByteBuf buf) {}

    public static RequestProvidersListC2SPacket decode(FriendlyByteBuf buf) { return new RequestProvidersListC2SPacket(); }

    public static void handle(RequestProvidersListC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Ctrl+Q pending 模式：不依赖编码终端，直接基于玩家网络给出列表（负数索引 ID�?
            if (ProviderUploadUtil.hasPendingCtrlQPattern(player)) {
                List<PatternContainer> containers = ProviderUploadUtil.listAvailableProvidersFromPlayerNetwork(player);
                List<Long> idxIds = new ArrayList<>();
                List<String> names = new ArrayList<>();
                List<Integer> slots = new ArrayList<>();
                for (int i = 0; i < containers.size(); i++) {
                    var c = containers.get(i);
                    if (c == null) continue;
                    int empty = PatternProviderDataUtil.getAvailableSlots(c);
                    if (empty <= 0) continue;
                    long encodedId = -1L - i;
                    idxIds.add(encodedId);
                    names.add(PatternProviderDataUtil.getProviderDisplayName(c));
                    slots.add(empty);
                }
                ModNetwork.CHANNEL.sendTo(new ProvidersListS2CPacket(idxIds, names, slots), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                return;
            }

            if (!(player.containerMenu instanceof PatternEncodingTermMenu encMenu)) return;

            // 优先：若玩家也打开了样板访问终端，则用 byId 方式（精确服务器ID�?
            PatternAccessTermMenu accessMenu = PatternTerminalUtil.getPatternAccessMenu(player);
            if (accessMenu != null) {
                List<Long> ids = PatternTerminalUtil.getAllProviderIds(accessMenu);
                List<Long> filteredIds = new ArrayList<>();
                List<String> names = new ArrayList<>();
                List<Integer> slots = new ArrayList<>();

                for (Long id : ids) {
                    if (id == null) continue;
                    if (!PatternProviderDataUtil.isProviderAvailable(id, accessMenu)) continue;
                    int empty = PatternProviderDataUtil.getAvailableSlots(id, accessMenu);
                    if (empty <= 0) continue; // 只列出有空位�?
                    filteredIds.add(id);
                    names.add(PatternProviderDataUtil.getProviderDisplayName(id, accessMenu));
                    slots.add(empty);
                }

                ModNetwork.CHANNEL.sendTo(new ProvidersListS2CPacket(filteredIds, names, slots), player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
                return;
            }

            // 回退：基于编码终端所在网络枚举供应器，用“负数ID编码索引”：encodedId = -1 - index
            List<PatternContainer> containers = PatternTerminalUtil.listAvailableProvidersFromGrid(encMenu);
            List<Long> idxIds = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < containers.size(); i++) {
                var c = containers.get(i);
                if (c == null) continue;
                int empty = PatternProviderDataUtil.getAvailableSlots(c);
                if (empty <= 0) continue;
                long encodedId = -1L - i; // 约定：负数代表按索引
                idxIds.add(encodedId);
                names.add(PatternProviderDataUtil.getProviderDisplayName(c));
                slots.add(empty);
            }
            ModNetwork.CHANNEL.sendTo(new ProvidersListS2CPacket(idxIds, names, slots), player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
        });
        ctx.setPacketHandled(true);
    }
}
