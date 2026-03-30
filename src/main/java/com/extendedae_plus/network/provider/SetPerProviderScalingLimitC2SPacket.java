package com.extendedae_plus.network.provider;

import appeng.menu.implementations.PatternProviderMenu;
import com.extendedae_plus.api.smartDoubling.ISmartDoublingHolder;
import com.extendedae_plus.mixin.ae2.accessor.PatternProviderMenuAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: set per-provider scaling limit for the currently opened provider
 */
public class SetPerProviderScalingLimitC2SPacket {
    private final int limit;

    public SetPerProviderScalingLimitC2SPacket(int limit) {
        this.limit = limit;
    }

    public static void encode(SetPerProviderScalingLimitC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.limit);
    }

    public static SetPerProviderScalingLimitC2SPacket decode(FriendlyByteBuf buf) {
        return new SetPerProviderScalingLimitC2SPacket(buf.readInt());
    }

    public static void handle(SetPerProviderScalingLimitC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            try {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                var containerMenu = player.containerMenu;
                if (containerMenu instanceof PatternProviderMenu menu) {
                    var accessor = (PatternProviderMenuAccessor) menu;
                    var logic = accessor.eap$logic();
                    if (logic instanceof ISmartDoublingHolder handler) {
                        handler.eap$setProviderSmartDoublingLimit(msg.limit);
                        logic.saveChanges();
                    }
                }
            } catch (Throwable ignored) {
            }
        });
        ctx.setPacketHandled(true);
    }
}
