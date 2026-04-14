package com.extendedae_plus.content.matrix.network;

import com.extendedae_plus.content.matrix.gui.AssemblerMatrixScreen;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: Syncs pattern inventory updates and running thread count from server to client.
 */
public class SAssemblerMatrixUpdatePacket {

    private long patternID;
    private Int2ObjectMap<ItemStack> updateMap;
    private int runningThreads;

    public SAssemblerMatrixUpdatePacket(long id, Int2ObjectMap<ItemStack> updateMap, int runningThreads) {
        this.patternID = id;
        this.updateMap = new Int2ObjectOpenHashMap<>(updateMap);
        this.runningThreads = runningThreads;
    }

    public static void encode(SAssemblerMatrixUpdatePacket msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.patternID);
        buf.writeInt(msg.runningThreads);
        buf.writeInt(msg.updateMap.size());
        for (var entry : msg.updateMap.int2ObjectEntrySet()) {
            buf.writeInt(entry.getIntKey());
            buf.writeItem(entry.getValue());
        }
    }

    public static SAssemblerMatrixUpdatePacket decode(FriendlyByteBuf buf) {
        long patternID = buf.readLong();
        int runningThreads = buf.readInt();
        int size = buf.readInt();
        Int2ObjectMap<ItemStack> map = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readInt(), buf.readItem());
        }
        return new SAssemblerMatrixUpdatePacket(patternID, map, runningThreads);
    }

    public static void handle(SAssemblerMatrixUpdatePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SAssemblerMatrixUpdatePacket msg) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof AssemblerMatrixScreen screen) {
            if (!msg.updateMap.isEmpty()) {
                screen.receiveUpdate(msg.patternID, msg.updateMap);
            }
            if (msg.runningThreads >= 0) {
                screen.getMenu().setClientRunningThreads(msg.runningThreads);
            }
        }
    }
}
