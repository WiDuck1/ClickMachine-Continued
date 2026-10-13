package dev.shadowsoffire.clickmachine.data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import dev.shadowsoffire.clickmachine.ClickMachine;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.tags.TagKey;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;

public class ClickRecipes extends RecipeProvider {
    private ClickRecipes(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    public void buildRecipes() {
        var recipe = new ShapedRecipe(
            RecipeBuilder.createCraftingCommonInfo(true),
            RecipeBuilder.createCraftingBookInfo(RecipeCategory.MISC, ClickMachine.MODID),
            ShapedRecipePattern.of(Map.of(
                'D', Ingredient.of(Items.DIORITE),
                'C', Ingredient.of(Items.CHORUS_FLOWER),
                'S', this.tag(TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "storage_blocks/redstone")))),
                List.of("DDD", "DCD", "DSD")),
            new ItemStackTemplate(ClickMachine.CLICK_MACHINE_ITEM.get(), 1));
        this.output.accept(ResourceKey.create(Registries.RECIPE, ClickMachine.loc("click_machine")), recipe, null);
    }

    public static class Runner extends FabricRecipeProvider {
        public Runner(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) { super(output, registries); }
        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ClickRecipes(registries, output);
        }
        @Override
        public String getName() { return "Click Machine recipes"; }
    }
}
