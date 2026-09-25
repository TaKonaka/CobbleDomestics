package cobbledomestics.init;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.haba.HabaColor;
import cobbledomestics.haba.HabaTier;
import cobbledomestics.item.HabaItem;
import cobbledomestics.item.PipetaItem;
import cobbledomestics.item.SoapItem;
import cobbledomestics.item.ToallaItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CobbleDomesticsModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(CobbleDomesticsMod.MODID);

	public static final DeferredItem<Item> SOAP = REGISTRY.register("soap",
			() -> SoapItem.create(CobbleDomesticsModBlocks.SOAP_BLOCK.get()));
	public static final DeferredItem<Item> TOALLA = REGISTRY.register("toalla", ToallaItem::new);
	public static final DeferredItem<Item> PIPETA = REGISTRY.register("pipeta", PipetaItem::new);

	public static final DeferredItem<Item> BASIC_RED = registerHaba("basic_red", HabaTier.BASIC, HabaColor.RED);
	public static final DeferredItem<Item> BASIC_PURPLE = registerHaba("basic_purple", HabaTier.BASIC, HabaColor.PURPLE);
	public static final DeferredItem<Item> BASIC_ORANGE = registerHaba("basic_orange", HabaTier.BASIC, HabaColor.ORANGE);
	public static final DeferredItem<Item> BASIC_GREEN = registerHaba("basic_green", HabaTier.BASIC, HabaColor.GREEN);
	public static final DeferredItem<Item> BASIC_CIAN = registerHaba("basic_cian", HabaTier.BASIC, HabaColor.CIAN);
	public static final DeferredItem<Item> BASIC_BLUE = registerHaba("basic_blue", HabaTier.BASIC, HabaColor.BLUE);
	public static final DeferredItem<Item> BASIC_YELLOW = registerHaba("basic_yellow", HabaTier.BASIC, HabaColor.YELLOW);

	public static final DeferredItem<Item> IRIS_RED = registerHaba("iris_red", HabaTier.IRIS, HabaColor.RED);
	public static final DeferredItem<Item> IRIS_PURPLE = registerHaba("iris_purple", HabaTier.IRIS, HabaColor.PURPLE);
	public static final DeferredItem<Item> IRIS_ORANGE = registerHaba("iris_orange", HabaTier.IRIS, HabaColor.ORANGE);
	public static final DeferredItem<Item> IRIS_GREEN = registerHaba("iris_green", HabaTier.IRIS, HabaColor.GREEN);
	public static final DeferredItem<Item> IRIS_CIAN = registerHaba("iris_cian", HabaTier.IRIS, HabaColor.CIAN);
	public static final DeferredItem<Item> IRIS_BLUE = registerHaba("iris_blue", HabaTier.IRIS, HabaColor.BLUE);
	public static final DeferredItem<Item> IRIS_YELLOW = registerHaba("iris_yellow", HabaTier.IRIS, HabaColor.YELLOW);

	public static final DeferredItem<Item> ARCO_IRIS = REGISTRY.register("arco_iris", () -> new HabaItem(HabaTier.RAINBOW, null));

	private CobbleDomesticsModItems() {
	}

	private static DeferredItem<Item> registerHaba(String id, HabaTier tier, HabaColor color) {
		return REGISTRY.register(id, () -> new HabaItem(tier, color));
	}
}
