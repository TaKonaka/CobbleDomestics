package cobbledomestics.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.cobblemon.mod.common.client.render.layer.PokemonOnShoulderRenderer;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;

import cobbledomestics.bath.BathData;
import cobbledomestics.client.DirtTextureBaker;
import cobbledomestics.client.SoapTextureBaker;
import net.minecraft.resources.ResourceLocation;

/**
 * Shoulder mounts skip {@code PokemonRenderer#getTextureLocation}; bake dirt/soap on the
 * texture resolved inside {@code PokemonOnShoulderRenderer}'s private per-shoulder render.
 */
@Mixin(PokemonOnShoulderRenderer.class)
public abstract class PokemonOnShoulderRendererMixin {

	@Redirect(
		method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/player/Player;FFFFFFZ)V",
		at = @At(
			value = "INVOKE",
			target = "Lcom/cobblemon/mod/common/client/render/models/blockbench/repository/VaryingModelRepository;getTexture(Lnet/minecraft/resources/ResourceLocation;Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableState;)Lnet/minecraft/resources/ResourceLocation;"
		)
	)
	private ResourceLocation cobbledomestics$bakeShoulderDirtTexture(
			VaryingModelRepository repository,
			ResourceLocation name,
			PosableState state) {
		ResourceLocation original = repository.getTexture(name, state);
		int jabonoso = BathData.jabonosoFromAspects(state.getCurrentAspects());
		if (jabonoso > 0) {
			return SoapTextureBaker.getBakedTexture(original, jabonoso);
		}
		int suciedad = BathData.suciedadFromAspects(state.getCurrentAspects());
		if (suciedad <= 0) {
			return original;
		}
		return DirtTextureBaker.getBakedTexture(original, suciedad);
	}
}
