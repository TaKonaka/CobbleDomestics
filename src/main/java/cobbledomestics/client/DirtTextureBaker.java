package cobbledomestics.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;

import cobbledomestics.CobbleDomesticsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Genera, bajo demanda y en caché, una textura combinada "base + suciedad" a partir de la
 * textura original de un Pokémon y de dirt.png, en vez de dibujar la suciedad como una capa
 * de render aparte (lo que producía z-fighting).
 *
 * Reglas del horneado:
 *  - Para cada píxel de la textura base con alpha == 0 (fuera de la silueta del modelo),
 *    NUNCA se pinta suciedad ahí: se copia tal cual (mascarado por la textura original).
 *  - Para píxeles con alpha > 0, se mezcla el color de dirt.png con el color base, usando
 *    como fuerza de mezcla: (alpha del píxel de dirt / 255) * opacidad del nivel de suciedad.
 *  - El alpha del píxel resultante siempre es el alpha ORIGINAL del Pokémon (nunca se toca),
 *    así la silueta del modelo nunca cambia, solo su color.
 *  - dirt.png se muestrea reescalando sus coordenadas al tamaño de cada textura base, así
 *    sirve para Pokémon con distintas resoluciones de textura sin tener que pintar una
 *    máscara distinta a mano por especie.
 */
public final class DirtTextureBaker {

	private static final ResourceLocation DIRT_SOURCE =
		ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "textures/entity/pokemon/dirt.png");

	// Índice 0 sin usar (suciedad 0 == sin suciedad, se devuelve la textura original sin hornear).
	private static final float[] ALPHA_BY_LEVEL = {0.0F, 0.28F, 0.44F, 0.60F, 0.78F, 0.95F};

	private static NativeImage dirtSourceImage;
	private static boolean dirtLoadFailed;

	private static final Map<CacheKey, ResourceLocation> CACHE = new HashMap<>();

	private DirtTextureBaker() {
	}

	/**
	 * Devuelve la ResourceLocation a usar para renderizar: la original si suciedadLevel <= 0,
	 * o una textura horneada (generada la primera vez, cacheada después) si suciedadLevel > 0.
	 */
	public static ResourceLocation getBakedTexture(ResourceLocation baseTexture, int suciedadLevel) {
		if (suciedadLevel <= 0 || suciedadLevel >= ALPHA_BY_LEVEL.length) {
			return baseTexture;
		}
		CacheKey key = new CacheKey(baseTexture, suciedadLevel);
		ResourceLocation cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		ResourceLocation baked = bake(baseTexture, suciedadLevel);
		ResourceLocation result = baked != null ? baked : baseTexture;
		CACHE.put(key, result);
		return result;
	}

	/** Limpia la caché (llamar en un resource-reload del cliente para no arrastrar texturas viejas). */
	public static void clearCache() {
		CACHE.clear();
		if (dirtSourceImage != null) {
			dirtSourceImage.close();
		}
		dirtSourceImage = null;
		dirtLoadFailed = false;
	}

	private static ResourceLocation bake(ResourceLocation baseTexture, int suciedadLevel) {
		NativeImage dirt = dirtSource();
		if (dirt == null) {
			return null;
		}
		try (NativeImage base = readImage(baseTexture)) {
			if (base == null) {
				return null;
			}
			int width = base.getWidth();
			int height = base.getHeight();
			float opacity = ALPHA_BY_LEVEL[suciedadLevel];

			try (NativeImage output = new NativeImage(width, height, false)) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						bakePixel(base, dirt, output, x, y, width, height, opacity);
					}
				}

				ResourceLocation dynamicId = ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID,
					"dynamic/dirt/" + suciedadLevel + "/" + baseTexture.getNamespace() + "/"
						+ baseTexture.getPath().replace('/', '_'));
				DynamicTexture texture = new DynamicTexture(output);
				Minecraft.getInstance().getTextureManager().register(dynamicId, texture);
				return dynamicId;
			}
		} catch (IOException exception) {
			CobbleDomesticsMod.LOGGER.error("No se pudo generar la textura de suciedad para {}", baseTexture, exception);
			return null;
		}
	}

	private static void bakePixel(NativeImage base, NativeImage dirt, NativeImage output,
			int x, int y, int width, int height, float opacity) {
		int baseColor = base.getPixelRGBA(x, y);
		int baseAlpha = (baseColor >>> 24) & 0xFF;

		// Fuera de la silueta del Pokémon: se respeta el recorte de la textura original,
		// nunca se pinta suciedad sobre alpha 0.
		if (baseAlpha == 0) {
			output.setPixelRGBA(x, y, baseColor);
			return;
		}

		int dirtX = Math.min(dirt.getWidth() - 1, x * dirt.getWidth() / width);
		int dirtY = Math.min(dirt.getHeight() - 1, y * dirt.getHeight() / height);
		int dirtColor = dirt.getPixelRGBA(dirtX, dirtY);
		int dirtAlpha = (dirtColor >>> 24) & 0xFF;

		float effective = (dirtAlpha / 255.0F) * opacity;
		if (effective <= 0.0F) {
			output.setPixelRGBA(x, y, baseColor);
			return;
		}

		int baseR = baseColor & 0xFF;
		int baseG = (baseColor >>> 8) & 0xFF;
		int baseB = (baseColor >>> 16) & 0xFF;
		int dirtR = dirtColor & 0xFF;
		int dirtG = (dirtColor >>> 8) & 0xFF;
		int dirtB = (dirtColor >>> 16) & 0xFF;

		int r = Math.round(baseR * (1.0F - effective) + dirtR * effective);
		int g = Math.round(baseG * (1.0F - effective) + dirtG * effective);
		int b = Math.round(baseB * (1.0F - effective) + dirtB * effective);

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

	private static NativeImage dirtSource() {
		if (dirtSourceImage != null || dirtLoadFailed) {
			return dirtSourceImage;
		}
		try {
			dirtSourceImage = readImage(DIRT_SOURCE);
		} catch (IOException exception) {
			CobbleDomesticsMod.LOGGER.error("No se pudo cargar dirt.png", exception);
		}
		if (dirtSourceImage == null) {
			dirtLoadFailed = true;
		}
		return dirtSourceImage;
	}

	private record CacheKey(ResourceLocation baseTexture, int level) {
	}
}
