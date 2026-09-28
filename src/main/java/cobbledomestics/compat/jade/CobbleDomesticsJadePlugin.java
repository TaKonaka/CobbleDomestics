package cobbledomestics.compat.jade;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;

import cobbledomestics.CobbleDomesticsMod;
import cobbledomestics.affection.AffectionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class CobbleDomesticsJadePlugin implements IWailaPlugin {
	public static final ResourceLocation AFFECTION_UID =
			ResourceLocation.fromNamespaceAndPath(CobbleDomesticsMod.MODID, "affection");

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerEntityDataProvider(PokemonAffectionProvider.INSTANCE, PokemonEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerEntityComponent(PokemonAffectionProvider.INSTANCE, PokemonEntity.class);
	}

	public enum PokemonAffectionProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
		INSTANCE;

		private static final String HUMOR = "cd_humor";
		private static final String WILD = "cd_wild";
		private static final String CONFIANZA = "cd_confianza";
		private static final String CONFIANZA_MAX = "cd_confianza_max";
		private static final String AMISTAD = "cd_amistad";

		@Override
		public void appendServerData(CompoundTag data, EntityAccessor accessor) {
			if (!(accessor.getEntity() instanceof PokemonEntity pokemonEntity)) {
				return;
			}
			Pokemon pokemon = pokemonEntity.getPokemon();
			data.putInt(HUMOR, AffectionData.getHumor(pokemon));
			boolean wild = AffectionData.isWild(pokemon);
			data.putBoolean(WILD, wild);
			data.putInt(CONFIANZA, AffectionData.getConfianza(pokemon));
			data.putInt(CONFIANZA_MAX, AffectionData.getLvCaptura(pokemon));
			data.putInt(AMISTAD, pokemon.getFriendship());
		}

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			CompoundTag data = accessor.getServerData();
			if (!data.contains(HUMOR)) {
				return;
			}
			tooltip.add(Component.translatable(
					"tooltip.cobbledomestics.jade.humor",
					data.getInt(HUMOR),
					AffectionData.MAX_HUMOR));
			if (data.getBoolean(WILD)) {
				tooltip.add(Component.translatable(
						"tooltip.cobbledomestics.jade.confianza",
						data.getInt(CONFIANZA),
						data.getInt(CONFIANZA_MAX)));
			}
			tooltip.add(Component.translatable(
					"tooltip.cobbledomestics.jade.amistad",
					data.getInt(AMISTAD)));
		}

		@Override
		public ResourceLocation getUid() {
			return AFFECTION_UID;
		}
	}
}
