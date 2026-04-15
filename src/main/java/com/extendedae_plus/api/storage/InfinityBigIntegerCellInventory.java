package com.extendedae_plus.api.storage;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.core.AELog;
import com.extendedae_plus.ExtendedAEPlus;
import com.extendedae_plus.items.InfinityBigIntegerCellItem;
import com.extendedae_plus.util.storage.InfinityConstants;
import com.extendedae_plus.util.storage.InfinityDataStorage;
import com.extendedae_plus.util.storage.InfinityStorageManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * This code is inspired by AE2Things[](https://github.com/Technici4n/AE2Things-Forge), licensed under the MIT License.<p>
 * Original copyright (c) Technici4n<p>
 */
public class InfinityBigIntegerCellInventory implements StorageCell {
    private final InfinityBigIntegerCellItem cell;
    // 磁盘本身
    private final ItemStack self;
    // AE2 提供的保存提供者，用于在容器中批量保存时触发回调
    private final ISaveProvider container;
    // 存储物品键和数量的映射
    private Object2ObjectMap<AEKey, BigInteger> AEKey2AmountsMap;
    // 存储的物品种类数量
    private int totalAEKeyType;
    // 存储的物品总数
    private final MutableBigCounter totalAEKey2Amounts = new MutableBigCounter();
    // 标记是否已持久化到 SavedData
    private boolean isPersisted = true;

    private static final BigInteger BI_ZERO = BigInteger.ZERO;
    private static final BigInteger BI_LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);


    public InfinityBigIntegerCellInventory(InfinityBigIntegerCellItem cell, ItemStack stack, ISaveProvider saveProvider) {
        // 保存存储单元类型（InfinityBigIntegerCellItem 实例），用于访问磁盘属性
        this.cell = cell;
        // 保存物品堆栈，表示磁盘本身，包含运行时的 NBT 数据
        this.self = stack;
        // 保存提供者，用于触发数据保存
        this.container = saveProvider;
        // 初始化 storedAmounts 为 null，延迟加载物品数据
        this.AEKey2AmountsMap = null;
        // 初始化磁盘数据
        initData();
    }

    // 将 BigInteger 格式化为带单位的字符串，保留两位小数
    public static String formatBigInteger(BigInteger number) {
        // 使用方法局部的 DecimalFormat，避免静态共享的非线程安全问题
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");
        BigDecimal bd = new BigDecimal(number);
        BigDecimal thousand = new BigDecimal(1000);
        String[] units = new String[]{"", "K", "M", "G", "T", "P", "E", "Z", "Y"};
        int idx = 0;
        while (bd.compareTo(thousand) >= 0 && idx < units.length - 1) {
            bd = bd.divide(thousand, 2, RoundingMode.HALF_UP);
            idx++;
        }
        if (idx == 0) {
            return bd.setScale(0, RoundingMode.DOWN).toPlainString();
        }
        return df.format(bd.doubleValue()) + units[idx];
    }

    // 获取磁盘的 InfinityDataStorage 数据
    private InfinityDataStorage getCellStorage() {
        // 如果磁盘有 UUID，返回对应的 InfinityDataStorage
        if (getUUID() != null) {
            return getStorageManagerInstance().getOrCreateCell(getUUID());
        } else {
            // 否则返回空的 InfinityDataStorage
            return InfinityDataStorage.EMPTY;
        }
    }

    // 初始化磁盘数据
    private void initData() {
        // 如果磁盘有 UUID，加载存储的物品数据
        if (hasUUID()) {
            InfinityDataStorage storage = getCellStorage();
            this.totalAEKeyType = storage.amounts.size();
            if (storage.itemCount.equals(BI_ZERO)) {
                this.totalAEKey2Amounts.setZero();
            } else {
                this.totalAEKey2Amounts.set(storage.itemCount);
            }

        } else {
            // 否则初始化为空
            this.totalAEKeyType = 0;
            this.totalAEKey2Amounts.setZero();
            // 加载物品数据
            getCellStoredMap();
        }
    }

    // 获取存储单元的状态（空、部分填充）
    @Override
    public CellState getStatus() {
        // 如果没有存储任何物品，返回空状态
        if (this.totalAEKey2Amounts.isZero()) {
            return CellState.EMPTY;
        }
        // 否则返回满状态
        return CellState.NOT_EMPTY;
    }

    // 获取存储单元的待机能耗
    @Override
    public double getIdleDrain() {
        return 512;
    }

    // 持久化存储单元数据到全局存储
    @Override
    public void persist() {
        if (this.isPersisted)
            return;

        if (totalAEKey2Amounts.isZero()) {
            if (hasUUID()) {
                getStorageManagerInstance().removeCell(getUUID());
                if (self.hasTag()) {
                    var tag = self.getTag();
                    // remove persisted identifiers and cached summary fields from the ItemStack
                    tag.remove(InfinityConstants.INFINITY_CELL_UUID);
                    tag.remove(InfinityConstants.INFINITY_ITEM_TOTAL);
                    tag.remove(InfinityConstants.INFINITY_ITEM_TYPES);
                    // backward compat: also remove internal cell item count key if present
                    tag.remove(InfinityConstants.INFINITY_CELL_ITEM_COUNT);
                }
                initData();
            }
            return;
        }

        // 创建物品键列表
        ListTag keys = new ListTag();
        // 创建物品数量列表
        ListTag amounts = new ListTag();
        // 直接使用缓存总量，避免在持久化阶段再次全量 BigInteger.add
        BigInteger itemCount = this.totalAEKey2Amounts.toBigInteger();

        for (var entry : this.AEKey2AmountsMap.object2ObjectEntrySet()) {
            BigInteger amount = entry.getValue();
            // 如果数量大于 0，添加到键和数量列表
            if (amount.compareTo(BI_ZERO) > 0) {
                keys.add(entry.getKey().toTagGeneric());
                CompoundTag amountTag = new CompoundTag();
                amountTag.putByteArray("value", amount.toByteArray());
                amounts.add(amountTag);
            }
        }

        if (keys.isEmpty()) {
            getStorageManagerInstance().updateCell(getUUID(), new InfinityDataStorage());
        } else {
            getStorageManagerInstance().modifyDisk(getUUID(), keys, amounts, itemCount);
        }

        // 更新存储的物品种类数量
        this.totalAEKeyType = this.AEKey2AmountsMap.size();
        // 重新写入缓存总量，保证与最终持久化值一致
        this.totalAEKey2Amounts.set(itemCount);
        // 将物品总数与种类数量存入物品堆栈的 NBT（用于快捷查看／tooltip），同时保留旧字段以兼容历史版本
        var tag = self.getOrCreateTag();
        tag.putByteArray(InfinityConstants.INFINITY_ITEM_TOTAL, itemCount.toByteArray());
        tag.putInt(InfinityConstants.INFINITY_ITEM_TYPES, this.totalAEKeyType);
        // backward compat storage field (kept for legacy readers)
        tag.putByteArray(InfinityConstants.INFINITY_CELL_ITEM_COUNT, itemCount.toByteArray());

        // 标记数据已持久化
        this.isPersisted = true;
    }

    // 获取存储单元的描述（此处返回null，可自定义）
    @Override
    public Component getDescription() {
        return null;
    }

    // 静态方法，创建存储单元库存
    public static InfinityBigIntegerCellInventory createInventory(ItemStack stack, ISaveProvider saveProvider) {
        // 检查物品堆栈是否为空
        Objects.requireNonNull(stack, "Cannot create cell inventory for null itemstack");
        // 检查物品是否为 IDISKCellItem 类型
        if (!(stack.getItem() instanceof InfinityBigIntegerCellItem cell)) {
            return null;
        }
        // 创建并返回新的 DISKCellInventory 实例
        return new InfinityBigIntegerCellInventory(cell, stack, saveProvider);
    }

    // 获取存储的物品总数
    public BigInteger getTotalAEKey2Amounts() {
        return this.totalAEKey2Amounts.toBigInteger();
    }

    // 获取存储的物品种类数量
    public int getTotalAEKeyType() {
        return this.totalAEKeyType;
    }

    // 判断物品堆栈是否有UUID
    public boolean hasUUID() {
        return self.hasTag() && self.getOrCreateTag().contains(InfinityConstants.INFINITY_CELL_UUID);
    }

    // 获取物品堆栈的UUID
    public UUID getUUID() {
        if (this.hasUUID()) {
            return self.getOrCreateTag().getUUID(InfinityConstants.INFINITY_CELL_UUID);
        } else {
            return null;
        }
    }

    // 获取或初始化存储映射
    private Object2ObjectMap<AEKey, BigInteger> getCellStoredMap() {
        if (AEKey2AmountsMap == null) {
            AEKey2AmountsMap = new Object2ObjectOpenHashMap<>(512, 0.6f);
            this.loadCellStoredMap();
        }
        return AEKey2AmountsMap;
    }

    // 获取所有可用的物品堆栈及其数量
    @Override
    public void getAvailableStacks(KeyCounter out) {
        var map = getCellStoredMap();
        if (map == null || map.isEmpty()) {
            return;
        }

        for (var entry : map.object2ObjectEntrySet()) {
            AEKey key = entry.getKey();
            BigInteger amount = entry.getValue();

            // 如果当前要添加的数量本身就超过 Long.MAX_VALUE，直接设为 MAX
            if (amount.compareTo(BI_LONG_MAX) > 0) {
                out.set(key, Long.MAX_VALUE);
                continue;
            }

            long addAmount = amount.longValue();
            long existing = out.get(key);

            // 如果已有值已是 MAX，直接跳过
            if (existing == Long.MAX_VALUE) {
                continue;
            }

            // 计算总和，防止溢出
            long sum = existing + addAmount;
            if (sum < 0 || sum < existing) { // 溢出检测
                out.set(key, Long.MAX_VALUE);
            } else {
                out.add(key, addAmount); // 安全添加
            }
        }
    }

    // 从存储中加载物品映射
    private void loadCellStoredMap() {
        boolean dataCorruption = false;
        if (!self.hasTag()) return;

        var keys = getCellStorage().keys;
        var amounts = getCellStorage().amounts;
        // 数据损坏
        if (keys.size() != amounts.size()) {
            AELog.warn("Loading storage cell with mismatched amounts/tags: %d != %d", amounts.size(), keys.size());
        }
        // 遍历数量和键，加载到 AEKey2AmountsMap
        for (int i = 0; i < amounts.size(); i++) {
            AEKey key = AEKey.fromTagGeneric(keys.getCompound(i));
            BigInteger amount = new BigInteger(amounts.getCompound(i).getByteArray("value"));
            // 检查数据是否损坏
            if (amount.compareTo(BI_ZERO) <= 0 || key == null) {
                dataCorruption = true;
            } else {
                AEKey2AmountsMap.put(key, amount);
            }
        }
        if (dataCorruption) {
            this.saveChangesWithRecount();
        }
    }

    // 获取全局存储实例
    private static InfinityStorageManager getStorageManagerInstance() {
        return ExtendedAEPlus.STORAGE_INSTANCE;
    }

    // 标记数据需要保存，并通知容器或直接持久化
    private void markDirty() {
        // 标记数据未持久化
        this.isPersisted = false;
        // 如果有保存提供者，通知保存
        if (this.container != null) {
            this.container.saveChanges();
        } else {
            // 否则立即持久化
            this.persist();
        }
    }

    // 全量重算总量（仅用于数据修复等低频路径）
    private void saveChangesWithRecount() {
        // 更新存储的物品种类数量
        this.totalAEKeyType = this.AEKey2AmountsMap.size();
        // 重置物品总数
        this.totalAEKey2Amounts.setZero();
        // 计算物品总数
        for (BigInteger AEKey2Amounts : this.AEKey2AmountsMap.values()) {
            this.totalAEKey2Amounts.add(AEKey2Amounts);
        }
        markDirty();
    }

    // 增量更新总量（高频插入/提取路径）
    private void saveChangesWithDelta(BigInteger amountDelta, int typeDelta) {
        this.totalAEKey2Amounts.add(amountDelta);
        this.totalAEKey2Amounts.clampToZero();

        if (typeDelta != 0) {
            this.totalAEKeyType = Math.max(0, this.totalAEKeyType + typeDelta);
        }

        markDirty();
    }

    // 使用减法可避免 amount.negate() 产生中间对象
    private void saveChangesWithSubtract(BigInteger amount, int typeDelta) {
        this.totalAEKey2Amounts.subtract(amount);
        this.totalAEKey2Amounts.clampToZero();

        if (typeDelta != 0) {
            this.totalAEKeyType = Math.max(0, this.totalAEKeyType + typeDelta);
        }

        markDirty();
    }

    // long 快路径，减少高频 BigInteger.valueOf 创建
    private void saveChangesWithDelta(long amountDelta, int typeDelta) {
        this.totalAEKey2Amounts.add(amountDelta);
        this.totalAEKey2Amounts.clampToZero();

        if (typeDelta != 0) {
            this.totalAEKeyType = Math.max(0, this.totalAEKeyType + typeDelta);
        }

        markDirty();
    }

    // 插入物品到存储单元
    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        // 数量为0或类型不匹配直接返回
        if (amount == 0){
            return 0;
        }
        // 不允许存储有物品的无限单元
        if (what instanceof AEItemKey itemKey &&
                itemKey.getItem() instanceof InfinityBigIntegerCellItem &&
                itemKey.hasTag()
        ) {
            return 0;
        }

        // 如果没有UUID，尝试在服务器端且存储管理器已就绪时生成UUID并初始化存储
        if (!this.hasUUID()) {
           self.getOrCreateTag().putUUID(InfinityConstants.INFINITY_CELL_UUID, UUID.randomUUID());
            getStorageManagerInstance().getOrCreateCell(getUUID());
            loadCellStoredMap();
        }
        // 获取当前物品数量
        BigInteger currentAmount = this.getCellStoredMap().getOrDefault(what, BI_ZERO);

        if (mode == Actionable.MODULATE) {
            // 实际插入，优先走 long 快路径避免高频大数加法开销
            BigInteger newAmount = addLongFast(currentAmount, amount);
            getCellStoredMap().put(what, newAmount);
            int typeDelta = currentAmount.signum() == 0 ? 1 : 0;
            this.saveChangesWithDelta(amount, typeDelta);
        }
        return amount;
    }

    // 从存储单元提取物品
    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        BigInteger currentAmount = this.getCellStoredMap().getOrDefault(what, BI_ZERO);
        // 如果有物品可提取
        if (currentAmount.compareTo(BI_ZERO) > 0) {
            // 如果提取数量大于等于当前数量
            boolean drainAll;
            if (currentAmount.bitLength() <= 63) {
                drainAll = amount >= currentAmount.longValue();
            } else {
                // currentAmount > Long.MAX_VALUE，requested(long) 不可能覆盖全部
                drainAll = false;
            }

            if (drainAll) {
                if (mode == Actionable.MODULATE) {
                    getCellStoredMap().remove(what);
                    this.saveChangesWithSubtract(currentAmount, -1);
                }
                return currentAmount.compareTo(BI_LONG_MAX) > 0 ? Long.MAX_VALUE : currentAmount.longValue();
            } else {
                // 提取部分数量
                if (mode == Actionable.MODULATE) {
                    getCellStoredMap().put(what, addLongFast(currentAmount, -amount));
                    this.saveChangesWithDelta(-amount, 0);
                }
                return amount;
            }
        }
        return 0;
    }

    private static BigInteger addLongFast(BigInteger base, long delta) {
        if (delta == 0L) {
            return base;
        }

        if (base.bitLength() <= 63) {
            long v = base.longValue();
            if (!willOverflow(v, delta)) {
                return BigInteger.valueOf(v + delta);
            }
        }

        return base.add(BigInteger.valueOf(delta));
    }

    private static boolean willOverflow(long a, long b) {
        long r = a + b;
        return ((a ^ r) & (b ^ r)) < 0;
    }

    // 获取存储单元内所有物品的总数量（格式化字符串）
    public String getTotalStorage() {
        // 使用缓存的 totalStored，避免每次全表扫描
        return formatBigInteger(totalAEKey2Amounts.toBigInteger());
    }

    /**
     * 内部可变计数器：对外仍暴露 BigInteger，内部优先走 long 快路径。
     */
    private static final class MutableBigCounter {
        private long fastValue;
        private BigInteger bigValue;

        void setZero() {
            this.fastValue = 0L;
            this.bigValue = null;
        }

        void set(BigInteger value) {
            if (value == null || value.signum() == 0) {
                setZero();
                return;
            }

            if (value.bitLength() <= 63) {
                this.fastValue = value.longValue();
                this.bigValue = null;
            } else {
                this.fastValue = 0L;
                this.bigValue = value;
            }
        }

        boolean isZero() {
            return this.bigValue == null ? this.fastValue == 0L : this.bigValue.signum() == 0;
        }

        void add(long delta) {
            if (delta == 0L) {
                return;
            }

            if (this.bigValue == null) {
                if (willOverflow(this.fastValue, delta)) {
                    this.bigValue = BigInteger.valueOf(this.fastValue).add(BigInteger.valueOf(delta));
                    this.fastValue = 0L;
                } else {
                    this.fastValue += delta;
                }
            } else {
                this.bigValue = this.bigValue.add(BigInteger.valueOf(delta));
            }
        }

        void add(BigInteger delta) {
            if (delta == null || delta.signum() == 0) {
                return;
            }

            if (this.bigValue == null) {
                this.bigValue = BigInteger.valueOf(this.fastValue).add(delta);
                this.fastValue = 0L;
            } else {
                this.bigValue = this.bigValue.add(delta);
            }

            if (this.bigValue.bitLength() <= 63) {
                this.fastValue = this.bigValue.longValue();
                this.bigValue = null;
            }
        }

        void subtract(BigInteger amount) {
            if (amount == null || amount.signum() == 0) {
                return;
            }

            if (this.bigValue == null) {
                if (amount.bitLength() <= 63) {
                    this.fastValue -= amount.longValue();
                } else {
                    this.bigValue = BigInteger.valueOf(this.fastValue).subtract(amount);
                    this.fastValue = 0L;
                }
            } else {
                this.bigValue = this.bigValue.subtract(amount);
            }
        }

        void clampToZero() {
            if (this.bigValue == null) {
                if (this.fastValue < 0L) {
                    this.fastValue = 0L;
                }
                return;
            }

            if (this.bigValue.signum() < 0) {
                setZero();
            } else if (this.bigValue.bitLength() <= 63) {
                this.fastValue = this.bigValue.longValue();
                this.bigValue = null;
            }
        }

        BigInteger toBigInteger() {
            return this.bigValue == null ? BigInteger.valueOf(this.fastValue) : this.bigValue;
        }

        private static boolean willOverflow(long a, long b) {
            long r = a + b;
            return ((a ^ r) & (b ^ r)) < 0;
        }
    }
}
