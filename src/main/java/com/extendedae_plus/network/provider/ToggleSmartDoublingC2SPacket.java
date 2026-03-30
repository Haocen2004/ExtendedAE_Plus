package com.extendedae_plus.network.provider;

import appeng.menu.implementations.PatternProviderMenu;
import com.extendedae_plus.api.smartDoubling.ISmartDoublingHolder;
import com.extendedae_plus.mixin.ae2.accessor.PatternProviderMenuAccessor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: Toggle smart doubling enabled state.
 * No extra payload; toggles based on the player's currently open PatternProviderMenu.
 */
public class ToggleSmartDoublingC2SPacket {
    public ToggleSmartDoublingC2SPacket() {}

    public static void encode(ToggleSmartDoublingC2SPacket msg, FriendlyByteBuf buf) {}

    public static ToggleSmartDoublingC2SPacket decode(FriendlyByteBuf buf) {
        return new ToggleSmartDoublingC2SPacket();
    }

    public static void handle(ToggleSmartDoublingC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            var containerMenu = player.containerMenu;
            if (containerMenu instanceof PatternProviderMenu menu) {
                var accessor = (PatternProviderMenuAccessor) menu;
                var logic = accessor.eap$logic();
                if (logic instanceof ISmartDoublingHolder holder) {
                    boolean current = holder.eap$getSmartDoubling();
                    boolean next = !current;
                    holder.eap$setSmartDoubling(next);
                    logic.saveChanges();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
