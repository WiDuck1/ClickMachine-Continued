package dev.shadowsoffire.clickmachine.data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import dev.shadowsoffire.clickmachine.ClickMachine;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.neoforge.common.Tags;

public class ClickRecipes extends RecipeProvider {
    private ClickRecipes(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        var recipe = new ShapedRecipe(
            RecipeBuilder.createCraftingCommonInfo(true),
            RecipeBuilder.createCraftingBookInfo(RecipeCategory.MISC, ClickMachine.MODID),
            ShapedRecipePattern.of(Map.of(
                'D', Ingredient.of(Items.DIORITE),
                'C', Ingredient.of(Items.CHORUS_FLOWER),
                'S', Ingredient.of(this.items.getOrThrow(Tags.Items.STORAGE_BLOCKS_REDSTONE))),
                List.of("DDD", "DCD", "DSD")),
            new ItemStackTemplate(ClickMachine.CLICK_MACHINE_ITEM.get(), 1));
        this.output.accept(ResourceKey.create(Registries.RECIPE, ClickMachine.loc("click_machine")), recipe, null);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) { super(output, registries); }
        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ClickRecipes(registries, output);
        }
        @Override
        public String getName() { return "Click Machine recipes"; }
    }
}
