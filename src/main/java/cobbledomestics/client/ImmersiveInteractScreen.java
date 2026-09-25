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
import cobbledomestics.bath.BathItems;
import cobbledomestics.bath.network.BathRinsePacket;
import cobbledomestics.bath.network.BathScrubEndPacket;
import cobbledomestics.bath.network.BathScrubTickPacket;
import cobbledomestics.init.CobbleDomesticsModSounds;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Unified immersive care: free mouse, camera locked on a Pokémon, hotbar usable.
 * Empty hand → mimos; soap/towel/pipette → scrub; water bucket → rinse (RMB).
 */
public final class ImmersiveInteractScreen extends Screen {
	private static final ResourceLocation HAND = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand.png");
	private static final ResourceLocation HAND_RED = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand_red.png");
	private static final ResourceLocation HAND_BLUE = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand_blue.png");
	private static final ResourceLocation HAND_GREEN = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand_green.png");
	private static final ResourceLocation HAND_ATTACK = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/gui/interact/hand_attack.png");
	private static final ResourceLocation TOWEL_CURSOR = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/item/towel.png");
	private static final ResourceLocation TOWEL_EXTEND = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/item/extend.png");

	private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("hud/hotbar");
	private static final ResourceLocation HOTBAR_SELECTION = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");

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
	private static final int TOWEL_CURSOR_SIZE = 16;
	private static final float MOVE_THRESHOLD = 0.35F;

	private final UUID pokemonEntityId;

	private boolean rubbing;
	private boolean scrubbing;
	private float smoothedMouseSpeed;
	private double lastMouseX = Double.NaN;
	private double lastMouseY = Double.NaN;
	private RubHint rubHint = RubHint.OK;
	private int attackPauseTicks;
	private final long[] pokeTimes = new long[POKE_COUNT];
	private int pokeIndex;
	private boolean movedThisTick;
	private ImmersiveScrubSound scrubSound;
	private ImmersiveRubSound rubSound;

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
		if (hint == null) {
			return;
		}
		boolean wasCorrect = ImmersiveRubSound.isCorrectHint(this.rubHint);
		this.rubHint = hint;
		if (rubbing) {
			boolean nowCorrect = ImmersiveRubSound.isCorrectHint(hint);
			if (rubSound == null || wasCorrect != nowCorrect) {
				startRubSound(nowCorrect);
			}
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
		mc.getSoundManager().play(SimpleSoundInstance.forUI(CobbleDomesticsModSounds.RELAX_ENTER.get(), 1.0F));
	}

	@Override
	public void removed() {
		stopAllActions(true);
		Minecraft mc = Minecraft.getInstance();
		if (mc.getWindow() != null) {
			GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
		}
		mc.getSoundManager().play(SimpleSoundInstance.forUI(CobbleDomesticsModSounds.RELAX_ENTER.get(), 1.0F));
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

		ItemStack held = player.getMainHandItem();
		syncModeToHeldItem(held);

		if (rubbing && !over) {
			stopRubbing(true);
		}
		if (scrubbing && !over) {
			stopScrubbing(true);
		}

		// Poll action key every tick (UNIVERSAL context) so remapped keys exit reliably.
		if (CobbleDomesticsKeyMappings.RUB.consumeClick()) {
			AffectionClient.consumeRubToggle();
			if (rubbing || scrubbing) {
				stopAllActions(true);
			} else {
				onClose();
				return;
			}
		}

		if (rubbing && attackPauseTicks <= 0 && over) {
			updateMouseSpeed(mx, my);
			int speed = mapToRubSpeed(smoothedMouseSpeed);
			PacketDistributor.sendToServer(new RubTickPacket(pokemonEntityId, speed));
		} else if (scrubbing && over) {
			updateMouseMotion(mx, my);
			if (BathItems.isScrubTool(held) && movedThisTick) {
				int speed = mapToScrubSpeed(smoothedMouseSpeed);
				PacketDistributor.sendToServer(new BathScrubTickPacket(pokemonEntityId, speed));
			}
		} else if (scrubbing || rubbing) {
			smoothedMouseSpeed = Mth.lerp(SPEED_SMOOTHING, smoothedMouseSpeed, 0.0F);
		}

		movedThisTick = false;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}

		renderHotbar(graphics, player);
		renderCursor(graphics, player, mouseX, mouseY);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Keep world visible.
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (CobbleDomesticsKeyMappings.RUB.matchesMouse(button)) {
			AffectionClient.consumeRubToggle();
			if (rubbing || scrubbing) {
				stopAllActions(true);
			} else {
				onClose();
			}
			return true;
		}

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		PokemonEntity pokemon = findPokemon(mc);
		boolean over = pokemon != null && isMouseOverHitbox(mc, pokemon, mouseX, mouseY);
		if (player == null || !over) {
			return true;
		}

		ItemStack held = player.getMainHandItem();

		if (button == 0) {
			if (held.isEmpty()) {
				if (attackPauseTicks > 0) {
					return true;
				}
				recordPoke();
				if (isTriplePoke()) {
					PacketDistributor.sendToServer(new RubPokePacket(pokemonEntityId));
					beginAttackPause();
					return true;
				}
				stopScrubbing(true);
				PacketDistributor.sendToServer(new RubEndPacket());
				rubbing = true;
				smoothedMouseSpeed = 0.0F;
				lastMouseX = mouseX;
				lastMouseY = mouseY;
				rubHint = RubHint.OK;
				startRubSound(true);
				return true;
			}
			if (BathItems.isSoap(held) || BathItems.isTowel(held) || BathItems.isPipeta(held)) {
				stopRubbing(true);
				PacketDistributor.sendToServer(new BathScrubEndPacket());
				scrubbing = true;
				smoothedMouseSpeed = 0.0F;
				lastMouseX = mouseX;
				lastMouseY = mouseY;
				movedThisTick = false;
				startScrubSound(held);
			}
			return true;
		}

		if (button == 1 && BathItems.isWaterBucket(held)) {
			PacketDistributor.sendToServer(new BathRinsePacket(pokemonEntityId));
			return true;
		}

		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			stopAllActions(true);
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0 && attackPauseTicks <= 0) {
			if (rubbing) {
				updateMouseSpeed(mouseX, mouseY);
				return true;
			}
			if (scrubbing) {
				updateMouseMotion(mouseX, mouseY);
				return true;
			}
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (rubbing && attackPauseTicks <= 0) {
			updateMouseSpeed(mouseX, mouseY);
		} else if (scrubbing) {
			updateMouseMotion(mouseX, mouseY);
		} else {
			lastMouseX = mouseX;
			lastMouseY = mouseY;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null && scrollY != 0.0) {
			player.getInventory().swapPaint(scrollY);
			syncModeToHeldItem(player.getMainHandItem());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (CobbleDomesticsKeyMappings.RUB.matches(keyCode, scanCode)) {
			AffectionClient.consumeRubToggle();
			if (rubbing || scrubbing) {
				stopAllActions(true);
				return true;
			}
			onClose();
			return true;
		}

		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player != null) {
			for (int i = 0; i < 9; i++) {
				if (mc.options.keyHotbarSlots[i].matches(keyCode, scanCode)) {
					player.getInventory().selected = i;
					syncModeToHeldItem(player.getMainHandItem());
					return true;
				}
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void renderCursor(GuiGraphics graphics, LocalPlayer player, int mouseX, int mouseY) {
		ItemStack held = player.getMainHandItem();
		if (held.isEmpty()) {
			ResourceLocation hand = currentHandTexture();
			int x = mouseX - HAND_SIZE / 2;
			int y = mouseY - HAND_SIZE / 2;
			graphics.blit(hand, x, y, 0, 0, HAND_SIZE, HAND_SIZE, HAND_SIZE, HAND_SIZE);
			return;
		}
		if (BathItems.isTowel(held)) {
			ResourceLocation towel = scrubbing ? TOWEL_EXTEND : TOWEL_CURSOR;
			int x = mouseX - TOWEL_CURSOR_SIZE / 2;
			int y = mouseY - TOWEL_CURSOR_SIZE / 2;
			graphics.blit(towel, x, y, 0, 0, TOWEL_CURSOR_SIZE, TOWEL_CURSOR_SIZE, TOWEL_CURSOR_SIZE, TOWEL_CURSOR_SIZE);
			return;
		}
		if (BathItems.isSoap(held) || BathItems.isWaterBucket(held) || BathItems.isPipeta(held)) {
			graphics.renderItem(held, mouseX - 8, mouseY - 8);
			graphics.renderItemDecorations(Minecraft.getInstance().font, held, mouseX - 8, mouseY - 8);
		}
	}

	private void renderHotbar(GuiGraphics graphics, LocalPlayer player) {
		Inventory inventory = player.getInventory();
		int left = this.width / 2 - 91;
		int top = this.height - 22;
		graphics.blitSprite(HOTBAR, left, top, 182, 22);
		graphics.blitSprite(HOTBAR_SELECTION, left - 1 + inventory.selected * 20, top - 1, 24, 23);
		for (int i = 0; i < 9; i++) {
			int slotX = left + i * 20 + 3;
			int slotY = top + 3;
			ItemStack stack = inventory.items.get(i);
			if (!stack.isEmpty()) {
				graphics.renderItem(player, stack, slotX, slotY, i);
				graphics.renderItemDecorations(this.font, stack, slotX, slotY);
			}
		}
	}

	/** Stops scrub/rub when the held item no longer matches the active mode. */
	private void syncModeToHeldItem(ItemStack held) {
		if (rubbing && !held.isEmpty()) {
			stopRubbing(true);
		}
		if (scrubbing && !BathItems.isScrubTool(held)) {
			stopScrubbing(true);
		}
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

	private void updateMouseMotion(double mouseX, double mouseY) {
		if (!Double.isNaN(lastMouseX)) {
			double dx = mouseX - lastMouseX;
			double dy = mouseY - lastMouseY;
			float delta = (float) Math.sqrt(dx * dx + dy * dy);
			if (delta >= MOVE_THRESHOLD) {
				movedThisTick = true;
			}
			float speed = delta * SPEED_AMPLIFY;
			smoothedMouseSpeed = Mth.lerp(SPEED_SMOOTHING, smoothedMouseSpeed, speed);
		}
		lastMouseX = mouseX;
		lastMouseY = mouseY;
	}

	private void stopAllActions(boolean notifyServer) {
		stopRubbing(notifyServer);
		stopScrubbing(notifyServer);
	}

	private void stopRubbing(boolean notifyServer) {
		if (rubbing && notifyServer) {
			PacketDistributor.sendToServer(new RubEndPacket());
		}
		rubbing = false;
		smoothedMouseSpeed = 0.0F;
		rubHint = RubHint.OK;
		stopRubSound();
	}

	private void startRubSound(boolean correct) {
		stopRubSound();
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		rubSound = new ImmersiveRubSound(player, correct);
		Minecraft.getInstance().getSoundManager().play(rubSound);
	}

	private void stopRubSound() {
		if (rubSound != null) {
			rubSound.requestStop();
			rubSound = null;
		}
	}

	private void stopScrubbing(boolean notifyServer) {
		if (scrubbing && notifyServer) {
			PacketDistributor.sendToServer(new BathScrubEndPacket());
		}
		scrubbing = false;
		smoothedMouseSpeed = 0.0F;
		movedThisTick = false;
		stopScrubSound();
	}

	private void startScrubSound(ItemStack held) {
		stopScrubSound();
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		ImmersiveScrubSound sound = ImmersiveScrubSound.forHeldItem(player, held);
		if (sound != null) {
			scrubSound = sound;
			Minecraft.getInstance().getSoundManager().play(sound);
		}
	}

	private void stopScrubSound() {
		if (scrubSound != null) {
			scrubSound.requestStop();
			scrubSound = null;
		}
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

	/** Maps smoothed GUI motion to scrub contribution 1–5 (faster = less time to apply). */
	private static int mapToScrubSpeed(float smoothed) {
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
		return 5;
	}
}
