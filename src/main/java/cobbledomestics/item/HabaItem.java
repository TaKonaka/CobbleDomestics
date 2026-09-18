package cobbledomestics.item;

import cobbledomestics.haba.HabaColor;
import cobbledomestics.haba.HabaTier;
import net.minecraft.world.item.Item;

public class HabaItem extends Item {
	private final HabaTier tier;
	private final HabaColor color;

	public HabaItem(HabaTier tier, HabaColor color) {
		super(new Item.Properties().stacksTo(64));
		this.tier = tier;
		this.color = color;
	}

	public HabaTier getTier() {
		return tier;
	}

	/** Null for Arcoíris (no type bonus). */
	public HabaColor getColor() {
		return color;
	}
}
