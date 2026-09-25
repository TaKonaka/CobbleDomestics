package cobbledomestics.init;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.block.SoapBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CobbleDomesticsModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(CobbleDomesticsMod.MODID);

	public static final DeferredHolder<Block, SoapBlock> SOAP_BLOCK = REGISTRY.register("soap_block", () -> new SoapBlock());

	private CobbleDomesticsModBlocks() {
	}
}
