package dev.shadowsoffire.clickmachine;

import java.util.Set;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dev.shadowsoffire.clickmachine.block.ClickMachineBlock;
import dev.shadowsoffire.clickmachine.block.ClickMachineTile;
import dev.shadowsoffire.clickmachine.data.ClickRecipes;
import dev.shadowsoffire.clickmachine.data.LootProvider;
import dev.shadowsoffire.clickmachine.gui.ClickMachineMenu;
import dev.shadowsoffire.clickmachine.util.FakePlayerUtil.UsefulFakePlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ClickMachine.MODID)
public class ClickMachine {
    public static final String MODID = "clickmachine";
    public static final Logger LOG = LogManager.getLogger(MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredBlock<ClickMachineBlock> CLICK_MACHINE = BLOCKS.registerBlock("click_machine", ClickMachineBlock::new);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CLICK_MACHINE_ITEM = ITEMS.registerSimpleBlockItem(CLICK_MACHINE);
    public static final Supplier<BlockEntityType<ClickMachineTile>> CLICK_MACHINE_TILE = BLOCK_ENTITIES.register("click_machine",
        () -> new BlockEntityType<>(ClickMachineTile::new, Set.of(CLICK_MACHINE.get())));
    public static final Supplier<MenuType<ClickMachineMenu>> CLICK_MACHINE_MENU = MENUS.register("click_machine",
        () -> IMenuTypeExtension.create((id, inv, buf) -> new ClickMachineMenu(id, inv, buf.readBlockPos())));

    public ClickMachine(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(this::caps);
        bus.addListener(this::network);
        bus.addListener(this::creativeTab);
        bus.addListener(this::data);
        NeoForge.EVENT_BUS.register(new GameBusEvents());
    }

    private void caps(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, CLICK_MACHINE_TILE.get(), (be, side) -> be.getEnergy());
        event.registerBlockEntity(Capabilities.Item.BLOCK, CLICK_MACHINE_TILE.get(), (be, side) -> be.getHandler());
    }

    private void network(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(ClickMachineConfig.ConfigPayload.TYPE, ClickMachineConfig.ConfigPayload.CODEC,
            (msg, ctx) -> ClickMachineConfig.apply(msg));
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) event.accept(CLICK_MACHINE_ITEM);
    }

    private void data(GatherDataRegistryEntriesEvent event) {
        event.add(RecipeProvider.asBootstrap(ClickRecipes::new));
        event.add(Registries.LOOT_TABLE, new LootProvider());
    }

    public static Identifier loc(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private static class GameBusEvents {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void blockJoin(EntityJoinLevelEvent event) {
            if (event.getEntity() instanceof UsefulFakePlayer) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void dims(EntityEvent.Size event) {
            if (event.getEntity() instanceof UsefulFakePlayer) event.setNewSize(event.getNewSize().withEyeHeight(0));
        }

        @SubscribeEvent
        public void reloads(AddServerReloadListenersEvent event) {
            event.addListener(loc("config"), new SimplePreparableReloadListener<Void>() {
                @Override
                protected Void prepare(ResourceManager resources, ProfilerFiller profiler) { return null; }
                @Override
                protected void apply(Void unused, ResourceManager resources, ProfilerFiller profiler) {
                    ClickMachineConfig.reload();
                }
            });
        }

        @SubscribeEvent
        public void sync(OnDatapackSyncEvent event) {
            var payload = new ClickMachineConfig.ConfigPayload();
            if (event.getPlayer() != null) PacketDistributor.sendToPlayer(event.getPlayer(), payload);
            else PacketDistributor.sendToAllPlayers(payload);
        }
    }
}
