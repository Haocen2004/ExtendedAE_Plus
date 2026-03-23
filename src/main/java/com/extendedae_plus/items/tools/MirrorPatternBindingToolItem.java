package com.extendedae_plus.items.tools;

import appeng.blockentity.crafting.PatternProviderBlockEntity;
import com.extendedae_plus.content.ae2.MirrorPatternProviderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MirrorPatternBindingToolItem extends Item {
    private static final String TAG_SELECTED_MASTER = "selectedMaster";
    private static final String TAG_DIMENSION = "dimension";
    private static final String TAG_POS = "pos";

    public MirrorPatternBindingToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var blockEntity = level.getBlockEntity(context.getClickedPos());
        var stack = context.getItemInHand();

        if (blockEntity instanceof PatternProviderBlockEntity
                && !(blockEntity instanceof MirrorPatternProviderBlockEntity)) {
            if (player != null && player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    if (MirrorPatternProviderBlockEntity.isSupportedMaster(blockEntity)) {
                        setSelectedMaster(stack, GlobalPos.of(level.dimension(), context.getClickedPos()));
                        player.displayClientMessage(
                                Component.translatable(
                                        "extendedae_plus.message.mirror_binding_tool.selected",
                                        context.getClickedPos().getX(),
                                        context.getClickedPos().getY(),
                                        context.getClickedPos().getZ()),
                                true);
                    } else {
                        player.displayClientMessage(
                                Component.translatable(
                                        "extendedae_plus.message.mirror_binding_tool.only_normal_provider"),
                                true);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            return InteractionResult.PASS;
        }

        if (blockEntity instanceof MirrorPatternProviderBlockEntity mirror) {
            if (!level.isClientSide) {
                var selectedMaster = getSelectedMaster(stack);
                if (selectedMaster == null) {
                    if (player != null) {
                        player.displayClientMessage(
                                Component.translatable("extendedae_plus.message.mirror_binding_tool.no_selection"),
                                true);
                    }
                    return InteractionResult.SUCCESS;
                }

                if (mirror.bindToMaster(selectedMaster)) {
                    if (player != null) {
                        player.displayClientMessage(mirror.createBoundMessage(), true);
                    }
                } else if (player != null) {
                    player.displayClientMessage(
                            Component.translatable("extendedae_plus.message.mirror_binding_tool.bind_failed"),
                            true);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.translatable("item.extendedae_plus.mirror_pattern_binding_tool.tip.select"));
        tooltipComponents.add(Component.translatable("item.extendedae_plus.mirror_pattern_binding_tool.tip.bind"));
        tooltipComponents.add(Component.translatable("item.extendedae_plus.mirror_pattern_binding_tool.tip.clear"));

        var selectedMaster = getSelectedMaster(stack);
        if (selectedMaster != null) {
            var pos = selectedMaster.pos();
            tooltipComponents.add(Component.translatable(
                    "item.extendedae_plus.mirror_pattern_binding_tool.selected",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()));
            tooltipComponents.add(Component.translatable(
                    "item.extendedae_plus.mirror_pattern_binding_tool.dimension",
                    selectedMaster.dimension().location().toString()));
        } else {
            tooltipComponents.add(Component.translatable("item.extendedae_plus.mirror_pattern_binding_tool.unset"));
        }
    }

    private static void setSelectedMaster(ItemStack stack, GlobalPos master) {
        var tag = stack.getOrCreateTag();
        var selectedTag = new CompoundTag();
        selectedTag.putString(TAG_DIMENSION, master.dimension().location().toString());
        selectedTag.putLong(TAG_POS, master.pos().asLong());
        tag.put(TAG_SELECTED_MASTER, selectedTag);
    }

    private static void clearSelectedMaster(ItemStack stack) {
        var tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_SELECTED_MASTER);
        }
    }

    @Nullable
    private static GlobalPos getSelectedMaster(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SELECTED_MASTER, Tag.TAG_COMPOUND)) {
            return null;
        }

        var selectedTag = tag.getCompound(TAG_SELECTED_MASTER);
        if (!selectedTag.contains(TAG_DIMENSION, Tag.TAG_STRING) || !selectedTag.contains(TAG_POS, Tag.TAG_LONG)) {
            return null;
        }

        return GlobalPos.of(
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation(selectedTag.getString(TAG_DIMENSION))),
                BlockPos.of(selectedTag.getLong(TAG_POS)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);

        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            if (getSelectedMaster(stack) != null) {
                clearSelectedMaster(stack);
                player.displayClientMessage(
                        Component.translatable("extendedae_plus.message.mirror_binding_tool.cleared"),
                        true);
            } else {
                player.displayClientMessage(
                        Component.translatable("extendedae_plus.message.mirror_binding_tool.no_selection"),
                        true);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
