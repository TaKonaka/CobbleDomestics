package cobbledomestics.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Candle-like stackable soap bars (1–4 per block).
 */
public class SoapBlock extends Block implements SimpleWaterloggedBlock {
	public static final MapCodec<SoapBlock> CODEC = simpleCodec(SoapBlock::new);
	public static final int MAX_SOAP = 4;
	public static final IntegerProperty SOAPS = IntegerProperty.create("soaps", 1, MAX_SOAP);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape SHAPE_1 = Block.box(6.0, 0.0, 6.0, 10.0, 4.0, 10.0);
	private static final VoxelShape SHAPE_2 = Block.box(3.0, 0.0, 3.0, 13.0, 4.0, 13.0);
	private static final VoxelShape SHAPE_3 = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
	private static final VoxelShape SHAPE_4 = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);

	public SoapBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(SOAPS, 1).setValue(WATERLOGGED, false));
	}

	public SoapBlock() {
		this(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_YELLOW)
				.instabreak()
				.noOcclusion()
				.sound(net.minecraft.world.level.block.SoundType.WOOL));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SOAPS, WATERLOGGED);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(SOAPS)) {
			case 1 -> SHAPE_1;
			case 2 -> SHAPE_2;
			case 3 -> SHAPE_3;
			default -> SHAPE_4;
		};
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
		if (existing.is(this)) {
			return existing.setValue(SOAPS, Math.min(MAX_SOAP, existing.getValue(SOAPS) + 1));
		}
		FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
		return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
	}

	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		return !context.isSecondaryUseActive()
				&& context.getItemInHand().is(this.asItem())
				&& state.getValue(SOAPS) < MAX_SOAP
				|| super.canBeReplaced(state, context);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.is(this.asItem()) || state.getValue(SOAPS) >= MAX_SOAP) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!level.isClientSide) {
			level.setBlock(pos, state.setValue(SOAPS, state.getValue(SOAPS) + 1), Block.UPDATE_ALL);
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
			level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOOL_PLACE,
					net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
			LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
}
