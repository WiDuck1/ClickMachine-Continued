package dev.shadowsoffire.clickmachine.block;

import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import dev.shadowsoffire.clickmachine.ClickMachine;
import dev.shadowsoffire.clickmachine.ClickMachineConfig;
import dev.shadowsoffire.clickmachine.util.FakePlayerUtil;
import dev.shadowsoffire.clickmachine.util.FakePlayerUtil.UsefulFakePlayer;
import dev.shadowsoffire.clickmachine.util.MachineEnergy;
import dev.shadowsoffire.clickmachine.util.MachineInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ClickMachineTile extends BlockEntity implements Consumer<ItemStack> {

    public static final GameProfile DEFAULT_CLICKER = new GameProfile(UUID.fromString("36f373ac-29ef-4150-b664-e7e6006efcd8"), "[The Click Machine]");

    protected MachineInventory held;
    protected final MachineEnergy power = new MachineEnergy(ClickMachineConfig.maxPowerStorage, this::setChanged);
    protected int speedIdx = 0;
    protected boolean sneak = false;
    protected boolean rightClick = true;
    protected int counter = 0;

    protected transient GameProfile profile;
    protected transient UsefulFakePlayer player;

    public ClickMachineTile(BlockPos pos, BlockState state) {
        super(ClickMachine.CLICK_MACHINE_TILE.get(), pos, state);
        this.held = new MachineInventory(this::setChanged);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (this.player == null) {
            this.player = FakePlayerUtil.createPlayer(level, this.profile != null ? this.profile : DEFAULT_CLICKER);
        }
        if (!level.hasNeighborSignal(this.worldPosition)) {
            int use = ClickMachineConfig.usesRF ? ClickMachineConfig.powerPerSpeed[this.getSpeedIndex()] : 0;
            if (this.power.extractEnergy(use, true) == use) {
                this.power.extractEnergy(use, false);
                if (this.counter++ % this.getSpeed() == 0) {
                    Direction facing = level.getBlockState(this.worldPosition).getValue(ClickMachineBlock.FACING);
                    FakePlayerUtil.setupFakePlayerForUse(this.getPlayer(), this.worldPosition, facing, this.held.getStackInSlot(0).copy(), this.sneak);
                    ItemStack result = this.held.getStackInSlot(0);
                    if (this.rightClick) result = FakePlayerUtil.rightClickInDirection(this.getPlayer(), this.level, this.worldPosition, facing, level.getBlockState(this.worldPosition));
                    else result = FakePlayerUtil.leftClickInDirection(this.getPlayer(), this.level, this.worldPosition, facing, level.getBlockState(this.worldPosition));
                    FakePlayerUtil.cleanupFakePlayerFromUse(this.getPlayer(), result, this.held.getStackInSlot(0), this);
                    this.setChanged();
                }
            }
            if (!state.getValue(ClickMachineBlock.ACTIVE)) {
                level.setBlock(this.worldPosition, state.setValue(ClickMachineBlock.ACTIVE, true), 2);
            }
        }
        else {
            if (state.getValue(ClickMachineBlock.ACTIVE)) {
                level.setBlock(this.worldPosition, state.setValue(ClickMachineBlock.ACTIVE, false), 2);
            }
        }

        this.player.getCooldowns().tick();
    }

    public void setPlayer(Player player) {
        this.profile = player.getGameProfile();
        this.setChanged();
    }

    UsefulFakePlayer getPlayer() {
        return this.player;
    }

    public MachineInventory getHandler() {
        return this.held;
    }

    public MachineEnergy getEnergy() {
        return this.power;
    }

    public int getSpeed() {
        return ClickMachineConfig.speeds[this.getSpeedIndex()];
    }

    public int getSpeedIndex() {
        return this.speedIdx;
    }

    public void setSpeedIndex(int speedIdx) {
        this.speedIdx = Math.clamp(speedIdx, 0, 8);
        this.setChanged();
    }

    public boolean isSneaking() {
        return this.sneak;
    }

    public void setSneaking(boolean sneak) {
        this.sneak = sneak;
        this.setChanged();
    }

    public boolean isRightClicking() {
        return this.rightClick;
    }

    public void setRightClicking(boolean rightClick) {
        this.rightClick = rightClick;
        this.setChanged();
    }

    static final String tagUUID = "uuid";
    static final String tagName = "name";
    static final String tagCounter = "counter";
    static final String tagSpeed = "speed_index";
    static final String tagSneak = "sneak";
    static final String tagRightClick = "right_click";
    static final String tagHandler = "inv";
    static final String tagEnergy = "fe";

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.profile != null) {
            output.store(tagUUID, UUIDUtil.CODEC, this.profile.id());
            output.putString(tagName, this.profile.name());
        }
        this.held.serialize(output.child(tagHandler));
        output.putInt(tagCounter, this.counter % this.getSpeed());
        this.writeSyncData(output);
    }

    private void writeSyncData(ValueOutput output) {
        output.putInt(tagSpeed, this.getSpeedIndex());
        output.putBoolean(tagSneak, this.sneak);
        output.putBoolean(tagRightClick, this.rightClick);
        output.putInt(tagEnergy, this.power.getEnergyStored());
    }

    private void readSyncData(ValueInput input) {
        this.setSpeedIndex(input.getIntOr(tagSpeed, 0));
        this.sneak = input.getBooleanOr(tagSneak, false);
        this.rightClick = input.getBooleanOr(tagRightClick, false);
        this.setPower(input.getIntOr(tagEnergy, 0));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read(tagUUID, UUIDUtil.CODEC).ifPresent(uuid -> {
            this.profile = new GameProfile(uuid, input.getStringOr(tagName, DEFAULT_CLICKER.name()));
        });
        this.held.deserialize(input.childOrEmpty(tagHandler));
        this.counter = input.getIntOr(tagCounter, 0);
        this.readSyncData(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        writeSyncData(output);
        return output.buildResult();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null && !this.level.isClientSide()) {
            Block.popResource(this.level, pos, this.held.getStackInSlot(0));
        }
        super.preRemoveSideEffects(pos, state);
    }

    public int getPower() {
        return this.power.getEnergyStored();
    }

    public void setPower(int energy) {
        this.power.extractEnergy(this.power.getMaxEnergyStored(), false);
        this.power.receiveEnergy(energy, false);
        this.setChanged();
    }

    @Override
    public void accept(ItemStack s) {
        this.held.setStackInSlot(0, s);
    }

    public void registerSlots(Consumer<DataSlot> consumer) {
        // Vanilla menu packets carry signed shorts; transmit energy as two halves.
        // Keep the speed/toggle data indices and all control IDs unchanged.
        int[] syncedPower = { this.getPower() };
        consumer.accept(slot(() -> this.getPower() & 0xFFFF, value -> {
            syncedPower[0] = (syncedPower[0] & 0xFFFF0000) | (value & 0xFFFF);
            this.setPower(syncedPower[0]);
        }));
        consumer.accept(slot(this::getSpeedIndex, this::setSpeedIndex));
        consumer.accept(slot(() -> this.sneak ? 1 : 0, value -> this.sneak = value == 1));
        consumer.accept(slot(() -> this.rightClick ? 1 : 0, value -> this.rightClick = value == 1));
        consumer.accept(slot(() -> this.getPower() >>> 16, value -> {
            syncedPower[0] = (syncedPower[0] & 0xFFFF) | ((value & 0xFFFF) << 16);
            this.setPower(syncedPower[0]);
        }));
    }

    private static DataSlot slot(java.util.function.IntSupplier getter, java.util.function.IntConsumer setter) {
        return new DataSlot() {
            @Override
            public int get() { return getter.getAsInt(); }
            @Override
            public void set(int value) { setter.accept(value); }
        };
    }
}
