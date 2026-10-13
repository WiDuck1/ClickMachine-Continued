package dev.shadowsoffire.clickmachine.mixin;

import dev.shadowsoffire.clickmachine.util.FakePlayerData;
import dev.shadowsoffire.clickmachine.util.FakePlayerUtil.UsefulFakePlayer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bypass the owner's cached data while vanilla constructs a machine player. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "getPlayerStats(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/stats/ServerStatsCounter;", at = @At("HEAD"), cancellable = true)
    private void clickmachine$fakeStats(Player player, CallbackInfoReturnable<ServerStatsCounter> callback) {
        if (player instanceof UsefulFakePlayer fake) callback.setReturnValue(FakePlayerData.stats(fake));
    }

    @Inject(method = "getPlayerAdvancements(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/server/PlayerAdvancements;", at = @At("HEAD"), cancellable = true)
    private void clickmachine$fakeAdvancements(ServerPlayer player, CallbackInfoReturnable<PlayerAdvancements> callback) {
        if (player instanceof UsefulFakePlayer) callback.setReturnValue(FakePlayerData.advancements(player));
    }
}
