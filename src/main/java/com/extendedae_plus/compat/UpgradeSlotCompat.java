package com.extendedae_plus.compat;

import appeng.api.upgrades.IUpgradeInventory;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 升级卡槽兼容性管理类
 * 统一管理：
 * 1. 是否由我们自己提供升级槽
 * 2. appflux存在时是否复用其升级槽
 * 3. appflux PatternProviderLogic 的反射访问入口
 */
public final class UpgradeSlotCompat {
    private static final String APPFLUX_MOD_ID = "appflux";
    private static final int LOCAL_PATTERN_PROVIDER_UPGRADE_SLOTS = 2;
    private static final int APPFLUX_PATTERN_PROVIDER_UPGRADE_SLOTS = 2;
    private static final String[] APPFLUX_UPGRADES_FIELD_NAMES = { "af_upgrades", "af_$upgrades" };
    private static final String[] APPFLUX_UPGRADES_CHANGED_METHOD_NAMES = { "af_onUpgradesChanged", "af_$onUpgradesChanged" };

    private static Field patternProviderAppfluxUpgradesField;
    private static boolean patternProviderAppfluxUpgradesFieldResolved;
    private static Method patternProviderAppfluxUpgradesChangedMethod;
    private static boolean patternProviderAppfluxUpgradesChangedMethodResolved;

    private UpgradeSlotCompat() {
    }

    /**
     * 检测Applied Flux模组是否存在
     * @return true如果存在，false如果不存在
     */
    public static boolean isAppfluxPresent() {
        return ModList.get().isLoaded(APPFLUX_MOD_ID);
    }

    /**
     * 是否由我们自己提供升级槽实现。
     */
    public static boolean usesDedicatedUpgradeSlots() {
        return !isAppfluxPresent();
    }

    /**
     * 是否应当复用 appflux 注入到 PatternProviderLogic 上的升级槽。
     */
    public static boolean usesAppfluxUpgradeSlots() {
        return isAppfluxPresent();
    }

    /**
     * 检测是否应该启用我们的升级卡槽功能
     * @return true如果应该启用，false如果检测到appflux模组存在
     */
    public static boolean shouldEnableUpgradeSlots() {
        return usesDedicatedUpgradeSlots();
    }

    /**
     * 是否需要持久化和管理我们本地创建的升级槽。
     */
    public static boolean shouldManageLocalUpgradeInventory() {
        return usesDedicatedUpgradeSlots();
    }

    /**
     * 检测是否应该启用频道卡功能
     * 频道卡是我们独有的功能，即使appflux存在也应该启用
     * @return 总是返回true，因为频道卡功能不与appflux冲突
     */
    public static boolean shouldEnableChannelCard() {
        return true; // 频道卡功能总是启用，因为appflux没有实现这个功能
    }

    /**
     * appflux 存在时，我们仍然需要监听其升级槽变化来驱动额外的兼容逻辑。
     */
    public static boolean shouldListenToAppfluxUpgrades() {
        return usesAppfluxUpgradeSlots();
    }

    /**
     * 检测是否应该在Screen中添加升级面板
     * @return true如果应该添加，false如果检测到appflux模组存在
     */
    public static boolean shouldAddUpgradePanelToScreen() {
        return usesDedicatedUpgradeSlots();
    }

    public static int getPatternProviderLocalUpgradeSlots() {
        return LOCAL_PATTERN_PROVIDER_UPGRADE_SLOTS;
    }

    public static int getPatternProviderAppfluxUpgradeSlots() {
        return APPFLUX_PATTERN_PROVIDER_UPGRADE_SLOTS;
    }

    public static IUpgradeInventory getPatternProviderAppfluxUpgrades(Object logicInstance) {
        Field field = resolvePatternProviderAppfluxUpgradesField(logicInstance.getClass());
        if (field == null) {
            return null;
        }

        try {
            Object value = field.get(logicInstance);
            return value instanceof IUpgradeInventory inventory ? inventory : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static boolean setPatternProviderAppfluxUpgrades(Object logicInstance, IUpgradeInventory inventory) {
        Field field = resolvePatternProviderAppfluxUpgradesField(logicInstance.getClass());
        if (field == null) {
            return false;
        }

        try {
            field.set(logicInstance, inventory);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    public static boolean invokePatternProviderAppfluxUpgradesChanged(Object logicInstance) throws ReflectiveOperationException {
        Method method = resolvePatternProviderAppfluxUpgradesChangedMethod(logicInstance.getClass());
        if (method == null) {
            return false;
        }

        method.invoke(logicInstance);
        return true;
    }

    private static Field resolvePatternProviderAppfluxUpgradesField(Class<?> logicClass) {
        if (!patternProviderAppfluxUpgradesFieldResolved) {
            patternProviderAppfluxUpgradesField = findField(logicClass, APPFLUX_UPGRADES_FIELD_NAMES);
            patternProviderAppfluxUpgradesFieldResolved = true;
        }
        return patternProviderAppfluxUpgradesField;
    }

    private static Method resolvePatternProviderAppfluxUpgradesChangedMethod(Class<?> logicClass) {
        if (!patternProviderAppfluxUpgradesChangedMethodResolved) {
            patternProviderAppfluxUpgradesChangedMethod = findMethod(logicClass, APPFLUX_UPGRADES_CHANGED_METHOD_NAMES);
            patternProviderAppfluxUpgradesChangedMethodResolved = true;
        }
        return patternProviderAppfluxUpgradesChangedMethod;
    }

    private static Field findField(Class<?> owner, String[] candidates) {
        for (String candidate : candidates) {
            try {
                Field field = owner.getDeclaredField(candidate);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> owner, String[] candidates) {
        for (String candidate : candidates) {
            try {
                Method method = owner.getDeclaredMethod(candidate);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
