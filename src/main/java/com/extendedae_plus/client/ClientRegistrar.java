package com.extendedae_plus.client;

import appeng.client.render.crafting.CraftingCubeModel;
import appeng.init.client.InitScreens;
import com.extendedae_plus.ExtendedAEPlus;
import com.extendedae_plus.ae.menu.EntitySpeedTickerMenu;
import com.extendedae_plus.ae.screen.EntitySpeedTickerScreen;
import com.extendedae_plus.client.render.crafting.EPlusCraftingCubeModelProvider;
import com.extendedae_plus.client.screen.GlobalProviderModesScreen;
import com.extendedae_plus.client.screen.LabeledWirelessTransceiverScreen;
import com.extendedae_plus.content.crafting.EPlusCraftingUnitType;
import com.extendedae_plus.init.ModItems;
import com.extendedae_plus.init.ModMenuTypes;
import com.extendedae_plus.items.materials.EntitySpeedCardItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.fml.ModList;

/**
 * 客户端模型注册，将 formed 模型注册为内置模型。
 */
public final class ClientRegistrar {
    private ClientRegistrar() {}

    private static boolean REGISTERED = false;

    /**
     * 注册内置模型（formed 模型等）。可被 ModelEvent 或启动阶段直接调用。
     */
    public static void initBuiltInModels() {
        if (REGISTERED) return;
        REGISTERED = true;
        // 注册 Item property，用于根据 ItemStack 的 NBT exponent 切换模型
        ItemProperties.register(ModItems.ENTITY_SPEED_CARD.get(), ExtendedAEPlus.id("mult"),
                (stack, world, entity, seed) -> (float) EntitySpeedCardItem.readMultiplier(stack));
    }

    /**
     * 将菜单类型与对应的屏幕绑定。
     */
    public static void registerMenuScreens() {
        MenuScreens.register(ModMenuTypes.NETWORK_PATTERN_CONTROLLER.get(), GlobalProviderModesScreen::new);
        MenuScreens.register(ModMenuTypes.LABELED_WIRELESS_TRANSCEIVER.get(), LabeledWirelessTransceiverScreen::new);
    }

    /**
     * 注册由 AE2 InitScreens 所需的屏幕资源映射（用于内置 JSON 屏幕注册）
     */
    public static void registerInitScreens() {
        InitScreens.register(ModMenuTypes.ENTITY_TICKER_MENU.get(),
                EntitySpeedTickerScreen<EntitySpeedTickerMenu>::new,
                "/screens/entity_speed_ticker.json");
    }

    /**
     * 仅客户端：在 Mods 菜单注册配置界面入口。
     * 将对 Screen 的引用限制在客户端侧，避免服务端类加载。
     */
//    public static void registerConfigScreen() {
//        // 将 ModConfigScreen 的引用放在此处，确保仅在 Dist.CLIENT 下解析该类
//        ModLoadingContext.get().registerExtensionPoint(
//                ConfigScreenHandler.ConfigScreenFactory.class,
//                () -> new ConfigScreenHandler.ConfigScreenFactory(
//                        (mc, parent) -> new ModConfigScreen(parent))
//        );
//    }

    /**
     * 注册形成态合成加速器的 IGeometryLoader（与 AE2 12.9.12 相同做法）。
     * 每个 loader 忽略 JSON 内容，直接返回对应的 CraftingCubeModel。
     */
    public static void registerCraftingCubeGeometryLoaders(ModelEvent.RegisterGeometryLoaders evt) {
        registerFormedLoader(evt, "block/crafting/4x_accelerator_formed_v2", EPlusCraftingUnitType.ACCELERATOR_4x);
        registerFormedLoader(evt, "block/crafting/16x_accelerator_formed_v2", EPlusCraftingUnitType.ACCELERATOR_16x);
        registerFormedLoader(evt, "block/crafting/64x_accelerator_formed_v2", EPlusCraftingUnitType.ACCELERATOR_64x);
        registerFormedLoader(evt, "block/crafting/256x_accelerator_formed_v2", EPlusCraftingUnitType.ACCELERATOR_256x);
        registerFormedLoader(evt, "block/crafting/1024x_accelerator_formed_v2", EPlusCraftingUnitType.ACCELERATOR_1024x);
    }

    private static void registerFormedLoader(ModelEvent.RegisterGeometryLoaders evt, String name, EPlusCraftingUnitType type) {
        IGeometryLoader<CraftingCubeModel> loader = (json, ctx) -> new CraftingCubeModel(new EPlusCraftingCubeModelProvider(type));
        evt.register(name, loader);
    }
}
