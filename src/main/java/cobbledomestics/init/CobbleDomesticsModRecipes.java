package cobbledomestics.init;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.recipe.CompactCraftingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CobbleDomesticsModRecipes {
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
			DeferredRegister.create(Registries.RECIPE_SERIALIZER, CobbleDomesticsMod.MODID);

	public static final DeferredHolder<RecipeSerializer<?>, CompactCraftingRecipe.Serializer> COMPACT_CRAFTING =
			SERIALIZERS.register("compact_crafting", CompactCraftingRecipe.Serializer::new);

	private CobbleDomesticsModRecipes() {
	}
}
