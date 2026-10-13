package dev.shadowsoffire.clickmachine.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.shadowsoffire.clickmachine.ClickMachine;
import dev.shadowsoffire.clickmachine.block.ClickMachineBlock;
import dev.shadowsoffire.clickmachine.block.ClickMachineTile;
import dev.shadowsoffire.clickmachine.gui.ClickMachineMenu;
import dev.shadowsoffire.clickmachine.gui.ClickMachineScreen;
import dev.shadowsoffire.clickmachine.gui.SpeedSlider;
import dev.shadowsoffire.clickmachine.gui.ClickerCheckboxButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Exercises a real client/server menu in an isolated disposable test world. */
@EventBusSubscriber(modid = ClickMachineGameTests.MODID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static boolean requested, menuRequested, buttonsSent;
    private static volatile BlockPos machinePos;
    private static int ticks, renderedTicks;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("clickmachine.clientSmoke")) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (++ticks > 1200) throw new IllegalStateException("Click Machine client menu smoke test timed out");
        if (!requested) {
            requested = true;
            var uuid = client.player.getUUID();
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                var level = player.level();
                var pos = player.blockPosition().above(8);
                level.setBlock(pos, ClickMachine.CLICK_MACHINE.get().defaultBlockState().setValue(ClickMachineBlock.FACING, Direction.EAST), 3);
                level.setBlock(pos.below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                var tile = (ClickMachineTile) level.getBlockEntity(pos);
                tile.getHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 32));
                tile.setPower(40000);
                machinePos = pos;
            });
        }
        // Opening immediately after creating a block can outrun its first chunk update.
        if (!menuRequested && machinePos != null && client.level.getBlockEntity(machinePos) instanceof ClickMachineTile) {
            menuRequested = true;
            var uuid = client.player.getUUID();
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                var pos = machinePos;
                player.openMenu(new SimpleMenuProvider((id, inv, owner) -> new ClickMachineMenu(id, inv, pos),
                    Component.translatable("block.clickmachine.click_machine")), buf -> buf.writeBlockPos(pos));
            });
        }
        if (!(client.gui.screen() instanceof ClickMachineScreen screen)) return;
        var menu = screen.getMenu();
        if (!buttonsSent && menu.getEnergy() == 40000 && menu.getSlot(0).getItem().getCount() == 32) {
            buttonsSent = true;
            for (var child : screen.children()) {
                if (child instanceof ClickerCheckboxButton checkbox) {
                    var click = mouse(checkbox.getX() + 10, checkbox.getY() + 10);
                    if (!screen.mouseClicked(click, false)) throw new IllegalStateException("Checkbox did not accept left click");
                    screen.mouseReleased(click);
                }
            }
            var slider = screen.children().stream().filter(SpeedSlider.class::isInstance).map(SpeedSlider.class::cast).findFirst().orElseThrow();
            var start = mouse(slider.getX() + 4, slider.getY() + 10);
            var end = mouse(slider.getX() + slider.getWidth() - 4, slider.getY() + 10);
            if (!screen.mouseClicked(start, false) || !screen.mouseDragged(end, end.x() - start.x(), 0)) {
                throw new IllegalStateException("Speed slider did not accept left-button drag");
            }
            screen.mouseReleased(end);
        }
        if (buttonsSent && menu.isSneaking() && !menu.isRightClicking() && menu.getSpeedIdx() == 8 && menu.getEnergy() == 40000) {
            if (screen.children().size() != 3) throw new IllegalStateException("Expected the speed slider and two checkboxes");
            if (++renderedTicks == 40) {
                ClickMachine.LOG.info("CLIENT SMOKE PASSED: menu rendered, inventory and 40000 RF synchronized, checkbox clicks and speed-slider drag applied");
                client.stop();
            }
        }
    }

    private static MouseButtonEvent mouse(double x, double y) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
    }
}
