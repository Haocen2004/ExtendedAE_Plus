package com.extendedae_plus.content.matrix.gui;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.helpers.InventoryAction;
import appeng.menu.slot.AppEngSlot;
import appeng.util.inv.AppEngInternalInventory;
import com.extendedae_plus.content.matrix.entity.AssemblerMatrixPatternEntity;
import com.extendedae_plus.content.matrix.menu.AssemblerMatrixMenu;
import com.extendedae_plus.content.matrix.network.CAssemblerMatrixActionPacket;
import com.extendedae_plus.init.ModNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AssemblerMatrixScreen extends AEBaseScreen<AssemblerMatrixMenu> {

    private static final int GUI_WIDTH = 195;
    private static final int GUI_HEADER_HEIGHT = 17;
    private static final int GUI_FOOTER_HEIGHT = 97;
    private static final int GUI_PADDING_X = 8;
    private static final int COLUMNS = 9;
    private static final int ROW_HEIGHT = 18;
    private static final int SLOT_SIZE = 18;
    private static final int VISIBLE_ROWS = 5;

    private static final Rect2i HEADER_BBOX = new Rect2i(0, 0, GUI_WIDTH, GUI_HEADER_HEIGHT);
    private static final Rect2i ROW_TEXT_TOP_BBOX = new Rect2i(0, 17, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_TEXT_MIDDLE_BBOX = new Rect2i(0, 53, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_TEXT_BOTTOM_BBOX = new Rect2i(0, 89, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_TOP_BBOX = new Rect2i(0, 35, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_MIDDLE_BBOX = new Rect2i(0, 71, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_BOTTOM_BBOX = new Rect2i(0, 107, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i FOOTER_BBOX = new Rect2i(0, 125, GUI_WIDTH, GUI_FOOTER_HEIGHT);

    // Per-entity raw data (keyed by entity locateID)
    private final Long2ReferenceOpenHashMap<EntityPatternData> entityData = new Long2ReferenceOpenHashMap<>();

    // Unified flat list after merging all entities
    private final ArrayList<SlotEntry> allSlots = new ArrayList<>();
    private final ArrayList<SlotEntry> filteredSlots = new ArrayList<>();

    private final Scrollbar scrollbar;
    private final AETextField searchField;
    private Button cancelButton;

    public AssemblerMatrixScreen(AssemblerMatrixMenu menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
        this.scrollbar = widgets.addScrollBar("scrollbar");
        this.searchField = widgets.addTextField("search");
        this.searchField.setResponder(s -> this.rebuildFilteredSlots());
        this.searchField.setPlaceholder(Component.translatable("gui.extendedae_plus.assembler_matrix.tooltip"));
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + GUI_FOOTER_HEIGHT;
    }

    @Override
    public void init() {
        super.init();
        this.setInitialFocus(this.searchField);
        this.cancelButton = new Button(this.leftPos + 8, this.topPos + GUI_HEADER_HEIGHT + 1, 46, 14,
                Component.translatable("gui.extendedae_plus.assembler_matrix.cancel"),
                b -> ModNetwork.CHANNEL.sendToServer(new CAssemblerMatrixActionPacket(CAssemblerMatrixActionPacket.ACTION_CANCEL)));
        this.addRenderableWidget(this.cancelButton);
        this.resetScrollbar();
    }

    private void resetScrollbar() {
        int totalRows = (this.filteredSlots.size() + COLUMNS - 1) / COLUMNS;
        int visDataRows = VISIBLE_ROWS - 1; // row 0 is info text
        this.scrollbar.setHeight(VISIBLE_ROWS * ROW_HEIGHT - 2);
        this.scrollbar.setRange(0, Math.max(0, totalRows - visDataRows), 2);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        super.render(poseStack, mouseX, mouseY, partialTicks);
        if (this.cancelButton != null && this.cancelButton.isHoveredOrFocused()) {
            this.renderTooltip(poseStack,
                    Component.translatable("gui.extendedae_plus.assembler_matrix.cancel.tooltip"),
                    mouseX, mouseY);
        }
    }

    @Override
    public void drawFG(PoseStack poseStack, int offsetX, int offsetY, int mouseX, int mouseY) {
        this.menu.slots.removeIf(slot -> slot instanceof MatrixPatternSlot);

        int textColor = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();

        // Pattern count after title
        int totalSlots = 0;
        int usedSlots = 0;
        for (var data : this.entityData.values()) {
            totalSlots += data.stacks.length;
            for (var stack : data.stacks) {
                if (!stack.isEmpty()) usedSlots++;
            }
        }
        String countSuffix = " (" + usedSlots + "/" + totalSlots + ")";
        Component titleText = Component.translatable("gui.extendedae_plus.assembler_matrix.title");
        int titleWidth = this.font.width(titleText);
        this.font.draw(poseStack, countSuffix, 8 + titleWidth, 6, textColor);

        // Row 0: Running Jobs text
        this.font.draw(poseStack,
                Component.translatable("gui.extendedae_plus.assembler_matrix.threads",
                        String.valueOf(this.menu.getClientRunningThreads())),
                GUI_PADDING_X + 48, GUI_HEADER_HEIGHT + 4, textColor);

        // Rows 1+: unified pattern grid
        final int scrollLevel = scrollbar.getCurrentScroll();
        int visDataRows = VISIBLE_ROWS - 1;
        for (int visRow = 0; visRow < visDataRows; visRow++) {
            int dataRow = scrollLevel + visRow;
            for (int col = 0; col < COLUMNS; col++) {
                int flatIdx = dataRow * COLUMNS + col;
                if (flatIdx < this.filteredSlots.size()) {
                    var entry = this.filteredSlots.get(flatIdx);
                    // Default to showing pattern result items (decoded output)
                    ItemStack displayStack = getPatternDisplayStack(entry.stack);
                    var dispInv = new AppEngInternalInventory(1);
                    dispInv.setItemDirect(0, displayStack);
                    var slot = new MatrixPatternSlot(
                            dispInv, 0,
                            col * SLOT_SIZE + GUI_PADDING_X,
                            (visRow + 2) * SLOT_SIZE,
                            entry.entityId, entry.entitySlot);
                    this.menu.slots.add(slot);
                }
            }
        }
    }

    @Override
    public void drawBG(PoseStack poseStack, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        this.bindTexture("guis/patternaccessterminal.png");

        blitRect(poseStack, offsetX, offsetY, HEADER_BBOX);

        int currentY = offsetY + GUI_HEADER_HEIGHT;
        final int scrollLevel = scrollbar.getCurrentScroll();
        int totalRows = (this.filteredSlots.size() + COLUMNS - 1) / COLUMNS;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            boolean lastLine = i == VISIBLE_ROWS - 1;

            if (i == 0) {
                // First row is info text
                blitRect(poseStack, offsetX, currentY, ROW_TEXT_TOP_BBOX);
            } else {
                int dataRow = scrollLevel + (i - 1);
                Rect2i emptyBg = selectRowBg(false, false, lastLine);
                blitRect(poseStack, offsetX, currentY, emptyBg);

                if (dataRow < totalRows) {
                    int startIdx = dataRow * COLUMNS;
                    int slotsInRow = Math.min(COLUMNS, this.filteredSlots.size() - startIdx);
                    if (slotsInRow > 0) {
                        Rect2i invBg = selectRowBg(true, false, lastLine);
                        blit(poseStack, offsetX, currentY, invBg.getX(), invBg.getY(),
                                GUI_PADDING_X + SLOT_SIZE * slotsInRow - 1, invBg.getHeight());
                    }
                }
            }
            currentY += ROW_HEIGHT;
        }

        blitRect(poseStack, offsetX, currentY, FOOTER_BBOX);
    }

    private void blitRect(PoseStack poseStack, int x, int y, Rect2i src) {
        blit(poseStack, x, y, src.getX(), src.getY(), src.getWidth(), src.getHeight());
    }

    private Rect2i selectRowBg(boolean isInvLine, boolean firstLine, boolean lastLine) {
        if (isInvLine) {
            if (firstLine) return ROW_INVENTORY_TOP_BBOX;
            else if (lastLine) return ROW_INVENTORY_BOTTOM_BBOX;
            else return ROW_INVENTORY_MIDDLE_BBOX;
        } else {
            if (firstLine) return ROW_TEXT_TOP_BBOX;
            else if (lastLine) return ROW_TEXT_BOTTOM_BBOX;
            else return ROW_TEXT_MIDDLE_BBOX;
        }
    }

    @Override
    public boolean mouseClicked(double xCoord, double yCoord, int btn) {
        if (btn == 1 && this.searchField.isMouseOver(xCoord, yCoord)) {
            this.searchField.setValue("");
        }
        return super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void slotClicked(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (slot instanceof MatrixPatternSlot mSlot) {
            InventoryAction action = null;
            switch (clickType) {
                case PICKUP -> action = mouseButton == 1
                        ? InventoryAction.SPLIT_OR_PLACE_SINGLE
                        : InventoryAction.PICKUP_OR_SET_DOWN;
                case QUICK_MOVE -> action = mouseButton == 1
                        ? InventoryAction.PICKUP_SINGLE
                        : InventoryAction.SHIFT_CLICK;
                case CLONE -> {
                    if (getPlayer().getAbilities().instabuild) {
                        action = InventoryAction.CREATIVE_DUPLICATE;
                    }
                }
            }
            if (action != null) {
                var p = new InventoryActionPacket(action, mSlot.machineSlot, mSlot.machineId);
                NetworkHandler.instance().sendToServer(p);
            }
            return;
        }
        super.slotClicked(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    public boolean charTyped(char character, int key) {
        if (character == ' ' && this.searchField.getValue().isEmpty()) {
            return true;
        }
        return super.charTyped(character, key);
    }

    // Called by network packet handler
    public void receiveUpdate(long entityId, Int2ObjectMap<ItemStack> updateMap) {
        var data = this.entityData.computeIfAbsent(entityId, id -> new EntityPatternData(id, AssemblerMatrixPatternEntity.INV_SIZE));
        for (var entry : updateMap.int2ObjectEntrySet()) {
            int slot = entry.getIntKey();
            if (slot >= 0 && slot < data.stacks.length) {
                data.stacks[slot] = entry.getValue();
            }
        }
        this.rebuildFilteredSlots();
    }

    private void rebuildFilteredSlots() {
        // Step 1: Build unified flat list from all entities
        this.allSlots.clear();
        for (var id : getSortedEntityIds()) {
            var data = this.entityData.get(id);
            for (int i = 0; i < data.stacks.length; i++) {
                if (!data.stacks[i].isEmpty()) {
                    this.allSlots.add(new SlotEntry(data.entityId, i, data.stacks[i]));
                }
            }
        }

        // Step 2: Filter by search
        this.filteredSlots.clear();
        String filter = this.searchField != null ? this.searchField.getValue() : "";
        if (filter.isBlank()) {
            this.filteredSlots.addAll(this.allSlots);
        } else {
            String[] tokens = filter.toLowerCase().split("\\s+");
            for (var entry : this.allSlots) {
                if (matchesPattern(entry.stack, tokens)) {
                    this.filteredSlots.add(entry);
                }
            }
        }

        // Step 3: Append one empty slot at the end (for placing new patterns)
        // Find first available empty slot across all entities
        SlotEntry emptyTarget = findFirstEmptySlot();
        if (emptyTarget != null) {
            this.filteredSlots.add(emptyTarget);
        }

        this.resetScrollbar();
    }

    private SlotEntry findFirstEmptySlot() {
        for (var id : getSortedEntityIds()) {
            var data = this.entityData.get(id);
            for (int i = 0; i < data.stacks.length; i++) {
                if (data.stacks[i].isEmpty()) {
                    return new SlotEntry(data.entityId, i, ItemStack.EMPTY);
                }
            }
        }
        return null;
    }

    private ItemStack getPatternDisplayStack(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        if (!(stack.getItem() instanceof EncodedPatternItem)) return stack;
        var details = PatternDetailsHelper.decodePattern(stack, this.minecraft.level);
        if (details == null) return stack;
        var outputs = details.getOutputs();
        if (outputs.length > 0 && outputs[0] != null) {
            var what = outputs[0].what();
            if (what instanceof AEItemKey itemKey) {
                return itemKey.toStack();
            }
        }
        return stack;
    }

    private boolean matchesPattern(ItemStack stack, String[] tokens) {
        if (stack.isEmpty()) return false;
        IPatternDetails result = null;
        if (stack.getItem() instanceof EncodedPatternItem) {
            result = PatternDetailsHelper.decodePattern(stack, this.minecraft.level);
        }
        if (result == null) return false;
        for (var out : result.getOutputs()) {
            if (out != null && matchesAllTokens(out.what().getDisplayName().getString(), tokens)) {
                return true;
            }
        }
        for (var in : result.getInputs()) {
            if (in != null) {
                for (var possible : in.getPossibleInputs()) {
                    if (matchesAllTokens(possible.what().getDisplayName().getString(), tokens)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean matchesAllTokens(String name, String[] tokens) {
        String lower = name.toLowerCase();
        for (var token : tokens) {
            if (!lower.contains(token)) return false;
        }
        return true;
    }

    private long[] getSortedEntityIds() {
        return this.entityData.keySet().longStream().sorted().toArray();
    }

    // --- Inner types ---

    private static class MatrixPatternSlot extends AppEngSlot {
        final long machineId;
        final int machineSlot;

        MatrixPatternSlot(AppEngInternalInventory inv, int localSlotIndex, int x, int y, long machineId, int machineSlot) {
            super(inv, localSlotIndex);
            this.x = x;
            this.y = y;
            this.machineId = machineId;
            this.machineSlot = machineSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void set(ItemStack stack) {
        }

        @Override
        public void initialize(ItemStack stack) {
        }

        @Override
        public int getMaxStackSize() {
            return 0;
        }

        @Override
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private static class EntityPatternData {
        final long entityId;
        final ItemStack[] stacks;

        EntityPatternData(long entityId, int size) {
            this.entityId = entityId;
            this.stacks = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                this.stacks[i] = ItemStack.EMPTY;
            }
        }
    }

    private static class SlotEntry {
        final long entityId;
        final int entitySlot;
        final ItemStack stack;

        SlotEntry(long entityId, int entitySlot, ItemStack stack) {
            this.entityId = entityId;
            this.entitySlot = entitySlot;
            this.stack = stack;
        }
    }
}
