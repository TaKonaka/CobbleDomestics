package cobbledomestics.item;

import java.util.List;

import com.cobblemon.mod.common.api.types.ElementalType;

import cobbledomestics.haba.HabaColor;
import cobbledomestics.haba.HabaTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;

public class HabaItem extends Item {
	private final HabaTier tier;
	private final HabaColor color;

	public HabaItem(HabaTier tier, HabaColor color) {
		super(new Item.Properties().stacksTo(64));
		this.tier = tier;
		this.color = color;
	}

	public HabaTier getTier() {
		return tier;
	}

	/** Null for Arcoíris (no type bonus). */
	public HabaColor getColor() {
		return color;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (!FMLEnvironment.dist.isClient()) {
			return;
		}
		if (!cobbledomestics.client.ClientInput.hasShiftDown()) {
			tooltip.add(Component.translatable("tooltip.cobbledomestics.haba.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		tooltip.add(Component.translatable("tooltip.cobbledomestics.haba.compatible").withStyle(ChatFormatting.GRAY));
		if (color == null) {
			tooltip.add(Component.literal(" ").append(
					Component.translatable("tooltip.cobbledomestics.haba.compatible_all").withStyle(ChatFormatting.AQUA)));
			return;
		}
		for (ElementalType type : color.matchingTypes()) {
			MutableComponent name = type.getDisplayName().copy()
					.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(type.getHue())));
			tooltip.add(Component.literal(" ").append(name));
		}
	}
}
