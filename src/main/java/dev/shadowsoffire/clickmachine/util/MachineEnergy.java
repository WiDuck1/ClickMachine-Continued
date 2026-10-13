package dev.shadowsoffire.clickmachine.util;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Transactional energy storage retaining the original per-operation transfer limits. */
public class MachineEnergy extends SimpleEnergyHandler {
    private final Runnable changed;

    public MachineEnergy(int capacity, Runnable changed) {
        super(capacity);
        this.changed = changed;
    }

    public int getEnergyStored() { return getAmountAsInt(); }
    public int getMaxEnergyStored() { return getCapacityAsInt(); }

    public int extractEnergy(int amount, boolean simulate) {
        try (var transaction = Transaction.openRoot()) {
            int extracted = extract(amount, transaction);
            if (!simulate) transaction.commit();
            return extracted;
        }
    }

    public int receiveEnergy(int amount, boolean simulate) {
        try (var transaction = Transaction.openRoot()) {
            int inserted = insert(amount, transaction);
            if (!simulate) transaction.commit();
            return inserted;
        }
    }

    @Override
    protected void onEnergyChanged(int previous) { changed.run(); }
}
