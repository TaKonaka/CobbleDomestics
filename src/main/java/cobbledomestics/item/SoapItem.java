package cobbledomestics.item;

import net.minecraft.world.item.Item;

public class SoapItem extends Item {
	public static final int MAX_USES = 25;

	public SoapItem() {
		super(new Item.Properties().durability(MAX_USES).stacksTo(1));
	}
}
