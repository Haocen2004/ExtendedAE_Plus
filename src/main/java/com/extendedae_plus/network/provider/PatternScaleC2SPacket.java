package com.extendedae_plus.network.provider;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client→Server packet for multiply/divide pattern amounts in PatternProviderMenu.
 */
public class PatternScaleC2SPacket {
    private final int factor;
    private final boolean divide;

    public PatternScaleC2SPacket(int factor, boolean divide) {
        this.factor = factor;
        this.divide = divide;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(factor);
        buf.writeBoolean(divide);
    }

    public static PatternScaleC2SPacket decode(FriendlyByteBuf buf) {
        return new PatternScaleC2SPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(PatternScaleC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            var menu = player.containerMenu;
            if (!(menu instanceof PatternProviderMenu ppMenu)) return;
            int scale = pkt.factor;
            if (scale != 2 && scale != 5 && scale != 10) return;
            modifyPatterns(ppMenu, scale, pkt.divide);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void modifyPatterns(PatternProviderMenu menu, int scale, boolean div) {
        for (var slot : menu.getSlots(SlotSemantics.ENCODED_PATTERN)) {
            var stack = slot.getItem();
            if (stack.getItem() instanceof EncodedPatternItem pattern) {
                var detail = pattern.decode(stack, menu.getPlayer().level, false);
                if (detail instanceof AEProcessingPattern process) {
                    var input = process.getSparseInputs();
                    var output = process.getOutputs();
                    if (checkModify(input, scale, div) && checkModify(output, scale, div)) {
                        var mulInput = new GenericStack[input.length];
                        var mulOutput = new GenericStack[output.length];
                        applyScale(input, mulInput, scale, div);
                        applyScale(output, mulOutput, scale, div);
                        var newPattern = PatternDetailsHelper.encodeProcessingPattern(mulInput, mulOutput);
                        slot.set(newPattern);
                    }
                }
            }
        }
    }

    private static boolean checkModify(GenericStack[] stacks, int scale, boolean div) {
        if (stacks == null) return false;
        for (var stack : stacks) {
            if (stack != null) {
                if (div) {
                    if (stack.amount() % scale != 0) return false;
                } else {
                    if (stack.amount() > Integer.MAX_VALUE / scale) return false;
                }
            }
        }
        return true;
    }

    private static void applyScale(GenericStack[] src, GenericStack[] dst, int scale, boolean div) {
        for (int i = 0; i < src.length; i++) {
            var stack = src[i];
            if (stack != null) {
                long newAmt = div ? stack.amount() / scale : stack.amount() * scale;
                dst[i] = new GenericStack(stack.what(), newAmt);
            } else {
                dst[i] = null;
            }
        }
    }
}
