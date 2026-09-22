package cobbledomestics.client;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientInput {
	private ClientInput() {
	}

	public static boolean hasShiftDown() {
		return Screen.hasShiftDown();
	}
}
