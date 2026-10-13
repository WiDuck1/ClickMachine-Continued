package dev.shadowsoffire.clickmachine.util;

import java.nio.file.Path;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.player.Player;

/** Ephemeral player data: machines must never read, redirect or save their owner's progress. */
public final class FakePlayerData {
    private FakePlayerData() {}

    public static ServerStatsCounter stats(ServerPlayer player) {
        // A directory, not a file, so vanilla does not read an existing stats file.
        return new ServerStatsCounter(player.level().getServer(), Path.of("")) {
            @Override public void save() {}
            @Override public void setValue(Player owner, Stat<?> stat, int value) {}
        };
    }

    public static PlayerAdvancements advancements(ServerPlayer player) {
        var server = player.level().getServer();
        return new PlayerAdvancements(server.getFixerUpper(), server.getPlayerList(), server.getAdvancements(), Path.of(""), player) {
            @Override public void save() {}
            @Override public boolean award(AdvancementHolder advancement, String criterion) { return false; }
            @Override public boolean revoke(AdvancementHolder advancement, String criterion) { return false; }
            @Override public void flushDirty(ServerPlayer owner, boolean showAdvancements) {}
            @Override public void setSelectedTab(AdvancementHolder advancement) {}
            @Override public AdvancementProgress getOrStartProgress(AdvancementHolder advancement) { return new AdvancementProgress(); }
            @Override public <T extends CriterionTriggerInstance> Map<TriggerInstanceKey, T> getTriggerMapForType(CriterionTrigger<T> trigger) { return Map.of(); }
        };
    }
}
