package dev.shadowsoffire.clickmachine.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import dev.shadowsoffire.clickmachine.ClickMachineConfig;
import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

/** One slot with the original stack limit and save format. */
public class MachineInventory extends SingleStackStorage {
    private final Runnable changed;

    public MachineInventory(Runnable changed) {
        this.changed = changed;
    }

    private ItemStack stack = ItemStack.EMPTY;

    @Override
    public int getSlotCount() { return 1; }
    @Override
    public MachineInventory getSlot(int slot) { checkSlot(slot); return this; }
    @Override
    protected ItemStack getStack() { return stack; }
    @Override
    protected void setStack(ItemStack stack) { this.stack = stack; }
    @Override
    protected void onFinalCommit() { changed.run(); }

    public ItemStack getStackInSlot(int slot) { checkSlot(slot); return stack; }
    private static void checkSlot(int slot) { if (slot != 0) throw new IndexOutOfBoundsException(slot); }

    public void setStackInSlot(int slot, ItemStack stack) {
        checkSlot(slot);
        this.stack = stack;
        changed.run();
    }

    @Override
    protected boolean canInsert(ItemVariant resource) { return container.canPlaceItem(0, resource.toStack()); }
    @Override
    protected int getCapacity(ItemVariant resource) { return resource.isBlank() ? 64 : Math.min(64, super.getCapacity(resource)); }

    private final Container container = new Container() {
        @Override
        public int getContainerSize() { return 1; }
        @Override
        public boolean isEmpty() { return stack.isEmpty(); }
        @Override
        public ItemStack getItem(int slot) { return getStackInSlot(slot); }
        @Override
        public ItemStack removeItem(int slot, int amount) {
            checkSlot(slot);
            ItemStack removed = stack.split(amount);
            if (!removed.isEmpty()) changed.run();
            return removed;
        }
        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            checkSlot(slot);
            ItemStack removed = stack;
            stack = ItemStack.EMPTY;
            return removed;
        }
        @Override
        public void setItem(int slot, ItemStack item) { MachineInventory.this.setStackInSlot(slot, item); }
        @Override
        public void setChanged() { changed.run(); }
        @Override
        public boolean stillValid(Player player) { return true; }
        @Override
        public boolean canPlaceItem(int slot, ItemStack item) { return !ClickMachineConfig.blacklistedItems.contains(item.getItem()); }
        @Override
        public void clearContent() { MachineInventory.this.setStackInSlot(0, ItemStack.EMPTY); }

    };

    public Container asContainer() { return container; }

    public void serialize(ValueOutput output) {
        output.putInt("Size", 1);
        output.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
        var items = output.list("Items", ItemStackWithSlot.CODEC);
        if (!getStackInSlot(0).isEmpty()) items.add(new ItemStackWithSlot(0, getStackInSlot(0)));
    }

    public void deserialize(ValueInput input) {
        setStackInSlot(0, ItemStack.EMPTY);
        // Placebo's 1.21.1 inventory had no version marker, and Minecraft's world
        // fixer does not visit this mod-specific nested inventory automatically.
        int version = input.getIntOr("DataVersion", 3955);
        int current = SharedConstants.getCurrentVersion().dataVersion().version();
        Codec<ItemStackWithSlot> codec = version >= current ? ItemStackWithSlot.CODEC : new Codec<>() {
            @Override
            public <T> DataResult<Pair<ItemStackWithSlot, T>> decode(DynamicOps<T> ops, T data) {
                Dynamic<T> fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(ops, data), version, current);
                return ItemStackWithSlot.CODEC.decode(ops, fixed.getValue());
            }

            @Override
            public <T> DataResult<T> encode(ItemStackWithSlot item, DynamicOps<T> ops, T prefix) {
                return ItemStackWithSlot.CODEC.encode(item, ops, prefix);
            }
        };
        input.listOrEmpty("Items", codec).forEach(item -> {
            if (item.slot() == 0) setStackInSlot(0, item.stack());
        });
    }
}
