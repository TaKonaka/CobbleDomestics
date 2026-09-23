package cobbledomestics.client;

import java.util.UUID;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.AffectionData;
import cobbledomestics.affection.RubHint;
import cobbledomestics.affection.network.RubEndPacket;
import cobbledomestics.affection.network.RubPokePacket;
import cobbledomestics.affection.network.RubTickPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Immersive petting: free mouse, camera locked on a Pokémon, LMB to rub on hitbox.
 */
public final class ImmersiveInteractScreen extends Screen {
	private static final ResourceLocation HAND = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand.png");
	private static final ResourceLocation HAND_RED = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand_red.png");
	private static final ResourceLocation HAND_BLUE = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand_blue.png");
	private static final ResourceLocation HAND_GREEN = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand_green.png");
	private static final ResourceLocation HAND_ATTACK = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand_attack.png");

	private static final double MAX_DISTANCE = 2.0;
	private static final float CAMERA_LERP = 0.35F;
	private static final float SPEED_SMOOTHING = 0.55F;
	/** Amplifies tiny per-tick GUI deltas so small hitboxes can still reach speeds 1–5. */
	private static final float SPEED_AMPLIFY = 4.5F;
	private static final double HITBOX_INFLATE = 0.85;
	private static final double HITBOX_SCREEN_PAD = 28.0;
	private static final int ATTACK_PAUSE_TICKS = 20;
	private static final long POKE_WINDOW_MS = 400L;
	private static final int POKE_COUNT = 3;
	private static final int HAND_SIZE = 24;

	private final UUID pokemonEntityId;

	private boolean rubbing;
	private float smoothedMouseSpeed;
	private double lastMouseX = Double.NaN;
	private double lastMouseY = Double.NaN;
	private RubHint rubHint = RubHint.OK;
	private int attackPauseTicks;
	private final long[] pokeTimes = new long[POKE_COUNT];
	private int pokeIndex;

	public ImmersiveInteractScreen(UUID pokemonEntityId) {
		super(Component.empty());
		this.pokemonEntityId = pokemonEntityId;
	}

	public UUID getPokemonEntityId() {
		return pokemonEntityId;
	}

	public boolean isRubbing() {
		return rubbing;
	}

	public void applyRubHint(RubHint hint) {
		if (hint != null) {
			this.rubHint = hint;
		}
	}

	public void beginAttackPause() {
		this.attackPauseTicks = ATTACK_PAUSE_TICKS;
		stopRubbing(true);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void init() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getWindow() != null) {
			GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
		}
	}

	@Override
	public void removed() {
		stopRubbing(true);
		Minecraft mc = Minecraft.getInstance();
		if (mc.getWindow() != null) {
			GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
		}
		AffectionClient.onImmersiveClosed();
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		PokemonEntity pokemon = findPokemon(mc);
		if (player == null || pokemon == null || player.distanceTo(pokemon) > MAX_DISTANCE || pokemon.isBattling()) {
			onClose();
			return;
		}

		if (attackPauseTicks > 0) {
			attackPauseTicks--;
		}

		lockCamera(player, pokemon);

		double mx = currentGuiMouseX(mc);
		double my = currentGuiMouseY(mc);
		boolean over = isMouseOverHitbox(mc, pokemon, mx, my);

		if (rubbing && !over) {
			stopRubbing(true);
			return;
		}

		// Poll action key every tick (UNIVERSAL context) so remapped keys exit reliably.
		if (CobbleDomesticsKeyMappings.RUB.consumeClick()) {
			AffectionClient.consumeRubToggle();
			if (rubbing) {
				stopRubbing(true);
			} else {
				onClose();
				return;
			}
		}

		if (rubbing && attackPauseTicks <= 0) {
			// Sample mouse delta every tick — don't rely only on drag events.
			updateMouseSpeed(mx, my);
			int speed = mapToRubSpeed(smoothedMouseSpeed);
			PacketDistributor.sendToServer(new RubTickPacket(pokemonEntityId, speed));
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		ResourceLocation hand = currentHandTexture();
		int x = mouseX - HAND_SIZE / 2;
		int y = mouseY - HAND_SIZE / 2;
		graphics.blit(hand, x, y, 0, 0, HAND_SIZE, HAND_SIZE, HAND_SIZE, HAND_SIZE);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Keep world visible.
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// Action key remapped to a mouse button.
		if (CobbleDomesticsKeyMappings.RUB.matchesMouse(button)) {
			AffectionClient.consumeRubToggle();
			if (rubbing) {
				stopRubbing(true);
			} else {
				onClose();
			}
			return true;
		}

		if (button != 0) {
			return super.mouseClicked(mouseX, mouseY, button);
		}
		if (attackPauseTicks > 0) {
			return true;
		}

		Minecraft mc = Minecraft.getInstance();
		PokemonEntity pokemon = findPokemon(mc);
		boolean over = pokemon != null && isMouseOverHitbox(mc, pokemon, mouseX, mouseY);
		if (!over) {
			return true;
		}

		recordPoke();
		if (isTriplePoke()) {
			PacketDistributor.sendToServer(new RubPokePacket(pokemonEntityId));
			beginAttackPause();
			return true;
		}

		// Clear any leftover server progress so the 1–10 counter starts fresh each pet.
		PacketDistributor.sendToServer(new RubEndPacket());
		rubbing = true;
		smoothedMouseSpeed = 0.0F;
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		rubHint = RubHint.OK;
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			stopRubbing(true);
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0 && rubbing && attackPauseTicks <= 0) {
			updateMouseSpeed(mouseX, mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (rubbing && attackPauseTicks <= 0) {
			updateMouseSpeed(mouseX, mouseY);
		} else {
			lastMouseX = mouseX;
			lastMouseY = mouseY;
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (CobbleDomesticsKeyMappings.RUB.matches(keyCode, scanCode)) {
			AffectionClient.consumeRubToggle();
			if (rubbing) {
				stopRubbing(true);
				return true;
			}
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void updateMouseSpeed(double mouseX, double mouseY) {
		if (!Double.isNaN(lastMouseX)) {
			double dx = mouseX - lastMouseX;
			double dy = mouseY - lastMouseY;
			float speed = (float) Math.sqrt(dx * dx + dy * dy) * SPEED_AMPLIFY;
			smoothedMouseSpeed = Mth.lerp(SPEED_SMOOTHING, smoothedMouseSpeed, speed);
		}
		lastMouseX = mouseX;
		lastMouseY = mouseY;
	}

	private void stopRubbing(boolean notifyServer) {
		if (rubbing && notifyServer) {
			PacketDistributor.sendToServer(new RubEndPacket());
		}
		rubbing = false;
		smoothedMouseSpeed = 0.0F;
		rubHint = RubHint.OK;
	}

	private ResourceLocation currentHandTexture() {
		if (attackPauseTicks > 0) {
			return HAND_ATTACK;
		}
		if (!rubbing) {
			return HAND;
		}
		return switch (rubHint) {
			case TOO_FAST -> HAND_BLUE;
			case TOO_SLOW -> HAND_RED;
			case OK -> HAND_GREEN;
		};
	}

	private void recordPoke() {
		pokeTimes[pokeIndex % POKE_COUNT] = System.currentTimeMillis();
		pokeIndex++;
	}

	private boolean isTriplePoke() {
		if (pokeIndex < POKE_COUNT) {
			return false;
		}
		long newest = pokeTimes[(pokeIndex - 1) % POKE_COUNT];
		long oldest = pokeTimes[pokeIndex % POKE_COUNT];
		return newest - oldest <= POKE_WINDOW_MS;
	}

	private static void lockCamera(LocalPlayer player, PokemonEntity pokemon) {
		Vec3 eye = player.getEyePosition();
		Vec3 target = pokemon.position().add(0.0, pokemon.getBbHeight() * 0.55, 0.0);
		Vec3 delta = target.subtract(eye);
		double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		float desiredYaw = (float) (Mth.atan2(-delta.x, delta.z) * Mth.RAD_TO_DEG);
		float desiredPitch = (float) -(Mth.atan2(delta.y, flat) * Mth.RAD_TO_DEG);
		float yaw = Mth.rotLerp(CAMERA_LERP, player.getYRot(), desiredYaw);
		float pitch = Mth.lerp(CAMERA_LERP, player.getXRot(), Mth.clamp(desiredPitch, -90.0F, 90.0F));
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.yRotO = yaw;
		player.xRotO = pitch;
	}

	/**
	 * Projects an inflated Pokémon AABB to GUI space and tests if the mouse is inside.
	 */
	private boolean isMouseOverHitbox(Minecraft mc, PokemonEntity pokemon, double mouseX, double mouseY) {
		AABB bb = pokemon.getBoundingBox().inflate(HITBOX_INFLATE);
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		boolean any = false;

		double[] xs = {bb.minX, bb.maxX};
		double[] ys = {bb.minY, bb.maxY};
		double[] zs = {bb.minZ, bb.maxZ};
		for (double x : xs) {
			for (double y : ys) {
				for (double z : zs) {
					float[] screen = worldToScreen(mc, new Vec3(x, y, z));
					if (screen == null) {
						continue;
					}
					any = true;
					minX = Math.min(minX, screen[0]);
					minY = Math.min(minY, screen[1]);
					maxX = Math.max(maxX, screen[0]);
					maxY = Math.max(maxY, screen[1]);
				}
			}
		}
		if (!any) {
			return false;
		}
		return mouseX >= minX - HITBOX_SCREEN_PAD && mouseX <= maxX + HITBOX_SCREEN_PAD
				&& mouseY >= minY - HITBOX_SCREEN_PAD && mouseY <= maxY + HITBOX_SCREEN_PAD;
	}

	private static float[] worldToScreen(Minecraft mc, Vec3 world) {
		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 camPos = camera.getPosition();
		Vec3 delta = world.subtract(camPos);

		Vector3f look = camera.getLookVector();
		Vector3f up = camera.getUpVector();
		Vector3f left = camera.getLeftVector();
		Vec3 forward = new Vec3(look.x(), look.y(), look.z());
		Vec3 upV = new Vec3(up.x(), up.y(), up.z());
		Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());

		double z = delta.dot(forward);
		if (z <= 0.05) {
			return null;
		}
		double x = delta.dot(right);
		double y = delta.dot(upV);

		double fovDeg = mc.options.fov().get();
		double tanHalf = Math.tan(Math.toRadians(fovDeg) * 0.5);
		int guiW = mc.getWindow().getGuiScaledWidth();
		int guiH = mc.getWindow().getGuiScaledHeight();
		double aspect = (double) guiW / (double) Math.max(1, guiH);

		double ndcX = x / (z * tanHalf * aspect);
		double ndcY = y / (z * tanHalf);
		float sx = (float) ((ndcX + 1.0) * 0.5 * guiW);
		float sy = (float) ((1.0 - ndcY) * 0.5 * guiH);
		return new float[]{sx, sy};
	}

	private static double currentGuiMouseX(Minecraft mc) {
		return mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
	}

	private static double currentGuiMouseY(Minecraft mc) {
		return mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
	}

	private PokemonEntity findPokemon(Minecraft mc) {
		if (mc.level == null) {
			return null;
		}
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (pokemonEntityId.equals(entity.getUUID()) && entity instanceof PokemonEntity pokemonEntity) {
				return pokemonEntity;
			}
		}
		return null;
	}

	private static int mapToRubSpeed(float smoothed) {
		// Amplified per-tick GUI motion — calibrated so small circles can hit 3–5.
		if (smoothed < 0.8F) {
			return 0;
		}
		if (smoothed < 2.2F) {
			return 1;
		}
		if (smoothed < 4.5F) {
			return 2;
		}
		if (smoothed < 7.5F) {
			return 3;
		}
		if (smoothed < 12.0F) {
			return 4;
		}
		return AffectionData.RUB_SPEED_MAX;
	}
}
