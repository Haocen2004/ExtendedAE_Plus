package com.extendedae_plus.content.quantum.block;

import com.extendedae_plus.content.quantum.QuantumCraftingUnitType;
import com.extendedae_plus.content.quantum.entity.QuantumCraftingBlockEntity;
import com.extendedae_plus.init.ModMenuTypes;
import com.extendedae_plus.util.Logger;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import appeng.block.AEBaseEntityBlock;

/**
 * Block class for all Quantum Computer multiblock types.
 * Backported from AdvancedAE's AAEAbstractCraftingUnitBlock + AAECraftingUnitBlock.
 */
public class QuantumCraftingUnitBlock extends AEBaseEntityBlock<QuantumCraftingBlockEntity> {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public final QuantumCraftingUnitType type;

    public QuantumCraftingUnitBlock(QuantumCraftingUnitType type) {
        super(buildProperties(type));
        this.type = type;
        this.registerDefaultState(defaultBlockState()
                .setValue(FORMED, false)
                .setValue(POWERED, false));
    }

    private static Properties buildProperties(QuantumCraftingUnitType type) {
        var props = (type == QuantumCraftingUnitType.STRUCTURE
                ? Properties.of(Material.GLASS)
                : Properties.of(Material.METAL))
                .strength(2F, 6.0F)
                .requiresCorrectToolForDrops();

        if (type == QuantumCraftingUnitType.QUANTUM_CORE || type == QuantumCraftingUnitType.STRUCTURE) {
            props = props.lightLevel(state -> state.getValue(POWERED) ? 15 : 0).noOcclusion();
        }

        if (type == QuantumCraftingUnitType.STRUCTURE) {
            props = props.isViewBlocking((a, b, c) -> false);
        }
        return props;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
        builder.add(FORMED);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState stateIn, @NotNull Direction facing,
            @NotNull BlockState facingState, @NotNull LevelAccessor level,
            @NotNull BlockPos currentPos, @NotNull BlockPos facingPos) {
        var te = level.getBlockEntity(currentPos);
        if (te != null) {
            te.requestModelDataUpdate();
        }
        return super.updateShape(stateIn, facing, facingState, level, currentPos, facingPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving) {
        var cp = this.getBlockEntity(level, pos);
        if (cp != null) {
            cp.updateMultiBlock(fromPos);
        }
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos,
            BlockState newState, boolean isMoving) {
        if (newState.getBlock() == state.getBlock()) {
            return;
        }
        var cp = this.getBlockEntity(level, pos);
        if (cp != null) {
            cp.breakCluster();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult onActivated(Level level, BlockPos pos, Player player, InteractionHand hand,
            @Nullable ItemStack heldItem, BlockHitResult hit) {
        final QuantumCraftingBlockEntity te = this.getBlockEntity(level, pos);
        
        // Always allow opening the menu if a valid quantum block entity exists.
        // This avoids GUI being blocked by transient cluster/active state mismatches.
        if (te != null) {
            if (!level.isClientSide()) {
                Logger.EAP$LOGGER.debug("Quantum GUI open requested at {} by {} (type={})",
                        pos, player.getGameProfile().getName(), this.type);
                // Open Quantum Computer GUI
                MenuOpener.open(ModMenuTypes.QUANTUM_COMPUTER.get(), player, MenuLocators.forBlockEntity(te));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        
        return InteractionResult.PASS;
    }

    @Override
    public float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        if (this.type == QuantumCraftingUnitType.STRUCTURE) {
            return 1.0f;
        }
        return super.getShadeBrightness(state, level, pos);
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        if (this.type == QuantumCraftingUnitType.STRUCTURE) {
            return true;
        }
        return super.propagatesSkylightDown(state, level, pos);
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter level,
            @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (this.type == QuantumCraftingUnitType.STRUCTURE) {
            return Shapes.empty();
        }
        return super.getVisualShape(state, level, pos, context);
    }

    @Override
    public boolean skipRendering(@NotNull BlockState state, @NotNull BlockState adjacentState, @NotNull Direction direction) {
        if (this.type == QuantumCraftingUnitType.STRUCTURE
                && adjacentState.getBlock() instanceof QuantumCraftingUnitBlock other
                && other.type == QuantumCraftingUnitType.STRUCTURE) {
            return true;
        }
        return super.skipRendering(state, adjacentState, direction);
    }

    public Item getPresentItem() {
        return this.type.getItemFromType();
    }
}
