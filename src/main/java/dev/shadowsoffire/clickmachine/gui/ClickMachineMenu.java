package dev.shadowsoffire.clickmachine.gui;

import java.util.function.IntConsumer;
import dev.shadowsoffire.clickmachine.ClickMachine;
import dev.shadowsoffire.clickmachine.block.ClickMachineTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ClickMachineMenu extends AbstractContainerMenu {
    protected final ClickMachineTile tile;
    private IntConsumer speedListener = value -> {};

    public ClickMachineMenu(int id, Inventory inventory, BlockPos pos) {
        super(ClickMachine.CLICK_MACHINE_MENU.get(), id);
        if (!(inventory.player.level().getBlockEntity(pos) instanceof ClickMachineTile machine)) {
            throw new IllegalStateException("Missing Click Machine at " + pos);
        }
        this.tile = machine;
        machine.registerSlots(this::addDataSlot);
        this.addSlot(new Slot(machine.getHandler().asContainer(), 0, 8, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) { return machine.getHandler().asContainer().canPlaceItem(0, stack); }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 114 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) this.addSlot(new Slot(inventory, col, 8 + col * 18, 172));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack remaining = slot.getItem();
        ItemStack original = remaining.copy();
        // Placebo's old rules filled the main inventory before the hotbar.
        if (!this.moveItemStackTo(remaining, index == 0 ? 1 : 0, index == 0 ? this.slots.size() : 1, false)) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, remaining);
        if (remaining.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) { return !this.tile.isRemoved(); }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 2) this.tile.setSneaking(!this.tile.isSneaking());
        else if (button == 3) this.tile.setRightClicking(!this.tile.isRightClicking());
        else if (button >= 4 && button <= 12) this.tile.setSpeedIndex(button - 4);
        return button <= 12;
    }

    public void setSpeedListener(IntConsumer listener) { this.speedListener = listener; }

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        if (id == 1) speedListener.accept(value);
    }

    public boolean isSneaking() { return this.tile.isSneaking(); }
    public boolean isRightClicking() { return this.tile.isRightClicking(); }
    public int getEnergy() { return this.tile.getPower(); }
    public int getSpeedIdx() { return this.tile.getSpeedIndex(); }
}
