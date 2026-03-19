package com.extendedae_plus.content.matrix;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.AEItemFilters;
import appeng.util.inv.filter.IAEItemFilter;
import com.extendedae_plus.init.ModBlockEntities;
import com.extendedae_plus.init.ModItems;
import com.extendedae_plus.mixin.extendedae.accessor.TileAssemblerMatrixPatternAccessor;
import com.extendedae_plus.mixin.minecraft.accessor.BlockEntityAccessor;
import com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

public class PatternCorePlusBlockEntity extends TileAssemblerMatrixPattern {

    public static final int INV_SIZE = 72;

    private final FilteredInternalInventory exposedInventory;

    public PatternCorePlusBlockEntity(BlockPos pos, BlockState blockState) {
        super(pos, blockState);

        ((BlockEntityAccessor) (Object) this)
                .extendedae_plus$setType(ModBlockEntities.ASSEMBLER_MATRIX_PATTERN_PLUS_BE.get());

        var inventory = new AppEngInternalInventory(this, INV_SIZE, 1);
        inventory.setFilter(new Filter(this::getLevel));
        ((TileAssemblerMatrixPatternAccessor) (Object) this).extendedae_plus$setPatternInventory(inventory);
        this.exposedInventory = new FilteredInternalInventory(inventory, AEItemFilters.INSERT_ONLY);
    }

    @Override
    public FilteredInternalInventory getExposedInventory() {
        return this.exposedInventory;
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        var icon = AEItemKey.of(ModItems.ASSEMBLER_MATRIX_PATTERN_PLUS.get());
        var name = this.hasCustomName() ? this.getCustomName() : icon.getDisplayName();
        return new PatternContainerGroup(
                icon,
                name,
                List.of(Component.translatable("gui.extendedae_plus.assembler_matrix.pattern"))
        );
    }

    @Override
    public BlockEntityType<?> getType() {
        return ModBlockEntities.ASSEMBLER_MATRIX_PATTERN_PLUS_BE.get();
    }

    public record Filter(Supplier<Level> world) implements IAEItemFilter {

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            if (stack.getItem() instanceof EncodedPatternItem) {
                return PatternDetailsHelper.decodePattern(stack, world.get())
                        instanceof IMolecularAssemblerSupportedPattern;
            }
            return false;
        }
    }
}
