package dev.shadowsoffire.clickmachine.data;

import java.util.List;
import java.util.Map;
import dev.shadowsoffire.clickmachine.ClickMachine;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.neoforge.common.Tags;

public class ClickRecipes extends RecipeProvider {
    public ClickRecipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        super(recipes, advancements);
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

}
