package com.extendedae_plus.config;

import com.extendedae_plus.ExtendedAEPlus;
import com.extendedae_plus.util.entitySpeed.ConfigParsingUtils;
import com.extendedae_plus.util.entitySpeed.PowerUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public final class ModConfig {

    private static final Logger LOGGER = LogManager.getLogger();
    public static ModConfig INSTANCE;

    // --- Spec & Values ---
    public static ForgeConfigSpec SPEC;

    private static ForgeConfigSpec.IntValue PAGE_MULTIPLIER;
    private static ForgeConfigSpec.IntValue CRAFTING_PAUSE_THRESHOLD;
    private static ForgeConfigSpec.DoubleValue WIRELESS_MAX_RANGE;
    private static ForgeConfigSpec.BooleanValue WIRELESS_CROSS_DIM_ENABLE;
    private static ForgeConfigSpec.DoubleValue WIRELESS_TRANSCEIVER_IDLE_POWER;
    private static ForgeConfigSpec.BooleanValue PROVIDER_ROUND_ROBIN_ENABLE;
    private static ForgeConfigSpec.IntValue SMART_SCALING_MAX_MULTIPLIER;
    private static ForgeConfigSpec.BooleanValue SHOW_ENCODER_PATTERN_PLAYER;
    private static ForgeConfigSpec.BooleanValue PATTERN_TERMINAL_SHOW_SLOTS_DEFAULT;
    private static ForgeConfigSpec.IntValue ENTITY_TICKER_COST;
    private static ForgeConfigSpec.BooleanValue PRIORITIZE_DISK_ENERGY;
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_TICKER_BLACK_LIST;
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_TICKER_MULTIPLIERS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("扩展样板供应器总槽位容量的倍率。",
                        "基础为36，每页仍显示36格，倍率会增加总页数/总容量。",
                        "建议范围 1-16")
                .push("general");

        PAGE_MULTIPLIER = builder
                .defineInRange("pageMultiplier", 1, 1, 64);

        CRAFTING_PAUSE_THRESHOLD = builder
                .comment("设置AE构建合成计划过程中的 wait/notify 次数，提升吞吐但会降低调度响应性")
                .defineInRange("craftingPauseThreshold", 100000, 100, Integer.MAX_VALUE);

        WIRELESS_MAX_RANGE = builder
                .comment("无线收发器最大连接距离（单位：方块）",
                        "从端与主端的直线距离需小于等于该值才会建立连接")
                .defineInRange("wirelessMaxRange", 256.0, 1.0, 4096.0);

        WIRELESS_CROSS_DIM_ENABLE = builder
                .comment("是否允许无线收发器跨维度建立连接",
                        "开启后，从端可连接到不同维度的主端（忽略距离限制）")
                .define("wirelessCrossDimEnable", true);

        WIRELESS_TRANSCEIVER_IDLE_POWER = builder
                .comment("无线收发器待机能耗",
                        "无线收发器的基础待机能耗（AE/t），同时作用于普通与标签无线收发器")
                .defineInRange("wirelessTransceiverIdlePower", 100.0, 0.0, Double.MAX_VALUE);

        PROVIDER_ROUND_ROBIN_ENABLE = builder
                .comment("智能倍增时是否对样板供应器轮询分配",
                        "仅多个供应器有相同样板时生效，开启后请求会均分到所有可用供应器，关闭则全部分配给单一供应器",
                        "注意：所有相关供应器需开启智能倍增，否则可能失效")
                .define("providerRoundRobinEnable", true);

        SMART_SCALING_MAX_MULTIPLIER = builder
                .comment("全局智能倍增的最大倍率限制（0 表示不限制）",
                        "此限制针对单次样板产出的倍增上限，用于控制一次推送的最大缩放规模",
                        "优先级低于样板自身的供应器限制")
                .defineInRange("smartScalingMaxMultiplier", 0, 0, Integer.MAX_VALUE);

        SHOW_ENCODER_PATTERN_PLAYER = builder
                .comment("是否显示样板编码玩家",
                        "开启后将在样板 HoverText 上添加样板的编码玩家")
                .define("showEncoderPatternPlayer", true);

        PATTERN_TERMINAL_SHOW_SLOTS_DEFAULT = builder
                .comment("样板终端默认是否显示槽位",
                        "影响进入界面时SlotsRow的默认可见性，仅影响客户端显示")
                .define("patternTerminalShowSlotsDefault", true);

        builder.pop();
        builder.comment("实体加速器相关配置").push("entityTicker");

        ENTITY_TICKER_COST = builder
                .comment("实体加速器能量消耗基础值")
                .defineInRange("entityTickerCost", 512, 0, Integer.MAX_VALUE);

        PRIORITIZE_DISK_ENERGY = builder
                .comment("是否优先从磁盘提取FE能量（仅当Applied Flux模组存在时生效）",
                        "开启后，将优先尝试从磁盘提取FE能量；反之优先消耗AE网络中的能量")
                .define("prioritizeDiskEnergy", true);

        ENTITY_TICKER_BLACK_LIST = builder
                .comment("实体加速器黑名单：匹配的方块将不会被加速。支持通配符/正则（例如：minecraft:*）",
                        "格式：全名或通配符/正则字符串，例如 'minecraft:chest'、'minecraft:*'、'modid:.*_fluid'")
                .defineList("entityTickerBlackList", Arrays.asList(), o -> o instanceof String);

        ENTITY_TICKER_MULTIPLIERS = builder
                .comment("额外消耗倍率配置：为某些方块设置额外能量倍率，格式 'modid:blockid multiplier'，例如 'minecraft:chest 2x'",
                        "支持通配符/正则匹配（例如 'minecraft:* 2x' 会对整个命名空间生效）。")
                .defineList("entityTickerMultipliers", Arrays.asList(), o -> o instanceof String);

        builder.pop();
        SPEC = builder.build();
    }

    // --- Public field accessors (keep field-style access for all existing callers) ---
    public int pageMultiplier = 1;
    public int craftingPauseThreshold = 100000;
    public double wirelessMaxRange = 256.0;
    public boolean wirelessCrossDimEnable = true;
    public double wirelessTransceiverIdlePower = 100.0;
    public boolean providerRoundRobinEnable = true;
    public int smartScalingMaxMultiplier = 0;
    public boolean showEncoderPatternPlayer = true;
    public boolean patternTerminalShowSlotsDefault = true;
    public int entityTickerCost = 512;
    public boolean prioritizeDiskEnergy = true;
    public String[] entityTickerBlackList = {};
    public String[] entityTickerMultipliers = {};

    public static void init() {
        ModLoadingContext.get().registerConfig(Type.COMMON, SPEC);
        INSTANCE = new ModConfig();
    }

    /** 从 ForgeConfigSpec 刷新字段值（在配置加载/重载事件中调用） */
    public static void refresh() {
        if (INSTANCE == null || !SPEC.isLoaded()) return;

        INSTANCE.pageMultiplier = PAGE_MULTIPLIER.get();
        INSTANCE.craftingPauseThreshold = CRAFTING_PAUSE_THRESHOLD.get();
        INSTANCE.wirelessMaxRange = WIRELESS_MAX_RANGE.get();
        INSTANCE.wirelessCrossDimEnable = WIRELESS_CROSS_DIM_ENABLE.get();
        INSTANCE.wirelessTransceiverIdlePower = WIRELESS_TRANSCEIVER_IDLE_POWER.get();
        INSTANCE.providerRoundRobinEnable = PROVIDER_ROUND_ROBIN_ENABLE.get();
        INSTANCE.smartScalingMaxMultiplier = SMART_SCALING_MAX_MULTIPLIER.get();
        INSTANCE.showEncoderPatternPlayer = SHOW_ENCODER_PATTERN_PLAYER.get();
        INSTANCE.patternTerminalShowSlotsDefault = PATTERN_TERMINAL_SHOW_SLOTS_DEFAULT.get();

        int oldCost = INSTANCE.entityTickerCost;
        INSTANCE.entityTickerCost = ENTITY_TICKER_COST.get();
        INSTANCE.prioritizeDiskEnergy = PRIORITIZE_DISK_ENERGY.get();

        List<? extends String> bl = ENTITY_TICKER_BLACK_LIST.get();
        INSTANCE.entityTickerBlackList = bl.toArray(new String[0]);

        List<? extends String> ml = ENTITY_TICKER_MULTIPLIERS.get();
        INSTANCE.entityTickerMultipliers = ml.toArray(new String[0]);

        // 触发缓存刷新
        if (oldCost != INSTANCE.entityTickerCost) {
            synchronized (PowerUtils.class) {
                PowerUtils.initializeCaches();
            }
        }
        synchronized (ConfigParsingUtils.class) {
            ConfigParsingUtils.reload();
        }

        LOGGER.debug("[ExtendedAE_Plus] Config refreshed.");
    }
}