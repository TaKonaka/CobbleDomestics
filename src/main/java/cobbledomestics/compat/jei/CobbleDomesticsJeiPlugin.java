package cobbledomestics.compat.jei;

import java.util.List;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.recipe.CompactCraftingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class CobbleDomesticsJeiPlugin implements IModPlugin {
	private static final ResourceLocation UID =
			ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
		registration.getCraftingCategory().addExtension(CompactCraftingRecipe.class, new CompactCraftingExtension());
	}

	private static final class CompactCraftingExtension implements ICraftingCategoryExtension<CompactCraftingRecipe> {
		@Override
		public void setRecipe(
				RecipeHolder<CompactCraftingRecipe> recipeHolder,
				IRecipeLayoutBuilder builder,
				ICraftingGridHelper craftingGridHelper,
				IFocusGroup focuses) {
			CompactCraftingRecipe recipe = recipeHolder.value();
			ItemStack result = recipe.getResultItem(null);
			craftingGridHelper.createAndSetOutputs(builder, List.of(result));
			craftingGridHelper.createAndSetIngredients(
					builder,
					recipe.getIngredients(),
					recipe.getWidth(),
					recipe.getHeight());
		}

		@Override
		public int getWidth(RecipeHolder<CompactCraftingRecipe> recipeHolder) {
			return recipeHolder.value().getWidth();
		}

		@Override
		public int getHeight(RecipeHolder<CompactCraftingRecipe> recipeHolder) {
			return recipeHolder.value().getHeight();
		}
	}
}
