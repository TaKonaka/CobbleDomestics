package cobbledomestics.item;

import net.minecraft.world.item.Item;

public class ToallaItem extends Item {
	public static final int MAX_USES = 5;

	public ToallaItem() {
		super(new Item.Properties().durability(MAX_USES).stacksTo(1));
	}
}
