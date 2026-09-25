package cobbledomestics.item;

import cobbledomestics.block.SoapBlock;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Placeable candle-like soap that is also edible (poison + nutrition).
 */
public class SoapItem extends BlockItem {
	public SoapItem(Block block) {
		super(block, new Item.Properties()
				.stacksTo(64)
				.food(new FoodProperties.Builder()
						.nutrition(2)
						.saturationModifier(0.1F)
						.effect(() -> new MobEffectInstance(MobEffects.POISON, 20 * 20, 0), 1.0F)
						.alwaysEdible()
						.build()));
	}

	/** Convenience for registration when the block is already known. */
	public static SoapItem create(SoapBlock block) {
		return new SoapItem(block);
	}
}
