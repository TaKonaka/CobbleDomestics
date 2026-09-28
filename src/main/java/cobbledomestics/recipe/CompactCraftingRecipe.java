package cobbledomestics.recipe;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import cobbledomestics.init.CobbleDomesticsModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

/**
 * Shapeless within a fixed contiguous width×height region (default 2×2).
 * Relies on {@link CraftingInput} trimming to the occupied bounding box.
 */
public class CompactCraftingRecipe implements CraftingRecipe {
	final String group;
	final CraftingBookCategory category;
	final ItemStack result;
	final NonNullList<Ingredient> ingredients;
	final int width;
	final int height;

	public CompactCraftingRecipe(
			String group,
			CraftingBookCategory category,
			ItemStack result,
			NonNullList<Ingredient> ingredients,
			int width,
			int height) {
		this.group = group;
		this.category = category;
		this.result = result;
		this.ingredients = ingredients;
		this.width = width;
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return CobbleDomesticsModRecipes.COMPACT_CRAFTING.get();
	}

	@Override
	public String getGroup() {
		return group;
	}

	@Override
	public CraftingBookCategory category() {
		return category;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return ingredients;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		if (input.width() != width || input.height() != height) {
			return false;
		}
		if (input.size() != ingredients.size() || input.ingredientCount() != ingredients.size()) {
			return false;
		}
		List<ItemStack> nonEmpty = new ArrayList<>(ingredients.size());
		for (ItemStack stack : input.items()) {
			if (stack.isEmpty()) {
				return false;
			}
			nonEmpty.add(stack);
		}
		return RecipeMatcher.findMatches(nonEmpty, ingredients) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		return result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int gridWidth, int gridHeight) {
		return gridWidth >= width && gridHeight >= height;
	}

	public static class Serializer implements RecipeSerializer<CompactCraftingRecipe> {
		private static final MapCodec<CompactCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
				CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(r -> r.category),
				ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
				Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> {
					Ingredient[] array = list.toArray(Ingredient[]::new);
					if (array.length == 0) {
						return DataResult.error(() -> "No ingredients for compact crafting recipe");
					}
					return DataResult.success(NonNullList.of(Ingredient.EMPTY, array));
				}, DataResult::success).forGetter(r -> r.ingredients),
				Codec.INT.optionalFieldOf("width", 2).forGetter(r -> r.width),
				Codec.INT.optionalFieldOf("height", 2).forGetter(r -> r.height)
		).apply(instance, CompactCraftingRecipe::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, CompactCraftingRecipe> STREAM_CODEC = StreamCodec.of(
				Serializer::toNetwork, Serializer::fromNetwork);

		@Override
		public MapCodec<CompactCraftingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, CompactCraftingRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static CompactCraftingRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
			String group = buf.readUtf();
			CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
			int count = buf.readVarInt();
			NonNullList<Ingredient> ingredients = NonNullList.withSize(count, Ingredient.EMPTY);
			ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
			ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
			int width = buf.readVarInt();
			int height = buf.readVarInt();
			return new CompactCraftingRecipe(group, category, result, ingredients, width, height);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buf, CompactCraftingRecipe recipe) {
			buf.writeUtf(recipe.group);
			buf.writeEnum(recipe.category);
			buf.writeVarInt(recipe.ingredients.size());
			for (Ingredient ingredient : recipe.ingredients) {
				Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
			}
			ItemStack.STREAM_CODEC.encode(buf, recipe.result);
			buf.writeVarInt(recipe.width);
			buf.writeVarInt(recipe.height);
		}
	}
}
