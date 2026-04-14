package com.extendedae_plus.content.matrix.entity;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.helpers.iface.PatternContainer;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.extendedae_plus.content.matrix.cluster.AssemblerMatrixCluster;
import com.extendedae_plus.init.ModBlockEntities;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AssemblerMatrixPatternEntity extends AssemblerMatrixFunctionEntity implements InternalInventoryHost, ICraftingProvider, PatternContainer {

    public static final int INV_SIZE = 36;
    private final AppEngInternalInventory patternInventory;
    private final List<IPatternDetails> patterns = new ArrayList<>();

    public AssemblerMatrixPatternEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ASSEMBLER_MATRIX_PATTERN.get(), pos, blockState);
        this.patternInventory = new AppEngInternalInventory(this, INV_SIZE, 1);
        this.patternInventory.setFilter(new Filter(this::getLevel));
        this.getMainNode().addService(ICraftingProvider.class, this);
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        this.patternInventory.writeToNBT(data, "pattern");
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        this.patternInventory.readFromNBT(data, "pattern");
    }

    @Override
    public boolean isVisibleInTerminal() {
        return this.manager.getSetting(Settings.PATTERN_ACCESS_TERMINAL) == YesNo.YES;
    }

    public AppEngInternalInventory getPatternInventory() {
        return this.patternInventory;
    }

    public AppEngInternalInventory getExposedInventory() {
        return this.patternInventory;
    }

    public long getLocateID() {
        return this.worldPosition.asLong();
    }

    public void updatePatterns() {
        this.patterns.clear();
        for (var stack : this.patternInventory) {
            var details = PatternDetailsHelper.decodePattern(stack, this.getLevel());
            if (details != null) {
                patterns.add(details);
            }
        }
        ICraftingProvider.requestUpdate(this.getMainNode());
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (var pattern : this.patternInventory) {
            drops.add(pattern);
        }
    }

    public void clearPatterns() {
        for (int i = 0; i < this.patternInventory.size(); i++) {
            this.patternInventory.setItemDirect(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void add(AssemblerMatrixCluster c) {
        c.addPattern(this);
    }

    // InternalInventoryHost methods
    @Override
    public void saveChanges() {
        super.saveChanges();
    }

    @Override
    public boolean isClientSide() {
        return level == null || level.isClientSide;
    }

    @Override
    public void onReady() {
        super.onReady();
        this.updatePatterns();
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return this.patterns;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!isFormed() || !this.getMainNode().isActive() || !this.patterns.contains(patternDetails)) {
            return false;
        }
        return this.cluster.pushCraftingJob(patternDetails, inputHolder);
    }

    @Override
    public boolean isBusy() {
        return this.cluster == null || this.cluster.isBusy();
    }

    @Override
    public @Nullable IGrid getGrid() {
        var node = this.getMainNode().getNode();
        return node != null ? node.getGrid() : null;
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return this.patternInventory;
    }

    @Override
    public long getTerminalSortOrder() {
        return this.getLocateID();
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        var icon = AEItemKey.of(ModBlocks.ASSEMBLER_MATRIX_PATTERN.get());
        var name = this.hasCustomInventoryName() ? this.getCustomInventoryName() : Component.translatable("block.extendedae_plus.assembler_matrix_pattern");
        return new PatternContainerGroup(icon, name, List.of(Component.translatable("gui.extendedae_plus.assembler_matrix.pattern")));
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        this.saveChanges();
        this.updatePatterns();
    }

    public record Filter(Supplier<Level> world) implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            if (stack.getItem() instanceof EncodedPatternItem) {
                return PatternDetailsHelper.decodePattern(stack, world.get()) instanceof IMolecularAssemblerSupportedPattern;
            }
            return false;
        }
    }
}
