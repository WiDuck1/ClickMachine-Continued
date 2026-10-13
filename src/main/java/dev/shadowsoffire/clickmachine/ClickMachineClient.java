package dev.shadowsoffire.clickmachine;

import dev.shadowsoffire.clickmachine.gui.ClickMachineScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class ClickMachineClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(ClickMachine.CLICK_MACHINE_MENU.get(), ClickMachineScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(ClickMachineConfig.ConfigPayload.TYPE,
            (payload, context) -> ClickMachineConfig.apply(payload));
    }
}
