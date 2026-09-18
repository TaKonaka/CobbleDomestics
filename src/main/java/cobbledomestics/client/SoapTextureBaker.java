package cobbledomestics.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.bath.BathData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Hornea texturas "base + burbujas" usando burbles_1..5.png como máscaras por alpha
 * (blanco opaco/semitransparente = espuma, transparente = sin cambio), misma técnica
 * que DirtTextureBaker.
 */
public final class SoapTextureBaker {

	private static final Map<Integer, NativeImage> BUBBLE_SOURCES = new HashMap<>();
	private static final Map<Integer, Boolean> BUBBLE_LOAD_FAILED = new HashMap<>();
	private static final Map<CacheKey, ResourceLocation> CACHE = new HashMap<>();

	private SoapTextureBaker() {
	}

	public static ResourceLocation getBakedTexture(ResourceLocation baseTexture, int jabonosoLevel) {
		if (jabonosoLevel <= 0 || jabonosoLevel > BathData.MAX_JABONOSO) {
			return baseTexture;
		}
		CacheKey key = new CacheKey(baseTexture, jabonosoLevel);
		ResourceLocation cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		ResourceLocation baked = bake(baseTexture, jabonosoLevel);
		ResourceLocation result = baked != null ? baked : baseTexture;
		CACHE.put(key, result);
		return result;
	}

	public static void clearCache() {
		CACHE.clear();
		for (NativeImage image : BUBBLE_SOURCES.values()) {
			if (image != null) {
				image.close();
			}
		}
		BUBBLE_SOURCES.clear();
		BUBBLE_LOAD_FAILED.clear();
	}

	private static ResourceLocation bake(ResourceLocation baseTexture, int jabonosoLevel) {
		NativeImage bubbles = bubbleSource(jabonosoLevel);
		if (bubbles == null) {
			return null;
		}
		try (NativeImage base = readImage(baseTexture)) {
			if (base == null) {
				return null;
			}
			int width = base.getWidth();
			int height = base.getHeight();

			try (NativeImage output = new NativeImage(width, height, false)) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						bakePixel(base, bubbles, output, x, y, width, height);
					}
				}

				ResourceLocation dynamicId = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID,
					"dynamic/soap/" + jabonosoLevel + "/" + baseTexture.getNamespace() + "/"
						+ baseTexture.getPath().replace('/', '_'));
				DynamicTexture texture = new DynamicTexture(output);
				Minecraft.getInstance().getTextureManager().register(dynamicId, texture);
				return dynamicId;
			}
		} catch (IOException exception) {
			CobbleDomesticsMod.LOGGER.error("No se pudo generar la textura de burbujas para {}", baseTexture, exception);
			return null;
		}
	}

	private static void bakePixel(NativeImage base, NativeImage bubbles, NativeImage output,
			int x, int y, int width, int height) {
		int baseColor = base.getPixelRGBA(x, y);
		int baseAlpha = (baseColor >>> 24) & 0xFF;

		if (baseAlpha == 0) {
			output.setPixelRGBA(x, y, baseColor);
			return;
		}

		int bubbleX = Math.min(bubbles.getWidth() - 1, x * bubbles.getWidth() / width);
		int bubbleY = Math.min(bubbles.getHeight() - 1, y * bubbles.getHeight() / height);
		int bubbleColor = bubbles.getPixelRGBA(bubbleX, bubbleY);

		// Máscara por ALPHA: las burbles son blanco con fondo transparente
		// (RGB del fondo suele ser 255,255,255; luminancia pintaría todo de blanco).
		int bubbleR = bubbleColor & 0xFF;
		int bubbleG = (bubbleColor >>> 8) & 0xFF;
		int bubbleB = (bubbleColor >>> 16) & 0xFF;
		int bubbleA = (bubbleColor >>> 24) & 0xFF;
		float effective = bubbleA / 255.0F;
		if (effective <= 0.0F) {
			output.setPixelRGBA(x, y, baseColor);
			return;
		}

		int baseR = baseColor & 0xFF;
		int baseG = (baseColor >>> 8) & 0xFF;
		int baseB = (baseColor >>> 16) & 0xFF;

		int r = Math.round(baseR * (1.0F - effective) + bubbleR * effective);
		int g = Math.round(baseG * (1.0F - effective) + bubbleG * effective);
		int b = Math.round(baseB * (1.0F - effective) + bubbleB * effective);

		int blended = (baseAlpha << 24) | (b << 16) | (g << 8) | r;
		output.setPixelRGBA(x, y, blended);
	}

	private static NativeImage readImage(ResourceLocation location) throws IOException {
		Resource resource = Minecraft.getInstance().getResourceManager().getResource(location).orElse(null);
		if (resource == null) {
			return null;
		}
		try (InputStream stream = resource.open()) {
			return NativeImage.read(stream);
		}
	}

	private static ResourceLocation bubbleLocation(int level) {
		return ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID,
			"textures/entity/pokemon/burbles_" + level + ".png");
	}

	private static NativeImage bubbleSource(int level) {
		if (BUBBLE_SOURCES.containsKey(level) || Boolean.TRUE.equals(BUBBLE_LOAD_FAILED.get(level))) {
			return BUBBLE_SOURCES.get(level);
		}
		try {
			NativeImage image = readImage(bubbleLocation(level));
			if (image != null) {
				BUBBLE_SOURCES.put(level, image);
				return image;
			}
		} catch (IOException exception) {
			CobbleDomesticsMod.LOGGER.error("No se pudo cargar burbles_{}.png", level, exception);
		}
		BUBBLE_LOAD_FAILED.put(level, true);
		return null;
	}

	private record CacheKey(ResourceLocation baseTexture, int level) {
	}
}
