package cobbledomestics.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import cobbledomestics.client.DirtTextureBaker;
import cobbledomestics.client.PokemonDirtLayer;
import cobbledomestics.client.SoapTextureBaker;
import net.minecraft.resources.ResourceLocation;

/**
 * Cobblemon resuelve la textura de cada Pokémon en PokemonRenderer#getTextureLocation
 * (el método estándar que hereda de MobRenderer/EntityRenderer, ver PokemonRenderer.kt):
 *
 *   override fun getTextureLocation(entity: PokemonEntity): ResourceLocation {
 *       return VaryingModelRepository.getTexture(...)
 *   }
 *
 * Este mixin intercepta el valor de retorno de ESE método puntual, para ese entity en
 * particular, y lo cambia por la textura horneada (base + suciedad o burbujas) cuando corresponde.
 * No se toca PosableModel ni sus layers: es el mismo draw call, con otra ResourceLocation
 * bindeada, así que no hay geometría duplicada ni z-fighting posible.
 */
@Mixin(PokemonRenderer.class)
public abstract class PokemonRendererMixin {

	// El descriptor exacto evita que Mixin se confunda con el bridge method sintético
	// que Kotlin genera para el override genérico de MobRenderer<T, M>.
	@Inject(
		method = "getTextureLocation(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)Lnet/minecraft/resources/ResourceLocation;",
		at = @At("RETURN"),
		cancellable = true
	)
	private void cobbledomestics$applyDirtTexture(PokemonEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
		ResourceLocation original = cir.getReturnValue();
		int jabonoso = PokemonDirtLayer.visibleJabonoso(entity);
		if (jabonoso > 0) {
			ResourceLocation baked = SoapTextureBaker.getBakedTexture(original, jabonoso);
			if (baked != original) {
				cir.setReturnValue(baked);
			}
			return;
		}
		int suciedad = PokemonDirtLayer.visibleSuciedad(entity);
		if (suciedad <= 0) {
			return;
		}
		ResourceLocation baked = DirtTextureBaker.getBakedTexture(original, suciedad);
		if (baked != original) {
			cir.setReturnValue(baked);
		}
	}
}
