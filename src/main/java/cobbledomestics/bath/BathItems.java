package cobbledomestics.bath;

import cobbledomestics.init.CobbleDomesticsModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared bath-item checks for client immersive entry and server validation. */
public final class BathItems {
	private BathItems() {
	}

	/** Soap, towel, water bucket, or pipette — opens immersive care. */
	public static boolean isBathInteractItem(ItemStack stack) {
		return isSoap(stack)
				|| isTowel(stack)
				|| isWaterBucket(stack)
				|| isPipeta(stack);
	}

	public static boolean isSoap(ItemStack stack) {
		return stack.is(CobbleDomesticsModItems.SOAP.get());
	}

	public static boolean isTowel(ItemStack stack) {
		return stack.is(CobbleDomesticsModItems.TOALLA.get());
	}

	public static boolean isWaterBucket(ItemStack stack) {
		return stack.is(Items.WATER_BUCKET);
	}

	public static boolean isPipeta(ItemStack stack) {
		return stack.is(CobbleDomesticsModItems.PIPETA.get());
	}

	/** Soap, towel, or pipette — scrub tools. */
	public static boolean isScrubTool(ItemStack stack) {
		return isSoap(stack) || isTowel(stack) || isPipeta(stack);
	}
}
