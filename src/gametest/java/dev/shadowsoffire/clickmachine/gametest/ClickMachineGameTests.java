package dev.shadowsoffire.clickmachine.gametest;

import dev.shadowsoffire.clickmachine.block.ClickMachineBlock;
import dev.shadowsoffire.clickmachine.block.ClickMachineTile;
import dev.shadowsoffire.clickmachine.util.FakePlayerUtil.UsefulFakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;
import dev.shadowsoffire.clickmachine.ClickMachine;
import dev.shadowsoffire.clickmachine.ClickMachineConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.network.payload.AdvancedContainerSetDataPayload;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import dev.shadowsoffire.clickmachine.gui.ClickMachineMenu;

/** Loaded only by runGameTestServer; never included in the released mod. */
@Mod(ClickMachineGameTests.MODID)
public class ClickMachineGameTests {
    public static final String MODID = "clickmachine_tests";
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final List<String> NAMES = new ArrayList<>();

    public ClickMachineGameTests(IEventBus bus) {
        register("timing_and_energy", ClickMachineGameTests::timingAndEnergy);
        register("redstone_and_starvation", ClickMachineGameTests::redstoneAndStarvation);
        register("saved_state", ClickMachineGameTests::savedState);
        register("legacy_enchanted_tool", ClickMachineGameTests::legacyEnchantedTool);
        register("menu_and_comparator", ClickMachineGameTests::menuAndComparator);
        register("automation", ClickMachineGameTests::automation);
        register("right_click_placement", ClickMachineGameTests::rightClickPlacement);
        register("left_click_breaking", ClickMachineGameTests::leftClickBreaking);
        register("entity_attack_and_cleanup", ClickMachineGameTests::entityAttackAndCleanup);
        register("recipe", ClickMachineGameTests::recipe);
        register("facing_and_cooldowns", ClickMachineGameTests::facingAndCooldowns);
        register("machine_drops", ClickMachineGameTests::machineDrops);
        FUNCTIONS.register(bus);
        bus.addListener(this::tests);
    }

    private static Identifier loc(String name) { return Identifier.fromNamespaceAndPath(MODID, name); }

    private static void register(String name, Consumer<GameTestHelper> test) {
        NAMES.add(name);
        FUNCTIONS.register(name, () -> helper -> {
            int[] speeds = ClickMachineConfig.speeds;
            int[] costs = ClickMachineConfig.powerPerSpeed;
            boolean usesRF = ClickMachineConfig.usesRF;
            int max = ClickMachineConfig.maxPowerStorage;
            Set<net.minecraft.world.item.Item> blacklist = ClickMachineConfig.blacklistedItems;
            try {
                ClickMachineConfig.speeds = ClickMachineConfig.DEFAULT_SPEEDS.clone();
                ClickMachineConfig.powerPerSpeed = ClickMachineConfig.DEFAULT_COSTS.clone();
                ClickMachineConfig.maxPowerStorage = 50000;
                ClickMachineConfig.usesRF = false;
                ClickMachineConfig.blacklistedItems = Set.of(Items.BEDROCK);
                test.accept(helper);
                helper.succeed();
            } finally {
                ClickMachineConfig.speeds = speeds;
                ClickMachineConfig.powerPerSpeed = costs;
                ClickMachineConfig.usesRF = usesRF;
                ClickMachineConfig.maxPowerStorage = max;
                ClickMachineConfig.blacklistedItems = blacklist;
            }
        });
    }

    private void tests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(loc("environment"));
        for (String name : NAMES) {
            event.registerTest(loc(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, loc(name)),
                new TestData<>(environment, loc("empty"), 200, 0, true)));
        }
    }

    private static CountingMachine machine(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState state = ClickMachine.CLICK_MACHINE.get().defaultBlockState().setValue(ClickMachineBlock.FACING, Direction.EAST);
        helper.getLevel().setBlock(pos, state, 3);
        var tile = new CountingMachine(pos, state);
        helper.getLevel().setBlockEntity(tile);
        return tile;
    }

    private static void tick(GameTestHelper helper, ClickMachineTile tile) {
        tile.serverTick(helper.getLevel(), tile.getBlockPos(), helper.getLevel().getBlockState(tile.getBlockPos()));
    }

    private static void timingAndEnergy(GameTestHelper helper) {
        ClickMachineConfig.usesRF = true;
        var tile = machine(helper);
        for (int index = 0; index < 9; index++) {
            tile.setSpeedIndex(index);
            tile.setCounter(0);
            tile.clicks = 0;
            tile.setPower(50000);
            int ticks = 2 * ClickMachineConfig.speeds[index] + 1;
            for (int t = 0; t < ticks; t++) tick(helper, tile);
            helper.assertValueEqual(tile.clicks, 3, "clicks at speed " + index);
            helper.assertValueEqual(tile.getPower(), 50000 - ticks * ClickMachineConfig.powerPerSpeed[index], "energy charged per tick at speed " + index);
        }
        ClickMachineConfig.usesRF = false;
        tile.setPower(0);
        tile.setCounter(0);
        tile.clicks = 0;
        tick(helper, tile);
        helper.assertValueEqual(tile.clicks, 1, "first click without energy");
    }

    private static void redstoneAndStarvation(GameTestHelper helper) {
        ClickMachineConfig.usesRF = true;
        var tile = machine(helper);
        tile.setSpeedIndex(5);
        tile.setPower(50000);
        tick(helper, tile);
        int counter = tile.getCounter();
        int energy = tile.getPower();
        helper.getLevel().setBlock(tile.getBlockPos().below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        for (int t = 0; t < 12; t++) tick(helper, tile);
        helper.assertValueEqual(tile.getCounter(), counter, "redstone pauses timer");
        helper.assertValueEqual(tile.getPower(), energy, "redstone pauses energy");
        helper.assertTrue(!tile.getBlockState().getValue(ClickMachineBlock.ACTIVE), "redstone switches the block off");
        helper.getLevel().setBlock(tile.getBlockPos().below(), Blocks.AIR.defaultBlockState(), 3);
        tile.setPower(49);
        tick(helper, tile);
        helper.assertValueEqual(tile.getCounter(), counter, "insufficient energy pauses timer");
        helper.assertValueEqual(tile.getPower(), 49, "insufficient energy consumes nothing");
        helper.assertTrue(tile.getBlockState().getValue(ClickMachineBlock.ACTIVE), "unpowered redstone state remains active even when starved");
        tile.setPower(50);
        tick(helper, tile);
        helper.assertValueEqual(tile.getCounter(), counter + 1, "timer resumes without resetting");
        helper.assertValueEqual(tile.getPower(), 0, "one funded tick consumes the original RF cost");
    }

    private static void savedState(GameTestHelper helper) {
        var tile = machine(helper);
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        tile.setPlayer(owner);
        tile.setSpeedIndex(7);
        tile.setSneaking(true);
        tile.setRightClicking(false);
        tile.setCounter(9);
        tile.setPower(45000);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.setDamageValue(123);
        tile.getHandler().setStackInSlot(0, tool);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        tile.saveCustomOnly(output);
        var tag = output.buildResult();
        var input = TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag);
        helper.assertValueEqual(input.read("uuid", UUIDUtil.CODEC).orElseThrow(), owner.getGameProfile().id(), "original owner UUID tag");
        helper.assertTrue(input.child("inv").isPresent(), "original inventory tag");
        helper.assertValueEqual(input.getIntOr("counter", -1), 1, "saved counter retains original modulo");
        var copy = new CountingMachine(tile.getBlockPos(), tile.getBlockState());
        copy.loadCustomOnly(input);
        helper.assertValueEqual(copy.getSpeedIndex(), 7, "saved speed");
        helper.assertTrue(copy.isSneaking() && !copy.isRightClicking(), "saved click modes");
        helper.assertValueEqual(copy.getPower(), 45000, "saved energy");
        helper.assertTrue(ItemStack.matches(copy.getHandler().getStackInSlot(0), tool), "saved tool and damage");
        helper.assertValueEqual(copy.ownerProfile(), owner.getGameProfile(), "saved owner profile");
    }

    private static void legacyEnchantedTool(GameTestHelper helper) {
        var tile = machine(helper);
        net.minecraft.nbt.CompoundTag legacy;
        try {
            legacy = net.minecraft.nbt.TagParser.parseCompoundFully("{Size:1,Items:[{Slot:0b,id:\"minecraft:diamond_pickaxe\",count:1,components:{\"minecraft:damage\":123,\"minecraft:enchantments\":{levels:{\"minecraft:fortune\":3},show_in_tooltip:true}}}]}");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
            throw new IllegalStateException(ex);
        }
        tile.getHandler().deserialize(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacy));
        ItemStack item = tile.getHandler().getStackInSlot(0);
        helper.assertTrue(item.is(Items.DIAMOND_PICKAXE), "1.21.1 held enchanted tool survives loading");
        helper.assertValueEqual(item.getDamageValue(), 123, "1.21.1 held tool damage survives loading");
        var fortune = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        helper.assertValueEqual(item.getEnchantments().getLevel(fortune), 3, "1.21.1 held tool enchantments survive loading");
    }

    private static void menuAndComparator(GameTestHelper helper) {
        var tile = machine(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new ClickMachineMenu(1, player.getInventory(), tile.getBlockPos());
        tile.getHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        menu.quickMoveStack(player, 0);
        helper.assertTrue(tile.getHandler().getStackInSlot(0).isEmpty(), "shift click removes held stack");
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 64, "main inventory fills before hotbar");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "hotbar is not preferred");
        menu.quickMoveStack(player, 1);
        helper.assertValueEqual(tile.getHandler().getStackInSlot(0).getCount(), 64, "shift click returns the entire stack");
        menu.clickMenuButton(player, 2);
        menu.clickMenuButton(player, 3);
        menu.clickMenuButton(player, 12);
        helper.assertTrue(tile.isSneaking() && !tile.isRightClicking(), "menu toggle IDs remain unchanged");
        helper.assertValueEqual(tile.getSpeedIndex(), 8, "menu speed IDs remain unchanged");
        tile.setPower(50000);
        List<DataSlot> slots = new ArrayList<>();
        tile.registerSlots(slots::add);
        helper.assertValueEqual(slots.getFirst().get(), 50000, "energy data has full integer precision");
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            AdvancedContainerSetDataPayload.STREAM_CODEC.encode(buf, new AdvancedContainerSetDataPayload((byte) 1, (short) 0, 50000));
            helper.assertValueEqual(AdvancedContainerSetDataPayload.STREAM_CODEC.decode(buf).value(), 50000, "NeoForge menu packet retains full energy value");
        } finally { buf.release(); }
        tile.getHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 32));
        helper.assertValueEqual(tile.getBlockState().getAnalogOutputSignal(helper.getLevel(), tile.getBlockPos(), Direction.NORTH), 7, "stack comparator output");
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.setDamageValue(tool.getMaxDamage() / 2);
        tile.getHandler().setStackInSlot(0, tool);
        helper.assertValueEqual(tile.getBlockState().getAnalogOutputSignal(helper.getLevel(), tile.getBlockPos(), Direction.NORTH),
            (int) Math.floor(15F * (tool.getMaxDamage() - tool.getDamageValue()) / tool.getMaxDamage()), "tool comparator output");
    }

    private static void automation(GameTestHelper helper) {
        var tile = machine(helper);
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, tile.getBlockPos(), side) == tile.getHandler(), "item capability on " + side);
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.Energy.BLOCK, tile.getBlockPos(), side) == tile.getEnergy(), "energy capability on " + side);
        }
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(tile.getHandler().insert(0, ItemResource.of(Items.BEDROCK), 1, transaction), 0, "blacklist rejects insertion");
            helper.assertValueEqual(tile.getHandler().insert(0, ItemResource.of(Items.COBBLESTONE), 100, transaction), 64, "original stack limit");
        }
        helper.assertTrue(tile.getHandler().getStackInSlot(0).isEmpty(), "aborted transfer changes nothing");
        try (var transaction = Transaction.openRoot()) {
            tile.getHandler().insert(0, ItemResource.of(Items.COBBLESTONE), 64, transaction);
            transaction.commit();
        }
        helper.assertValueEqual(tile.getHandler().getStackInSlot(0).getCount(), 64, "committed transfer persists");
        helper.assertValueEqual(tile.getEnergy().receiveEnergy(60000, true), 50000, "original max energy input");
        helper.assertValueEqual(tile.getPower(), 0, "simulated energy transfer changes nothing");
        tile.getEnergy().receiveEnergy(60000, false);
        helper.assertValueEqual(tile.getPower(), 50000, "committed energy transfer");
    }

    private static void rightClickPlacement(GameTestHelper helper) {
        var tile = machine(helper);
        var support = tile.getBlockPos().relative(Direction.EAST, 2);
        helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        tile.getHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 2));
        tick(helper, tile);
        helper.assertTrue(helper.getLevel().getBlockState(tile.getBlockPos().east()).is(Blocks.COBBLESTONE), "right click places exactly one block");
        helper.assertValueEqual(tile.getHandler().getStackInSlot(0).getCount(), 1, "placement consumes exactly one held item");
        helper.assertTrue(tile.fakePlayer().getInventory().isEmpty(), "fake inventory is cleared after use");
    }

    private static void leftClickBreaking(GameTestHelper helper) {
        var tile = machine(helper);
        tile.setRightClicking(false);
        var target = tile.getBlockPos().relative(Direction.EAST, 2);
        helper.getLevel().setBlock(target, Blocks.SLIME_BLOCK.defaultBlockState(), 3);
        tick(helper, tile);
        helper.assertTrue(helper.getLevel().getBlockState(target).isAir(), "left click breaks instant-break blocks");
    }

    private static void entityAttackAndCleanup(GameTestHelper helper) {
        var tile = machine(helper);
        tile.setRightClicking(false);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        tile.getHandler().setStackInSlot(0, sword);
        var cow = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        cow.setNoAi(true);
        cow.snapTo(tile.getBlockPos().getX() + 2.5, tile.getBlockPos().getY(), tile.getBlockPos().getZ() + 0.5, 0, 0);
        helper.getLevel().addFreshEntity(cow);
        float health = cow.getHealth();
        tick(helper, tile);
        helper.assertTrue(cow.getHealth() < health, "left click attacks entities");
        helper.assertTrue(tile.getHandler().getStackInSlot(0).getDamageValue() > 0, "held sword receives normal durability damage");
        helper.assertTrue(tile.fakePlayer().getInventory().isEmpty(), "attack cleans up fake inventory");
        helper.assertTrue(!tile.fakePlayer().isShiftKeyDown(), "attack clears sneaking");
        helper.assertValueEqual(tile.fakePlayer().getAttributeValue(Attributes.ATTACK_DAMAGE),
            tile.fakePlayer().getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue(), "held attribute modifiers are removed after attack");
    }

    private static void recipe(GameTestHelper helper) {
        var key = ResourceKey.create(Registries.RECIPE, ClickMachine.loc("click_machine"));
        var recipe = (ShapedRecipe) helper.getLevel().getServer().getRecipeManager().byKey(key).orElseThrow().value();
        var input = CraftingInput.of(3, 3, List.of(
            new ItemStack(Items.DIORITE), new ItemStack(Items.DIORITE), new ItemStack(Items.DIORITE),
            new ItemStack(Items.DIORITE), new ItemStack(Items.CHORUS_FLOWER), new ItemStack(Items.DIORITE),
            new ItemStack(Items.DIORITE), new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(Items.DIORITE)));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "original crafting ingredients and pattern");
        var result = recipe.assemble(input);
        helper.assertTrue(result.is(ClickMachine.CLICK_MACHINE_ITEM.get()) && result.getCount() == 1, "original crafting result");
    }

    private static void facingAndCooldowns(GameTestHelper helper) {
        var tile = machine(helper);
        tick(helper, tile);
        helper.assertValueEqual(tile.fakePlayer().getEyeHeight(), 0F, "original fake player eye height");
        for (Direction direction : Direction.values()) {
            dev.shadowsoffire.clickmachine.util.FakePlayerUtil.setupFakePlayerForUse(tile.fakePlayer(), tile.getBlockPos(), direction, ItemStack.EMPTY, true);
            var look = tile.fakePlayer().getLookAngle();
            helper.assertTrue(Math.abs(look.x - direction.getStepX()) < 0.00001
                && Math.abs(look.y - direction.getStepY()) < 0.00001
                && Math.abs(look.z - direction.getStepZ()) < 0.00001, "look direction " + direction);
            helper.assertTrue(tile.fakePlayer().isShiftKeyDown(), "sneaking mode on " + direction);
        }
        var pearl = new ItemStack(Items.ENDER_PEARL);
        tile.fakePlayer().getCooldowns().addCooldown(pearl, 3);
        helper.getLevel().setBlock(tile.getBlockPos().below(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        for (int i = 0; i < 3; i++) tick(helper, tile);
        helper.assertTrue(!tile.fakePlayer().getCooldowns().isOnCooldown(pearl), "item cooldowns tick even when redstone disables clicking");
    }

    private static void machineDrops(GameTestHelper helper) {
        var tile = machine(helper);
        var pos = tile.getBlockPos();
        tile.getHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 32));
        helper.getLevel().destroyBlock(pos, true);
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1));
        int held = drops.stream().filter(item -> item.getItem().is(Items.COBBLESTONE)).mapToInt(item -> item.getItem().getCount()).sum();
        int blocks = drops.stream().filter(item -> item.getItem().is(ClickMachine.CLICK_MACHINE_ITEM.get())).mapToInt(item -> item.getItem().getCount()).sum();
        helper.assertValueEqual(held, 32, "breaking the machine drops held items exactly once");
        helper.assertValueEqual(blocks, 1, "breaking the machine drops the block exactly once");
    }

    private static class CountingMachine extends ClickMachineTile {
        int clicks;
        int getCounter() { return this.counter; }
        void setCounter(int value) { this.counter = value; }
        UsefulFakePlayer fakePlayer() { return this.player; }
        GameProfile ownerProfile() { return this.profile; }
        CountingMachine(BlockPos pos, BlockState state) { super(pos, state); }
        @Override
        public void accept(ItemStack stack) { super.accept(stack); clicks++; }
    }
}
