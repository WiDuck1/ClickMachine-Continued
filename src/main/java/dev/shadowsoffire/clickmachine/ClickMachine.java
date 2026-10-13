package dev.shadowsoffire.clickmachine;

import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dev.shadowsoffire.clickmachine.block.ClickMachineBlock;
import dev.shadowsoffire.clickmachine.block.ClickMachineTile;
import dev.shadowsoffire.clickmachine.gui.ClickMachineMenu;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import team.reborn.energy.api.EnergyStorage;

public class ClickMachine implements ModInitializer {
    public static final String MODID = "clickmachine";
    public static final Logger LOG = LogManager.getLogger(MODID);
    public static final Supplier<ClickMachineBlock> CLICK_MACHINE = registerBlock();
    public static final Supplier<BlockItem> CLICK_MACHINE_ITEM = registerItem();
    public static final Supplier<BlockEntityType<ClickMachineTile>> CLICK_MACHINE_TILE = registerTile();
    public static final Supplier<MenuType<ClickMachineMenu>> CLICK_MACHINE_MENU = registerMenu();

    private static Supplier<ClickMachineBlock> registerBlock() {
        var key = ResourceKey.create(Registries.BLOCK, loc("click_machine"));
        var block = Registry.register(BuiltInRegistries.BLOCK, key, new ClickMachineBlock(BlockBehaviour.Properties.of().setId(key)));
        return () -> block;
    }

    private static Supplier<BlockItem> registerItem() {
        var key = ResourceKey.create(Registries.ITEM, loc("click_machine"));
        var item = Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(CLICK_MACHINE.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));
        return () -> item;
    }

    private static Supplier<BlockEntityType<ClickMachineTile>> registerTile() {
        var type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, loc("click_machine"),
            FabricBlockEntityTypeBuilder.create(ClickMachineTile::new, CLICK_MACHINE.get()).build());
        return () -> type;
    }

    private static Supplier<MenuType<ClickMachineMenu>> registerMenu() {
        MenuType<ClickMachineMenu> type = Registry.register(BuiltInRegistries.MENU, loc("click_machine"),
            new ExtendedMenuType<>(ClickMachineMenu::new, BlockPos.STREAM_CODEC));
        return () -> type;
    }

    @Override
    public void onInitialize() {
        ItemStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getHandler(), CLICK_MACHINE_TILE.get());
        EnergyStorage.SIDED.registerForBlockEntity((tile, side) -> tile.getEnergy(), CLICK_MACHINE_TILE.get());
        PayloadTypeRegistry.clientboundPlay().register(ClickMachineConfig.ConfigPayload.TYPE, ClickMachineConfig.ConfigPayload.CODEC);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> output.accept(CLICK_MACHINE_ITEM.get()));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ClickMachineConfig.reload());
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
            ServerPlayNetworking.send(player, new ClickMachineConfig.ConfigPayload()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) {
                ClickMachineConfig.reload();
                for (var player : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(player, new ClickMachineConfig.ConfigPayload());
            }
        });
    }

    public static Identifier loc(String path) { return Identifier.fromNamespaceAndPath(MODID, path); }
}
