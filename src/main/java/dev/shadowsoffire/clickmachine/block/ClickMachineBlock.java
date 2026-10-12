package dev.shadowsoffire.clickmachine.block;

import dev.shadowsoffire.clickmachine.gui.ClickMachineMenu;
import dev.shadowsoffire.clickmachine.ClickMachine;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class ClickMachineBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ClickMachineBlock(BlockBehaviour.Properties properties) {
        super(properties.mapColor(MapColor.METAL).sound(SoundType.METAL).strength(5));
        this.registerDefaultState(this.defaultBlockState().setValue(ACTIVE, true));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new ClickMachineTile(pPos, pState);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    public BlockState rotate(BlockState state, LevelAccessor world, BlockPos pos, Rotation direction) {
        return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockEntity te = world.getBlockEntity(pos);
        if (!world.isClientSide() && te instanceof ClickMachineTile && placer instanceof Player) {
            ((ClickMachineTile) te).setPlayer((Player) placer);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player placer = context.getPlayer();
        Direction face = placer.getDirection().getOpposite();
        if (placer.getXRot() > 50) face = Direction.UP;
        else if (placer.getXRot() < -50) face = Direction.DOWN;
        return this.defaultBlockState().setValue(FACING, face);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        player.openMenu(new SimpleMenuProvider((id, inv, owner) -> new ClickMachineMenu(id, inv, pos),
            Component.translatable(this.getDescriptionId())), buf -> buf.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ClickMachine.CLICK_MACHINE_TILE.get()) return null;
        return (world, pos, blockState, entity) -> ((ClickMachineTile) entity).serverTick(world, pos, blockState);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    @Deprecated
    public int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos, Direction side) {
        BlockEntity te = pLevel.getBlockEntity(pPos);
        if (te instanceof ClickMachineTile) {
            ItemStack i = ((ClickMachineTile) te).held.getStackInSlot(0);
            if (i.isEmpty()) return 0;
            if (i.getMaxStackSize() == 1 && i.isDamageableItem()) {
                return Mth.floor(15F * (i.getMaxDamage() - i.getDamageValue()) / i.getMaxDamage());
            }
            else return Mth.floor(15F * i.getCount() / i.getMaxStackSize());
        }
        return 0;
    }

}
