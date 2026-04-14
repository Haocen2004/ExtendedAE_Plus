package com.extendedae_plus.util.uploadPattern;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.RestrictedInputSlot;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixPatternEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Utility to upload crafting patterns into the Assembler Matrix network.
 */
public final class MatrixUploadUtil {
    private MatrixUploadUtil() {}

    /**
     * Upload a pattern ItemStack into any available Assembler Matrix pattern entity on the grid.
     * @return true if the pattern was inserted successfully
     */
    public static boolean uploadPatternToMatrix(ServerPlayer player, ItemStack pattern, IGrid grid) {
        return uploadPatternToMatrix(player, pattern, grid, false);
    }

    /**
     * Upload a pattern ItemStack into any available Assembler Matrix pattern entity on the grid.
     * @param skipDuplicateCheck if true, skip duplicate detection (when shift is held)
     * @return true if the pattern was inserted successfully
     */
    public static boolean uploadPatternToMatrix(ServerPlayer player, ItemStack pattern, IGrid grid, boolean skipDuplicateCheck) {
        if (pattern.isEmpty()) return false;
        // Only accept crafting-type patterns (IMolecularAssemblerSupportedPattern)
        var level = player.level;
        var decoded = PatternDetailsHelper.decodePattern(pattern, level);
        if (!(decoded instanceof IMolecularAssemblerSupportedPattern)) {
            return false;
        }

        // Check duplicate across all pattern entities on the grid
        if (!skipDuplicateCheck && isDuplicateInGrid(decoded, grid, level)) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("gui.extendedae_plus.assembler_matrix.duplicate_blocked"),
                    false);
            return false;
        }

        var patternEntity = findAvailablePatternEntity(grid);
        if (patternEntity == null) {
            return false;
        }

        var inv = patternEntity.getPatternInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStackInSlot(i).isEmpty()) {
                inv.setItemDirect(i, pattern.copy());
                patternEntity.saveChanges();
                patternEntity.updatePatterns();
                return true;
            }
        }
        return false;
    }

    public static boolean isDuplicateInGrid(appeng.api.crafting.IPatternDetails newPattern, IGrid grid, net.minecraft.world.level.Level level) {
        for (var clazz : grid.getMachineClasses()) {
            if (AssemblerMatrixPatternEntity.class.isAssignableFrom(clazz)) {
                for (var node : grid.getMachineNodes(clazz)) {
                    if (node.getOwner() instanceof AssemblerMatrixPatternEntity pe) {
                        var inv = pe.getPatternInventory();
                        for (int i = 0; i < inv.size(); i++) {
                            var existing = inv.getStackInSlot(i);
                            if (existing.isEmpty()) continue;
                            var existingDetails = PatternDetailsHelper.decodePattern(existing, level);
                            if (existingDetails != null && com.extendedae_plus.content.matrix.menu.AssemblerMatrixMenu.patternsAreEquivalent(newPattern, existingDetails)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Upload the currently encoded pattern from the PatternEncodingTermMenu into the Assembler Matrix.
     */
    public static void uploadFromEncodingMenuToMatrix(ServerPlayer player, PatternEncodingTermMenu menu) {
        uploadFromEncodingMenuToMatrix(player, menu, false);
    }

    /**
     * Upload the currently encoded pattern from the PatternEncodingTermMenu into the Assembler Matrix.
     * @param skipDuplicateCheck if true, skip duplicate detection (shift held)
     */
    public static void uploadFromEncodingMenuToMatrix(ServerPlayer player, PatternEncodingTermMenu menu, boolean skipDuplicateCheck) {
        try {
            // Get the encoded pattern slot via reflection-free approach:
            // The encodedPatternSlot field is accessible through the menu's slots
            ItemStack encodedPattern = ItemStack.EMPTY;
            for (var slot : menu.slots) {
                if (slot instanceof RestrictedInputSlot ris) {
                    var stack = ris.getItem();
                    if (!stack.isEmpty() && stack.getItem() instanceof EncodedPatternItem) {
                        encodedPattern = stack;
                        break;
                    }
                }
            }

            if (encodedPattern.isEmpty()) return;

            var grid = getGridFromMenu(menu);
            if (grid == null) return;

            if (uploadPatternToMatrix(player, encodedPattern, grid, skipDuplicateCheck)) {
                // Clear the encoded pattern slot on success
                for (var slot : menu.slots) {
                    if (slot instanceof RestrictedInputSlot ris) {
                        if (ItemStack.isSame(ris.getItem(), encodedPattern)) {
                            ris.set(ItemStack.EMPTY);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    private static AssemblerMatrixPatternEntity findAvailablePatternEntity(IGrid grid) {
        // Use getMachineClasses to find all AssemblerMatrixPatternEntity on the grid
        for (var clazz : grid.getMachineClasses()) {
            if (AssemblerMatrixPatternEntity.class.isAssignableFrom(clazz)) {
                for (var node : grid.getMachineNodes(clazz)) {
                    if (node.getOwner() instanceof AssemblerMatrixPatternEntity pattern) {
                        var inv = pattern.getPatternInventory();
                        for (int i = 0; i < inv.size(); i++) {
                            if (inv.getStackInSlot(i).isEmpty()) {
                                return pattern;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static IGrid getGridFromMenu(PatternEncodingTermMenu menu) {
        try {
            var node = menu.getNetworkNode();
            if (node != null) {
                return node.getGrid();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
