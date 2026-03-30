package com.extendedae_plus.init;

import com.extendedae_plus.ExtendedAEPlus;
import com.extendedae_plus.content.ae2.MirrorPatternProviderBlockEntity;
import com.extendedae_plus.content.wireless.LabeledWirelessTransceiverBlockEntity;
import com.extendedae_plus.content.wireless.WirelessTransceiverBlockEntity;
import com.extendedae_plus.content.controller.NetworkPatternControllerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ExtendedAEPlus.MODID);

    public static final RegistryObject<BlockEntityType<WirelessTransceiverBlockEntity>> WIRELESS_TRANSCEIVER_BE =
            BLOCK_ENTITY_TYPES.register("wireless_transceiver",
                    () -> BlockEntityType.Builder.of(WirelessTransceiverBlockEntity::new,
                            ModBlocks.WIRELESS_TRANSCEIVER.get()).build(null));

    public static final RegistryObject<BlockEntityType<LabeledWirelessTransceiverBlockEntity>> LABELED_WIRELESS_TRANSCEIVER_BE =
            BLOCK_ENTITY_TYPES.register("labeled_wireless_transceiver",
                    () -> BlockEntityType.Builder.of(LabeledWirelessTransceiverBlockEntity::new,
                            ModBlocks.LABELED_WIRELESS_TRANSCEIVER.get()).build(null));

    public static final RegistryObject<BlockEntityType<NetworkPatternControllerBlockEntity>> NETWORK_PATTERN_CONTROLLER_BE =
            BLOCK_ENTITY_TYPES.register("network_pattern_controller",
                    () -> BlockEntityType.Builder.of(NetworkPatternControllerBlockEntity::new,
                            ModBlocks.NETWORK_PATTERN_CONTROLLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MirrorPatternProviderBlockEntity>> MIRROR_PATTERN_PROVIDER_BE =
            BLOCK_ENTITY_TYPES.register("mirror_pattern_provider",
                    () -> BlockEntityType.Builder.of(MirrorPatternProviderBlockEntity::new,
                            ModBlocks.MIRROR_PATTERN_PROVIDER.get()).build(null));
}
