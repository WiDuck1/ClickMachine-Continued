package dev.shadowsoffire.clickmachine.util;

import team.reborn.energy.api.base.SimpleEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/** Transactional energy storage retaining the original per-operation transfer limits. */
public class MachineEnergy extends SimpleEnergyStorage {
    private final Runnable changed;

    public MachineEnergy(int capacity, Runnable changed) {
        super(capacity, capacity, capacity);
        this.changed = changed;
    }

    public int getEnergyStored() { return (int) getAmount(); }
    public int getMaxEnergyStored() { return (int) getCapacity(); }

    public int extractEnergy(int amount, boolean simulate) {
        try (var transaction = Transaction.openOuter()) {
            int extracted = (int) extract(amount, transaction);
            if (!simulate) transaction.commit();
            return extracted;
        }
    }

    public int receiveEnergy(int amount, boolean simulate) {
        try (var transaction = Transaction.openOuter()) {
            int inserted = (int) insert(amount, transaction);
            if (!simulate) transaction.commit();
            return inserted;
        }
    }

    @Override
    protected void onFinalCommit() { changed.run(); }
}
