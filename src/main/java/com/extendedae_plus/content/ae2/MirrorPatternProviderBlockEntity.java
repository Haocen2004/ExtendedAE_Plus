package com.extendedae_plus.content.ae2;

import appeng.api.config.Settings;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IManagedGridNode;
import appeng.block.crafting.PatternProviderBlock;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.iface.PatternProviderLogic;
import appeng.util.CustomNameUtil;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import com.extendedae_plus.api.advancedBlocking.IAdvancedBlocking;
import com.extendedae_plus.api.smartDoubling.ISmartDoublingHolder;
import com.extendedae_plus.api.bridge.PatternProviderLogicSyncBridge;
import com.extendedae_plus.config.ModConfig;
import com.extendedae_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

public class MirrorPatternProviderBlockEntity extends PatternProviderBlockEntity {
    private static final String TAG_MASTER = "mirrorMaster";
    private static final String TAG_MASTER_DIMENSION = "dimension";
    private static final String TAG_MASTER_POS = "pos";
    private static final int FAST_SYNC_INTERVAL = 2;
    private static final int STABLE_SYNC_INTERVAL = 20;
    private static final int UNLOADED_MASTER_RETRY_INTERVAL = 40;
    private static final int UNKNOWN_VERSION_PATTERN_COMPARE_INTERVAL = 100;
    private static final int AE2_PATTERN_SLOTS = 9;
    private static final int EXTENDED_PATTERN_PROVIDER_BASE_SLOTS = 36;
    private static final InternalInventory DISABLED_PATTERN_INVENTORY = new AppEngInternalInventory(0);
    private static final long UNKNOWN_PATTERN_SYNC_VERSION = Long.MIN_VALUE;

    @Nullable
    private ResourceKey<Level> masterDimension;

    @Nullable
    private BlockPos masterPos;

    private long nextSyncTick = Long.MIN_VALUE;
    private long nextUnknownVersionPatternCompareTick = Long.MIN_VALUE;
    private long lastSyncedPatternVersion = UNKNOWN_PATTERN_SYNC_VERSION;
    private boolean needsUnboundPatternCleanup;

    public MirrorPatternProviderBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MIRROR_PATTERN_PROVIDER_BE.get(), pos, blockState);
    }

    @Override
    public boolean isVisibleInTerminal() {
        return false;
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return DISABLED_PATTERN_INVENTORY;
    }

    @Override
    protected PatternProviderLogic createLogic() {
        return new MirrorLogic(this.getMainNode(), this, getMirrorPatternSlotCapacity());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            MirrorPatternProviderBlockEntity blockEntity) {
        if (level instanceof ServerLevel serverLevel) {
            blockEntity.serverTick(serverLevel);
        }
    }

    private void serverTick(ServerLevel level) {
        if (level.getGameTime() < this.nextSyncTick) {
            return;
        }

        this.nextSyncTick = level.getGameTime() + this.syncBoundMaster();
    }

    @Override
    public void onReady() {
        super.onReady();
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            this.scheduleImmediateSync();
            this.nextSyncTick = serverLevel.getGameTime() + this.syncBoundMaster();
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);

        if (this.masterDimension != null && this.masterPos != null) {
            var masterTag = new CompoundTag();
            masterTag.putString(TAG_MASTER_DIMENSION, this.masterDimension.location().toString());
            masterTag.putLong(TAG_MASTER_POS, this.masterPos.asLong());
            data.put(TAG_MASTER, masterTag);
        } else {
            data.remove(TAG_MASTER);
        }
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);

        this.masterDimension = null;
        this.masterPos = null;
        this.scheduleImmediateSync();
        this.invalidatePatternSyncState();
        this.needsUnboundPatternCleanup = true;
        if (data.contains(TAG_MASTER, Tag.TAG_COMPOUND)) {
            var masterTag = data.getCompound(TAG_MASTER);
            if (masterTag.contains(TAG_MASTER_DIMENSION, Tag.TAG_STRING)
                    && masterTag.contains(TAG_MASTER_POS, Tag.TAG_LONG)) {
                this.masterDimension = ResourceKey.create(Registry.DIMENSION_REGISTRY,
                        new ResourceLocation(masterTag.getString(TAG_MASTER_DIMENSION)));
                this.masterPos = BlockPos.of(masterTag.getLong(TAG_MASTER_POS));
                this.needsUnboundPatternCleanup = false;
            }
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        var patternInventory = this.getPatternInventory();
        var snapshot = this.copyInventoryContents(patternInventory);

        this.clearInventory(patternInventory);
        this.getLogic().updatePatterns();
        super.addAdditionalDrops(level, pos, drops);

        this.restoreInventoryContents(patternInventory, snapshot);
        this.getLogic().updatePatterns();
    }

    public void clearContent() {
        var inv = this.getLogic().getPatternInv();
        for (int i = 0; i < inv.size(); i++) {
            inv.setItemDirect(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
        this.getLogic().updatePatterns();
    }

    public boolean bindToMaster(GlobalPos master) {
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (master.pos().equals(this.getBlockPos()) && master.dimension().equals(serverLevel.dimension())) {
            return false;
        }

        var masterLevel = serverLevel.getServer().getLevel(master.dimension());
        if (masterLevel != null && masterLevel.hasChunkAt(master.pos())) {
            var blockEntity = masterLevel.getBlockEntity(master.pos());
            if (isValidMaster(blockEntity)) {
                this.syncFromMaster((PatternProviderBlockEntity) blockEntity);
                return true;
            }
            return false;
        }

        var changed = this.setBoundMaster(master.dimension(), master.pos());
        if (changed) {
            this.flushStateChanges();
        }
        this.scheduleImmediateSync();
        return true;
    }

    @Nullable
    public PatternProviderBlockEntity getMaster() {
        if (this.masterDimension == null || this.masterPos == null || !(this.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }

        var masterLevel = this.getMasterLevel(serverLevel);
        if (masterLevel == null || !masterLevel.hasChunkAt(this.masterPos)) {
            return null;
        }

        var blockEntity = masterLevel.getBlockEntity(this.masterPos);
        return isValidMaster(blockEntity) ? (PatternProviderBlockEntity) blockEntity : null;
    }

    public Component createBoundMessage() {
        if (this.masterPos != null) {
            return Component.translatable(
                    "extendedae_plus.message.mirror_pattern_provider.bound",
                    this.masterPos.getX(),
                    this.masterPos.getY(),
                    this.masterPos.getZ());
        }

        return Component.translatable("extendedae_plus.message.mirror_pattern_provider.missing_master");
    }

    public Component getStatusMessage() {
        if (this.masterPos != null) {
            return Component.translatable(
                    "extendedae_plus.message.mirror_pattern_provider.following",
                    this.masterPos.getX(),
                    this.masterPos.getY(),
                    this.masterPos.getZ());
        }

        return Component.translatable("extendedae_plus.message.mirror_pattern_provider.missing_master");
    }

    public boolean hasMasterBinding() {
        return this.masterDimension != null && this.masterPos != null;
    }

    public boolean unbindFromMaster() {
        if (!(this.getLevel() instanceof ServerLevel)) {
            return false;
        }

        if (!this.hasMasterBinding()) {
            return false;
        }

        var changed = this.clearMasterBinding(true);
        if (changed) {
            this.flushStateChanges();
        }
        this.scheduleImmediateSync();
        return true;
    }

    public Component createUnboundMessage() {
        return Component.translatable("extendedae_plus.message.mirror_pattern_provider.unbound");
    }

    private int syncBoundMaster() {
        if (this.masterDimension == null || this.masterPos == null) {
            if (this.needsUnboundPatternCleanup) {
                this.needsUnboundPatternCleanup = false;
                if (this.clearMirroredPatterns()) {
                    this.flushStateChanges();
                }
            }
            return UNLOADED_MASTER_RETRY_INTERVAL;
        }

        var master = this.getMaster();
        if (master != null) {
            return this.syncFromMaster(master) ? FAST_SYNC_INTERVAL : STABLE_SYNC_INTERVAL;
        }

        if (this.shouldClearBrokenBinding()) {
            if (this.clearMasterBinding(true)) {
                this.flushStateChanges();
                return FAST_SYNC_INTERVAL;
            }
            return STABLE_SYNC_INTERVAL;
        }

        return UNLOADED_MASTER_RETRY_INTERVAL;
    }

    private boolean shouldClearBrokenBinding() {
        if (this.masterDimension == null || this.masterPos == null) {
            return false;
        }

        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            return true;
        }

        var masterLevel = this.getMasterLevel(serverLevel);
        if (masterLevel == null || !masterLevel.hasChunkAt(this.masterPos)) {
            return false;
        }

        return !isValidMaster(masterLevel.getBlockEntity(this.masterPos));
    }

    private boolean clearMasterBinding(boolean clearMirroredPatterns) {
        var hadBinding = this.masterDimension != null || this.masterPos != null;

        this.masterDimension = null;
        this.masterPos = null;
        this.invalidatePatternSyncState();
        this.needsUnboundPatternCleanup = false;

        var changed = hadBinding;
        if (clearMirroredPatterns) {
            changed |= this.clearMirroredPatterns();
            changed |= this.resetMirroredSettingsToInitialState();
        }

        return changed;
    }

    private boolean setBoundMaster(ResourceKey<Level> dimension, BlockPos pos) {
        var newPos = pos.immutable();
        var changed = !Objects.equals(this.masterDimension, dimension) || !Objects.equals(this.masterPos, newPos);

        this.masterDimension = dimension;
        this.masterPos = newPos;
        this.needsUnboundPatternCleanup = false;
        if (changed) {
            this.invalidatePatternSyncState();
        }
        return changed;
    }

    private boolean syncFromMaster(PatternProviderBlockEntity master) {
        var masterLevel = master.getLevel();
        if (masterLevel == null) {
            return false;
        }

        var bindingChanged = this.setBoundMaster(masterLevel.dimension(), master.getBlockPos());
        var changed = bindingChanged;
        changed |= this.syncMirroredOrientationFields(master);
        changed |= this.syncMirroredDirection(master);
        changed |= this.syncMirroredSettings(master);
        changed |= this.syncMirroredPatterns(master, bindingChanged);

        if (changed) {
            this.flushStateChanges();
        }

        return changed;
    }

    private boolean syncMirroredOrientationFields(PatternProviderBlockEntity master) {
        boolean changed = false;
        changed |= copyDirectionField(master, this, "forward");
        changed |= copyDirectionField(master, this, "up");
        changed |= copyBooleanField(master, this, "omniDirectional");
        changed |= copyBooleanField(master, this, "omnidirectional");
        return changed;
    }

    private boolean syncMirroredDirection(PatternProviderBlockEntity master) {
        var level = this.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }

        var masterState = master.getBlockState();
        var mirrorState = this.getBlockState();
        var updatedState = mirrorState;

        for (var masterProperty : masterState.getProperties()) {
            if (masterProperty.getValueClass() != Direction.class) {
                continue;
            }

            var mirrorProperty = findDirectionProperty(updatedState, masterProperty.getName());
            if (mirrorProperty == null) {
                continue;
            }

            var masterDirection = getDirectionValue(masterState, masterProperty);
            if (masterDirection == null || !mirrorProperty.getPossibleValues().contains(masterDirection)) {
                continue;
            }

            var currentDirection = getDirectionValue(updatedState, mirrorProperty);
            if (currentDirection != masterDirection) {
                updatedState = setDirectionValue(updatedState, mirrorProperty, masterDirection);
            }
        }

        if (updatedState == mirrorState) {
            return false;
        }

        level.setBlock(this.getBlockPos(), updatedState, 3);
        return true;
    }

    private boolean syncMirroredSettings(PatternProviderBlockEntity master) {
        if (!this.hasDifferentMirroredSettings(master)) {
            return false;
        }

        var settingsTag = new CompoundTag();
        master.getLogic().getConfigManager().writeToNBT(settingsTag);
        // PUSH_DIRECTION blockstate property not available in AE2 12.9.12
        CustomNameUtil.setCustomName(settingsTag, master.getCustomInventoryName());

        super.importSettings(SettingsFrom.MEMORY_CARD, settingsTag, null);
        this.getLogic().getConfigManager().readFromNBT(settingsTag);

        if (this.getPriority() != master.getPriority()) {
            this.setPriority(master.getPriority());
        }

        // Sync EAP smart features (advanced blocking, smart doubling, per-provider limit)
        var mirrorLogic = this.getLogic();
        var masterLogic = master.getLogic();
        if (mirrorLogic instanceof IAdvancedBlocking mirrorAB && masterLogic instanceof IAdvancedBlocking masterAB) {
            mirrorAB.eap$setAdvancedBlocking(masterAB.eap$getAdvancedBlocking());
        }
        if (mirrorLogic instanceof ISmartDoublingHolder mirrorSD && masterLogic instanceof ISmartDoublingHolder masterSD) {
            mirrorSD.eap$setSmartDoubling(masterSD.eap$getSmartDoubling());
            mirrorSD.eap$setProviderSmartDoublingLimit(masterSD.eap$getProviderSmartDoublingLimit());
        }

        return true;
    }

    private boolean resetMirroredSettingsToInitialState() {
        var defaultState = this.getBlockState().getBlock().defaultBlockState();
        var defaultMirror = new MirrorPatternProviderBlockEntity(this.getBlockPos(), defaultState);
        return this.syncMirroredSettings(defaultMirror);
    }

    private boolean hasDifferentMirroredSettings(PatternProviderBlockEntity master) {
        var mirrorLogic = this.getLogic();
        var masterLogic = master.getLogic();

        if (!Objects.equals(this.getCustomInventoryName(), master.getCustomInventoryName())
                || this.getPriority() != master.getPriority()
                || mirrorLogic.getConfigManager().getSetting(Settings.BLOCKING_MODE)
                != masterLogic.getConfigManager().getSetting(Settings.BLOCKING_MODE)
                || mirrorLogic.getConfigManager().getSetting(Settings.PATTERN_ACCESS_TERMINAL)
                != masterLogic.getConfigManager().getSetting(Settings.PATTERN_ACCESS_TERMINAL)
                || mirrorLogic.getConfigManager().getSetting(Settings.LOCK_CRAFTING_MODE)
                != masterLogic.getConfigManager().getSetting(Settings.LOCK_CRAFTING_MODE)) {
            return true;
        }

        // Check EAP smart features
        if (mirrorLogic instanceof IAdvancedBlocking mirrorAB && masterLogic instanceof IAdvancedBlocking masterAB) {
            if (mirrorAB.eap$getAdvancedBlocking() != masterAB.eap$getAdvancedBlocking()) {
                return true;
            }
        }
        if (mirrorLogic instanceof ISmartDoublingHolder mirrorSD && masterLogic instanceof ISmartDoublingHolder masterSD) {
            if (mirrorSD.eap$getSmartDoubling() != masterSD.eap$getSmartDoubling()
                    || mirrorSD.eap$getProviderSmartDoublingLimit() != masterSD.eap$getProviderSmartDoublingLimit()) {
                return true;
            }
        }

        return false;
    }

    private boolean syncMirroredPatterns(PatternProviderBlockEntity master, boolean forceSync) {
        var masterPatternVersion = getPatternSyncVersion(master);

        // Providers without version tracking require fallback compare; throttle this path to
        // avoid frequent deep NBT equals on every stable sync cycle.
        if (!forceSync && masterPatternVersion == UNKNOWN_PATTERN_SYNC_VERSION
                && this.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameTime() < this.nextUnknownVersionPatternCompareTick) {
                return false;
            }
            this.nextUnknownVersionPatternCompareTick =
                    serverLevel.getGameTime() + UNKNOWN_VERSION_PATTERN_COMPARE_INTERVAL;
        }

        if (!forceSync && masterPatternVersion != UNKNOWN_PATTERN_SYNC_VERSION
                && masterPatternVersion == this.lastSyncedPatternVersion) {
            return false;
        }

        var mirrorInventory = this.getPatternInventory();
        var masterInventory = asPatternInventory(master.getLogic().getPatternInv());
        var mirrorSize = mirrorInventory.size();
        var masterSize = masterInventory.size();
        var changed = false;

        for (int slot = 0; slot < mirrorSize; slot++) {
            var desiredStack = slot < masterSize ? masterInventory.getStackInSlot(slot) : ItemStack.EMPTY;
            var currentStack = mirrorInventory.getStackInSlot(slot);
            if (!sameStack(desiredStack, currentStack)) {
                mirrorInventory.setItemDirect(slot, desiredStack.isEmpty() ? ItemStack.EMPTY : desiredStack.copy());
                changed = true;
            }
        }

        if (changed) {
            this.getLogic().updatePatterns();
        }

        if (masterPatternVersion != UNKNOWN_PATTERN_SYNC_VERSION) {
            this.lastSyncedPatternVersion = masterPatternVersion;
            this.nextUnknownVersionPatternCompareTick = Long.MIN_VALUE;
        } else if (changed) {
            this.invalidatePatternSyncState();
        }

        return changed;
    }

    private boolean clearMirroredPatterns() {
        var patternInventory = this.getPatternInventory();
        var changed = false;

        for (int slot = 0; slot < patternInventory.size(); slot++) {
            if (!patternInventory.getStackInSlot(slot).isEmpty()) {
                patternInventory.setItemDirect(slot, ItemStack.EMPTY);
                changed = true;
            }
        }

        if (changed) {
            this.getLogic().updatePatterns();
        }

        return changed;
    }

    private AppEngInternalInventory getPatternInventory() {
        return ((MirrorLogic) this.getLogic()).getActualPatternInventory();
    }

    private ItemStack[] copyInventoryContents(AppEngInternalInventory inventory) {
        var contents = new ItemStack[inventory.size()];
        for (int slot = 0; slot < inventory.size(); slot++) {
            contents[slot] = inventory.getStackInSlot(slot).copy();
        }
        return contents;
    }

    private void restoreInventoryContents(AppEngInternalInventory inventory, ItemStack[] contents) {
        this.clearInventory(inventory);
        for (int slot = 0; slot < inventory.size(); slot++) {
            inventory.setItemDirect(slot, contents[slot].copy());
        }
    }

    private void clearInventory(AppEngInternalInventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            inventory.setItemDirect(slot, ItemStack.EMPTY);
        }
    }

    private void flushStateChanges() {
        this.saveChanges();
        this.markForUpdate();
    }

    private void scheduleImmediateSync() {
        this.nextSyncTick = Long.MIN_VALUE;
        this.nextUnknownVersionPatternCompareTick = Long.MIN_VALUE;
    }

    private void invalidatePatternSyncState() {
        this.lastSyncedPatternVersion = UNKNOWN_PATTERN_SYNC_VERSION;
        this.nextUnknownVersionPatternCompareTick = Long.MIN_VALUE;
    }

    @Nullable
    private ServerLevel getMasterLevel(ServerLevel serverLevel) {
        if (serverLevel.dimension() == this.masterDimension) {
            return serverLevel;
        }
        return serverLevel.getServer().getLevel(this.masterDimension);
    }

    private static AppEngInternalInventory asPatternInventory(Object inventory) {
        return (AppEngInternalInventory) inventory;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Property<Direction> findDirectionProperty(BlockState state, String propertyName) {
        for (var property : state.getProperties()) {
            if (property.getValueClass() == Direction.class && property.getName().equals(propertyName)) {
                return (Property<Direction>) property;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Direction getDirectionValue(BlockState state, Property<?> property) {
        if (property.getValueClass() != Direction.class) {
            return null;
        }
        return state.getValue((Property<Direction>) property);
    }

    private static BlockState setDirectionValue(BlockState state, Property<Direction> property, Direction direction) {
        return state.setValue(property, direction);
    }

    private static boolean copyDirectionField(Object source, Object target, String fieldName) {
        try {
            Field sourceField = findField(source.getClass(), fieldName);
            Field targetField = findField(target.getClass(), fieldName);
            if (sourceField == null || targetField == null || sourceField.getType() != Direction.class
                    || targetField.getType() != Direction.class) {
                return false;
            }

            sourceField.setAccessible(true);
            targetField.setAccessible(true);
            Direction sourceValue = (Direction) sourceField.get(source);
            Direction targetValue = (Direction) targetField.get(target);
            if (sourceValue == targetValue) {
                return false;
            }

            targetField.set(target, sourceValue);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean copyBooleanField(Object source, Object target, String fieldName) {
        try {
            Field sourceField = findField(source.getClass(), fieldName);
            Field targetField = findField(target.getClass(), fieldName);
            if (sourceField == null || targetField == null || sourceField.getType() != boolean.class
                    || targetField.getType() != boolean.class) {
                return false;
            }

            sourceField.setAccessible(true);
            targetField.setAccessible(true);
            boolean sourceValue = sourceField.getBoolean(source);
            boolean targetValue = targetField.getBoolean(target);
            if (sourceValue == targetValue) {
                return false;
            }

            targetField.setBoolean(target, sourceValue);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static Field findField(Class<?> owner, String fieldName) {
        Class<?> current = owner;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static int getMirrorPatternSlotCapacity() {
        int pageMultiplier = 1;
        if (ModConfig.INSTANCE != null) {
            pageMultiplier = ModConfig.INSTANCE.pageMultiplier;
        }

        pageMultiplier = Math.max(1, Math.min(64, pageMultiplier));
        return Math.max(AE2_PATTERN_SLOTS, EXTENDED_PATTERN_PROVIDER_BASE_SLOTS * pageMultiplier);
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        if (left == right) {
            return true;
        }

        if (left.isEmpty() && right.isEmpty()) {
            return true;
        }

        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }

        if (left.getItem() != right.getItem()) {
            return false;
        }

        if (left.getCount() != right.getCount()) {
            return false;
        }

        return ItemStack.tagMatches(left, right);
    }

    private static long getPatternSyncVersion(PatternProviderBlockEntity master) {
        if (master.getLogic() instanceof PatternProviderLogicSyncBridge bridge) {
            return bridge.eap$getPatternSyncVersion();
        }

        return UNKNOWN_PATTERN_SYNC_VERSION;
    }

    public static boolean isSupportedMaster(@Nullable BlockEntity blockEntity) {
        return blockEntity instanceof PatternProviderBlockEntity
                && !(blockEntity instanceof MirrorPatternProviderBlockEntity)
                && !blockEntity.isRemoved();
    }

    private static boolean isValidMaster(@Nullable BlockEntity blockEntity) {
        return isSupportedMaster(blockEntity);
    }

    private static final class MirrorLogic extends PatternProviderLogic {
        private MirrorLogic(IManagedGridNode mainNode, MirrorPatternProviderBlockEntity mirrorHost,
                int patternInventorySize) {
            super(mainNode, mirrorHost, patternInventorySize);
        }

        @Override
        public InternalInventory getPatternInv() {
            return DISABLED_PATTERN_INVENTORY;
        }

        private AppEngInternalInventory getActualPatternInventory() {
            return (AppEngInternalInventory) super.getPatternInv();
        }
    }
}
