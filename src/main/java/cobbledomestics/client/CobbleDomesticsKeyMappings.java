package cobbledomestics.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = CobbleDomesticsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CobbleDomesticsKeyMappings {
	public static final String CATEGORY = "key.categories.cobbledomestics";

	public static final KeyMapping RUB = new KeyMapping(
			"key.cobbledomestics.rub",
			KeyConflictContext.UNIVERSAL,
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			CATEGORY);

	private CobbleDomesticsKeyMappings() {
	}

	@SubscribeEvent
	public static void register(RegisterKeyMappingsEvent event) {
		event.register(RUB);
	}
}
