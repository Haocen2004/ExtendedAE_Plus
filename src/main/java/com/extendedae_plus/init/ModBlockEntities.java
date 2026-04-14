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

    // Assembler Matrix block entities
    public static final RegistryObject<BlockEntityType<com.extendedae_plus.content.matrix.entity.AssemblerMatrixFrameEntity>> ASSEMBLER_MATRIX_FRAME =
            BLOCK_ENTITY_TYPES.register("assembler_matrix_frame",
                    () -> BlockEntityType.Builder.of(com.extendedae_plus.content.matrix.entity.AssemblerMatrixFrameEntity::new,
                            ModBlocks.ASSEMBLER_MATRIX_FRAME.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.extendedae_plus.content.matrix.entity.AssemblerMatrixWallEntity>> ASSEMBLER_MATRIX_WALL =
            BLOCK_ENTITY_TYPES.register("assembler_matrix_wall",
                    () -> BlockEntityType.Builder.of(com.extendedae_plus.content.matrix.entity.AssemblerMatrixWallEntity::new,
                            ModBlocks.ASSEMBLER_MATRIX_WALL.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.extendedae_plus.content.matrix.entity.AssemblerMatrixGlassEntity>> ASSEMBLER_MATRIX_GLASS =
            BLOCK_ENTITY_TYPES.register("assembler_matrix_glass",
                    () -> BlockEntityType.Builder.of(com.extendedae_plus.content.matrix.entity.AssemblerMatrixGlassEntity::new,
                            ModBlocks.ASSEMBLER_MATRIX_GLASS.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.extendedae_plus.content.matrix.entity.AssemblerMatrixPatternEntity>> ASSEMBLER_MATRIX_PATTERN =
            BLOCK_ENTITY_TYPES.register("assembler_matrix_pattern",
                    () -> BlockEntityType.Builder.of(com.extendedae_plus.content.matrix.entity.AssemblerMatrixPatternEntity::new,
                            ModBlocks.ASSEMBLER_MATRIX_PATTERN.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.extendedae_plus.content.matrix.entity.AssemblerMatrixCrafterEntity>> ASSEMBLER_MATRIX_CRAFTER =
            BLOCK_ENTITY_TYPES.register("assembler_matrix_crafter",
                    () -> BlockEntityType.Builder.of(com.extendedae_plus.content.matrix.entity.AssemblerMatrixCrafterEntity::new,
                            ModBlocks.ASSEMBLER_MATRIX_CRAFTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.extendedae_plus.content.matrix.entity.AssemblerMatrixSpeedEntity>> ASSEMBLER_MATRIX_SPEED =
            BLOCK_ENTITY_TYPES.register("assembler_matrix_speed",
                    () -> BlockEntityType.Builder.of(com.extendedae_plus.content.matrix.entity.AssemblerMatrixSpeedEntity::new,
                            ModBlocks.ASSEMBLER_MATRIX_SPEED.get()).build(null));
}
