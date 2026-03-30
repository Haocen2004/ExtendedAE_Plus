package com.extendedae_plus.network.provider;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.menu.implementations.PatternProviderMenu;
import com.extendedae_plus.api.advancedBlocking.IAdvancedBlocking;
import com.extendedae_plus.mixin.ae2.accessor.PatternProviderMenuAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: Toggle advanced blocking mode.
 * No extra payload; toggles based on the player's currently open PatternProviderMenu.
 */
public class ToggleAdvancedBlockingC2SPacket {
    public ToggleAdvancedBlockingC2SPacket() {}

    public static void encode(ToggleAdvancedBlockingC2SPacket msg, FriendlyByteBuf buf) {}

    public static ToggleAdvancedBlockingC2SPacket decode(FriendlyByteBuf buf) {
        return new ToggleAdvancedBlockingC2SPacket();
    }

    public static void handle(ToggleAdvancedBlockingC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            var containerMenu = player.containerMenu;
            if (containerMenu instanceof PatternProviderMenu menu) {
                var accessor = (PatternProviderMenuAccessor) menu;
                var logic = accessor.eap$logic();
                if (logic instanceof IAdvancedBlocking holder) {
                    boolean current = holder.eap$getAdvancedBlocking();
                    boolean next = !current;
                    holder.eap$setAdvancedBlocking(next);
                    logic.getConfigManager().putSetting(Settings.BLOCKING_MODE, YesNo.YES);
                    logic.saveChanges();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
