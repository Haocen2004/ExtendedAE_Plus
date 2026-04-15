package com.extendedae_plus.content.quantum;

import appeng.block.crafting.ICraftingUnitType;
import com.extendedae_plus.config.ModConfig;
import com.extendedae_plus.init.ModBlocks;
import net.minecraft.world.item.Item;

/**
 * Enum for all Quantum Computer multiblock block types.
 * Backported from AdvancedAE's AAECraftingUnitType.
 */
public enum QuantumCraftingUnitType implements ICraftingUnitType {
    QUANTUM_UNIT(0, "quantum_unit"),
    QUANTUM_CORE(256, "quantum_core"),
    STORAGE_128M(128, "quantum_storage_128m"),
    STORAGE_256M(256, "quantum_storage_256m"),
    DATA_ENTANGLER(0, "quantum_data_entangler"),
    QUANTUM_ACCELERATOR(0, "quantum_accelerator"),
    MULTI_THREADER(0, "quantum_multi_threader"),
    STRUCTURE(0, "quantum_structure");

    private final int storageMb;
    private final String registryName;

    QuantumCraftingUnitType(int storageMb, String registryName) {
        this.storageMb = storageMb;
        this.registryName = registryName;
    }

    @Override
    public long getStorageBytes() {
        return 1024L * 1024 * storageMb;
    }

    public int getStorageMultiplier() {
        return this == DATA_ENTANGLER ? ModConfig.getQuantumDataEntanglerMultiplication() : 0;
    }

    @Override
    public int getAcceleratorThreads() {
        return switch (this) {
            case QUANTUM_ACCELERATOR, QUANTUM_CORE -> ModConfig.getQuantumAcceleratorThreads();
            default -> 0;
        };
    }

    public int getAccelerationMultiplier() {
        return this == MULTI_THREADER ? ModConfig.getQuantumMultiThreaderMultiplication() : 0;
    }

    public String getRegistryName() {
        return this.registryName;
    }

    @Override
    public Item getItemFromType() {
        return switch (this) {
            case QUANTUM_UNIT -> ModBlocks.QUANTUM_UNIT.get().asItem();
            case QUANTUM_CORE -> ModBlocks.QUANTUM_CORE.get().asItem();
            case STORAGE_128M -> ModBlocks.QUANTUM_STORAGE_128M.get().asItem();
            case STORAGE_256M -> ModBlocks.QUANTUM_STORAGE_256M.get().asItem();
            case DATA_ENTANGLER -> ModBlocks.QUANTUM_DATA_ENTANGLER.get().asItem();
            case QUANTUM_ACCELERATOR -> ModBlocks.QUANTUM_ACCELERATOR.get().asItem();
            case MULTI_THREADER -> ModBlocks.QUANTUM_MULTI_THREADER.get().asItem();
            case STRUCTURE -> ModBlocks.QUANTUM_STRUCTURE.get().asItem();
        };
    }
}
