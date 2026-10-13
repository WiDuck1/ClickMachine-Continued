package dev.shadowsoffire.clickmachine.data;

import java.util.List;
import java.util.Set;

import dev.shadowsoffire.clickmachine.ClickMachine;
import net.minecraft.core.Holder;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class LootProvider extends LootTableProvider {

    public LootProvider() {
        super(Set.of(), List.of(new SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)));
    }

    public static class BlockLoot extends BlockLootSubProvider {

        public static final Set<Item> EXPLOSION_RESISTANT = Set.of();

        protected BlockLoot(LootTableSubProvider.Context context) {
            super(EXPLOSION_RESISTANT, FeatureFlags.REGISTRY.allFlags(), context);
        }

        @Override
        protected void generate() {
            this.dropSelf(ClickMachine.CLICK_MACHINE);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(ClickMachine.CLICK_MACHINE.get());
        }

        protected void dropSelf(Holder<Block> block) {
            this.dropSelf(block.value());
        }

    }
}
