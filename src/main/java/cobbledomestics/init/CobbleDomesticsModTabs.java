package cobbledomestics.init;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CobbleDomesticsModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CobbleDomesticsMod.MODID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTRY.register("main", () -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.cobbledomestics"))
			.icon(() -> new ItemStack(CobbleDomesticsModItems.ARCO_IRIS.get()))
			.displayItems((params, output) -> {
				output.accept(CobbleDomesticsModItems.SOAP.get());
				output.accept(CobbleDomesticsModItems.TOALLA.get());
				output.accept(CobbleDomesticsModItems.PIPETA.get());
				output.accept(CobbleDomesticsModItems.BASIC_RED.get());
				output.accept(CobbleDomesticsModItems.BASIC_PURPLE.get());
				output.accept(CobbleDomesticsModItems.BASIC_ORANGE.get());
				output.accept(CobbleDomesticsModItems.BASIC_GREEN.get());
				output.accept(CobbleDomesticsModItems.BASIC_CIAN.get());
				output.accept(CobbleDomesticsModItems.BASIC_BLUE.get());
				output.accept(CobbleDomesticsModItems.BASIC_YELLOW.get());
				output.accept(CobbleDomesticsModItems.IRIS_RED.get());
				output.accept(CobbleDomesticsModItems.IRIS_PURPLE.get());
				output.accept(CobbleDomesticsModItems.IRIS_ORANGE.get());
				output.accept(CobbleDomesticsModItems.IRIS_GREEN.get());
				output.accept(CobbleDomesticsModItems.IRIS_CIAN.get());
				output.accept(CobbleDomesticsModItems.IRIS_BLUE.get());
				output.accept(CobbleDomesticsModItems.IRIS_YELLOW.get());
				output.accept(CobbleDomesticsModItems.ARCO_IRIS.get());
			})
			.build());

	private CobbleDomesticsModTabs() {
	}

	public static void register(IEventBus modEventBus) {
		REGISTRY.register(modEventBus);
	}
}
