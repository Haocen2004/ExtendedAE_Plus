package com.extendedae_plus.content.matrix.network;

import com.extendedae_plus.content.matrix.menu.AssemblerMatrixMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: Client actions for the Assembler Matrix menu (cancel all crafting, etc.)
 */
public class CAssemblerMatrixActionPacket {

    public static final int ACTION_CANCEL = 0;

    private final int action;

    public CAssemblerMatrixActionPacket(int action) {
        this.action = action;
    }

    public static void encode(CAssemblerMatrixActionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
    }

    public static CAssemblerMatrixActionPacket decode(FriendlyByteBuf buf) {
        return new CAssemblerMatrixActionPacket(buf.readVarInt());
    }

    public static void handle(CAssemblerMatrixActionPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.containerMenu instanceof AssemblerMatrixMenu menu) {
                switch (msg.action) {
                    case ACTION_CANCEL -> menu.cancelAllCrafting();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
