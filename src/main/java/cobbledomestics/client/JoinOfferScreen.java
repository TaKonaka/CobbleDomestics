package cobbledomestics.client;

import java.util.UUID;

import cobbledomestics.affection.network.JoinOfferResponsePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class JoinOfferScreen extends Screen {
	private final UUID pokemonEntityId;

	public JoinOfferScreen(UUID pokemonEntityId, Component pokemonName) {
		super(Component.translatable("message.cobbledomestics.join.offer", pokemonName));
		this.pokemonEntityId = pokemonEntityId;
	}

	public static void open(UUID pokemonEntityId, String pokemonName) {
		Minecraft.getInstance().setScreen(new JoinOfferScreen(pokemonEntityId, Component.literal(pokemonName)));
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		int buttonWidth = 100;
		int buttonHeight = 20;
		int gap = 8;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.cobbledomestics.join.accept"), button -> {
			PacketDistributor.sendToServer(new JoinOfferResponsePacket(pokemonEntityId, true));
			this.onClose();
		}).bounds(centerX - buttonWidth - gap / 2, centerY + 20, buttonWidth, buttonHeight).build());

		this.addRenderableWidget(Button.builder(Component.translatable("gui.cobbledomestics.join.reject"), button -> {
			PacketDistributor.sendToServer(new JoinOfferResponsePacket(pokemonEntityId, false));
			this.onClose();
		}).bounds(centerX + gap / 2, centerY + 20, buttonWidth, buttonHeight).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(this.font, this.getTitle(), this.width / 2, this.height / 2 - 20, 0xFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
