package dev.shadowsoffire.clickmachine.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class ClickMachineDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        var pack = generator.createPack();
        pack.addProvider(ClickRecipes.Runner::new);
        pack.addProvider(LootProvider::new);
    }
}
