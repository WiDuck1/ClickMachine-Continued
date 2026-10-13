package dev.shadowsoffire.clickmachine.data;

import java.util.concurrent.CompletableFuture;
import dev.shadowsoffire.clickmachine.ClickMachine;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;

public class LootProvider extends FabricBlockLootSubProvider {
    public LootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }
    @Override
    public void generate() {
        var block = ClickMachine.CLICK_MACHINE.get();
        add(block, createSingleItemTable(block).setRandomSequence(ClickMachine.loc("blocks/click_machine")));
    }
}
