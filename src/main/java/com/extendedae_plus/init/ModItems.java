package com.extendedae_plus.init;

import appeng.api.parts.IPart;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartModelsHelper;
import com.extendedae_plus.ExtendedAEPlus;
import com.extendedae_plus.ae.parts.EntitySpeedTickerPart;
import com.extendedae_plus.items.BasicCoreItem;
import com.extendedae_plus.items.EntitySpeedTickerPartItem;
import com.extendedae_plus.items.InfinityBigIntegerCellItem;
import com.extendedae_plus.items.materials.ChannelCardItem;
import com.extendedae_plus.items.materials.EntitySpeedCardItem;
import com.extendedae_plus.items.materials.VirtualCraftingCardItem;
import com.extendedae_plus.items.tools.MirrorPatternBindingToolItem;
import com.extendedae_plus.util.ModCheckUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ExtendedAEPlus.MODID);

    public static final RegistryObject<Item> WIRELESS_TRANSCEIVER = ITEMS.register(
            "wireless_transceiver",
            () -> new BlockItem(ModBlocks.WIRELESS_TRANSCEIVER.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<Item> LABELED_WIRELESS_TRANSCEIVER = ITEMS.register(
            "labeled_wireless_transceiver",
            () -> new BlockItem(ModBlocks.LABELED_WIRELESS_TRANSCEIVER.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<Item> NETWORK_PATTERN_CONTROLLER = ITEMS.register(
            "network_pattern_controller",
            () -> new BlockItem(ModBlocks.NETWORK_PATTERN_CONTROLLER.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    // Crafting Accelerators
    public static final RegistryObject<Item> CRAFTING_ACCELERATOR_4x = ITEMS.register(
            "4x_crafting_accelerator",
            () -> new BlockItem(ModBlocks.CRAFTING_ACCELERATOR_4x.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<Item> CRAFTING_ACCELERATOR_16x = ITEMS.register(
            "16x_crafting_accelerator",
            () -> new BlockItem(ModBlocks.CRAFTING_ACCELERATOR_16x.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<Item> CRAFTING_ACCELERATOR_64x = ITEMS.register(
            "64x_crafting_accelerator",
            () -> new BlockItem(ModBlocks.CRAFTING_ACCELERATOR_64x.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<Item> CRAFTING_ACCELERATOR_256x = ITEMS.register(
            "256x_crafting_accelerator",
            () -> new BlockItem(ModBlocks.CRAFTING_ACCELERATOR_256x.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<Item> CRAFTING_ACCELERATOR_1024x = ITEMS.register(
            "1024x_crafting_accelerator",
            () -> new BlockItem(ModBlocks.CRAFTING_ACCELERATOR_1024x.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<Item> MIRROR_PATTERN_PROVIDER = ITEMS.register(
            "mirror_pattern_provider",
            () -> new BlockItem(ModBlocks.MIRROR_PATTERN_PROVIDER.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    // Assembler Matrix block items
    public static final RegistryObject<Item> ASSEMBLER_MATRIX_FRAME = ITEMS.register(
            "assembler_matrix_frame",
            () -> new BlockItem(ModBlocks.ASSEMBLER_MATRIX_FRAME.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> ASSEMBLER_MATRIX_WALL = ITEMS.register(
            "assembler_matrix_wall",
            () -> new BlockItem(ModBlocks.ASSEMBLER_MATRIX_WALL.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> ASSEMBLER_MATRIX_GLASS = ITEMS.register(
            "assembler_matrix_glass",
            () -> new BlockItem(ModBlocks.ASSEMBLER_MATRIX_GLASS.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> ASSEMBLER_MATRIX_PATTERN = ITEMS.register(
            "assembler_matrix_pattern",
            () -> new BlockItem(ModBlocks.ASSEMBLER_MATRIX_PATTERN.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> ASSEMBLER_MATRIX_CRAFTER = ITEMS.register(
            "assembler_matrix_crafter",
            () -> new BlockItem(ModBlocks.ASSEMBLER_MATRIX_CRAFTER.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> ASSEMBLER_MATRIX_SPEED = ITEMS.register(
            "assembler_matrix_speed",
            () -> new BlockItem(ModBlocks.ASSEMBLER_MATRIX_SPEED.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    // Quantum Computer block items
    public static final RegistryObject<Item> QUANTUM_UNIT = ITEMS.register(
            "quantum_unit", () -> new BlockItem(ModBlocks.QUANTUM_UNIT.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> QUANTUM_CORE = ITEMS.register(
            "quantum_core", () -> new BlockItem(ModBlocks.QUANTUM_CORE.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> QUANTUM_STORAGE_128M = ITEMS.register(
            "quantum_storage_128m", () -> new BlockItem(ModBlocks.QUANTUM_STORAGE_128M.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> QUANTUM_STORAGE_256M = ITEMS.register(
            "quantum_storage_256m", () -> new BlockItem(ModBlocks.QUANTUM_STORAGE_256M.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> QUANTUM_DATA_ENTANGLER = ITEMS.register(
            "quantum_data_entangler", () -> new BlockItem(ModBlocks.QUANTUM_DATA_ENTANGLER.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> QUANTUM_ACCELERATOR = ITEMS.register(
            "quantum_accelerator", () -> new BlockItem(ModBlocks.QUANTUM_ACCELERATOR.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> QUANTUM_MULTI_THREADER = ITEMS.register(
            "quantum_multi_threader", () -> new BlockItem(ModBlocks.QUANTUM_MULTI_THREADER.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB)));
    public static final RegistryObject<Item> QUANTUM_STRUCTURE = ITEMS.register(
            "quantum_structure", () -> new BlockItem(ModBlocks.QUANTUM_STRUCTURE.get(), new Item.Properties().tab(ModCreativeTabs.MAIN_TAB)));

    public static final RegistryObject<MirrorPatternBindingToolItem> MIRROR_PATTERN_BINDING_TOOL = ITEMS.register(
            "mirror_pattern_binding_tool",
            () -> new MirrorPatternBindingToolItem(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<EntitySpeedTickerPartItem> ENTITY_TICKER_PART_ITEM = ITEMS.register(
            "entity_speed_ticker",
            () -> new EntitySpeedTickerPartItem(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    // AE Upgrade Cards: 实体加速卡（四个等级：x2,x4,x8,x16）
    // 单一实体加速卡 Item（不同等级由 ItemStack.nbt 存储）
    public static final RegistryObject<EntitySpeedCardItem> ENTITY_SPEED_CARD = ITEMS.register(
            "entity_speed_card",
            () -> new EntitySpeedCardItem(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<InfinityBigIntegerCellItem> INFINITY_BIGINTEGER_CELL = ITEMS.register(
            "infinity_biginteger_cell", () -> new InfinityBigIntegerCellItem(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    // 频道卡（作为 AE 升级卡使用）
    public static final RegistryObject<ChannelCardItem> CHANNEL_CARD = ITEMS.register(
            "channel_card",
            () -> new ChannelCardItem(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    // 虚拟合成卡
    public static final RegistryObject<VirtualCraftingCardItem> VIRTUAL_CRAFTING_CARD = ITEMS.register(
            "virtual_crafting_card",
            () -> new VirtualCraftingCardItem(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );

    public static final RegistryObject<BasicCoreItem> BASIC_CORE = ITEMS.register(
            "basic_core",
            () -> new BasicCoreItem(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> STORAGE_CORE = ITEMS.register(
            "storage_core",
            () -> new Item(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> SPATIAL_CORE = ITEMS.register(
            "spatial_core",
            () -> new Item(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> INFINITY_CORE = ITEMS.register(
            "infinity_core",
            () -> new Item(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> OBLIVION_SINGULARITY = ITEMS.register(
            "oblivion_singularity",
            () -> new Item(new Item.Properties().tab(ModCreativeTabs.MAIN_TAB))
    );
    public static final RegistryObject<Item> ENERGY_STORAGE_CORE;
    public static final RegistryObject<Item> QUANTUM_STORAGE_CORE;

    static {
        if (ModCheckUtils.isAppfluxLoading()) {
            ENERGY_STORAGE_CORE = ITEMS.register(
                    "energy_storage_core",
                    () -> new Item(new Item.Properties())
            );
        } else {
            ENERGY_STORAGE_CORE = null;
        }

        if (ModCheckUtils.isAAELoading()) {
            QUANTUM_STORAGE_CORE = ITEMS.register(
                    "quantum_storage_core",
                    () -> new Item(new Item.Properties())
            );
        } else {
            QUANTUM_STORAGE_CORE = null;
        }
    }

    private ModItems() {}

        private static boolean PART_MODELS_REGISTERED = false;

    /**
     * 为 PartItem 注册 AE2 部件模型。
     * 在客户端进行模型/几何体注册时调用。
     */
    public static void registerPartModels() {
        if (PART_MODELS_REGISTERED) {
            return;
        }
        PART_MODELS_REGISTERED = true;
        PartModels.registerModels(
                PartModelsHelper.createModels(
                                                EntitySpeedTickerPart.class.asSubclass(IPart.class)
                )
        );
    }

    /**
     * 工厂：创建带 multiplier 的实体加速卡 ItemStack（2/4/8/16）
     */
    public static ItemStack createEntitySpeedCardStack(int multiplier) {
        return EntitySpeedCardItem.withMultiplier(multiplier);
    }
}
