package cobbledomestics.item;

import net.minecraft.world.item.Item;

public class PipetaItem extends Item {
	public static final int MAX_USES = 5;

	public PipetaItem() {
		super(new Item.Properties().durability(MAX_USES).stacksTo(1));
	}
}
