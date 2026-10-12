package dev.shadowsoffire.clickmachine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.common.primitives.Ints;
import dev.shadowsoffire.clickmachine.util.LegacyConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;

public final class ClickMachineConfig {
    public static final int[] DEFAULT_SPEEDS = { 500, 200, 100, 50, 20, 10, 5, 2, 1 };
    public static final int[] DEFAULT_COSTS = { 0, 3, 5, 10, 25, 50, 100, 250, 500 };
    public static int[] speeds = DEFAULT_SPEEDS.clone();
    public static boolean usesRF = false;
    public static int maxPowerStorage = 50000;
    public static int[] powerPerSpeed = DEFAULT_COSTS.clone();
    public static Set<Item> blacklistedItems = Set.of();

    public static void reload() {
        var file = FMLPaths.CONFIGDIR.get().resolve("clickmachine.cfg");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                try (var defaults = ClickMachineConfig.class.getResourceAsStream("/clickmachine-default.cfg")) {
                    if (defaults == null) throw new IOException("Missing default Click Machine config");
                    Files.copy(defaults, file);
                }
            }
            var values = LegacyConfig.parse(Files.readAllLines(file));
            int[] newSpeeds = LegacyConfig.integers(values.get("Speeds"), speeds, 1);
            int[] newCosts = LegacyConfig.integers(values.get("RF Costs"), powerPerSpeed, 0);
            int newMax = LegacyConfig.integers(values.get("Max Power Storage"), new int[] { maxPowerStorage }, 0)[0];
            boolean newUsesRF = Boolean.parseBoolean(values.getOrDefault("Uses RF", List.of(Boolean.toString(usesRF))).getFirst());
            Set<Item> blacklist = new HashSet<>();
            for (String name : values.getOrDefault("Item Blacklist", List.of("minecraft:bedrock"))) {
                var id = Identifier.tryParse(name);
                var item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
                if (item == null || item == Items.AIR) {
                    ClickMachine.LOG.error("Failed to parse unknown item \"{}\" in Click Machine Blacklist.", name);
                } else blacklist.add(item);
            }
            speeds = newSpeeds;
            powerPerSpeed = newCosts;
            maxPowerStorage = newMax;
            usesRF = newUsesRF;
            blacklistedItems = Set.copyOf(blacklist);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read " + file, ex);
        }
    }

    public static void apply(ConfigPayload msg) {
        speeds = Ints.toArray(msg.speeds());
        usesRF = msg.usesRF();
        maxPowerStorage = msg.maxPower();
        powerPerSpeed = Ints.toArray(msg.powerPerSpeed());
        blacklistedItems = Set.copyOf(msg.blacklist());
    }

    public record ConfigPayload(List<Integer> speeds, boolean usesRF, int maxPower, List<Integer> powerPerSpeed, Set<Item> blacklist) implements CustomPacketPayload {
        public static final Type<ConfigPayload> TYPE = new Type<>(ClickMachine.loc("config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ConfigPayload::speeds,
            ByteBufCodecs.BOOL, ConfigPayload::usesRF,
            ByteBufCodecs.INT, ConfigPayload::maxPower,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ConfigPayload::powerPerSpeed,
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.registry(Registries.ITEM)), ConfigPayload::blacklist,
            ConfigPayload::new);

        public ConfigPayload() {
            this(List.copyOf(Ints.asList(ClickMachineConfig.speeds)), ClickMachineConfig.usesRF, ClickMachineConfig.maxPowerStorage,
                List.copyOf(Ints.asList(ClickMachineConfig.powerPerSpeed)), ClickMachineConfig.blacklistedItems);
        }

        @Override
        public Type<ConfigPayload> type() { return TYPE; }
    }
}
