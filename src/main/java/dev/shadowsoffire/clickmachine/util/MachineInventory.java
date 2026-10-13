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
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

/** One slot with the original stack limit and save format. */
public class MachineInventory extends ItemStacksResourceHandler {
    private final Runnable changed;

    public MachineInventory(Runnable changed) {
        super(1);
        this.changed = changed;
    }

    public ItemStack getStackInSlot(int slot) { return stacks.get(slot); }

    public void setStackInSlot(int slot, ItemStack stack) {
        set(slot, ItemResource.of(stack), stack.getCount());
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return !ClickMachineConfig.blacklistedItems.contains(resource.getItem());
    }

    @Override
    protected int getCapacity(int slot, ItemResource resource) {
        return resource.isEmpty() ? 64 : Math.min(64, resource.getMaxStackSize());
    }

    @Override
    protected void onContentsChanged(int slot, ItemStack previous) { changed.run(); }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("Size", 1);
        output.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
        var items = output.list("Items", ItemStackWithSlot.CODEC);
        if (!getStackInSlot(0).isEmpty()) items.add(new ItemStackWithSlot(0, getStackInSlot(0)));
    }

    @Override
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
