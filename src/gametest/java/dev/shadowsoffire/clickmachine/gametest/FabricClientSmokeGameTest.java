package dev.shadowsoffire.clickmachine.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Creates a fresh integrated-server world, including PlayerList mixin application. */
public class FabricClientSmokeGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksRender();
            context.waitFor(client -> ClientSmokeTest.passed, 1200);
            context.takeScreenshot("click-machine-menu");
        }
    }
}
