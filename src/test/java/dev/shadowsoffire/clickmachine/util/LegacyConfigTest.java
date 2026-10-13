package dev.shadowsoffire.clickmachine.util;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyConfigTest {
    @Test
    void readsExistingQuotedNamesListsAndComments() {
        var parsed = LegacyConfig.parse(List.of(
            "# Existing Placebo config", "general {", "S:Speeds <", "500", "200", "100", "50", "20", "10", "5", "2", "1", ">",
            "B:\"Uses RF\"=true", "I:\"Max Power Storage\"=120000", "S:\"Item Blacklist\" <", "minecraft:bedrock", "minecraft:diamond", ">", "}"));
        assertArrayEquals(new int[] {500, 200, 100, 50, 20, 10, 5, 2, 1}, LegacyConfig.integers(parsed.get("Speeds"), new int[9], 1));
        assertEquals(List.of("true"), parsed.get("Uses RF"));
        assertEquals(List.of("120000"), parsed.get("Max Power Storage"));
        assertEquals(List.of("minecraft:bedrock", "minecraft:diamond"), parsed.get("Item Blacklist"));
    }

    @Test
    void fallsBackPerEntryWithoutAcceptingZeroSpeedOrNegativeCosts() {
        assertArrayEquals(new int[] {4, 200, 100, 50}, LegacyConfig.integers(List.of("4", "invalid", "0"), new int[] {500, 200, 100, 50}, 1));
        assertArrayEquals(new int[] {0, 3}, LegacyConfig.integers(List.of("0", "-5"), new int[] {0, 3}, 0));
        assertArrayEquals(new int[] {500, 200}, LegacyConfig.integers(null, new int[] {500, 200}, 1));
    }

    @Test
    void ignoresOtherCategoriesAndPreservesAnEmptyBlacklist() {
        var parsed = LegacyConfig.parse(List.of("other {", "B:\"Uses RF\"=true", "}", "general {", "S:\"Item Blacklist\" <", ">", "}"));
        assertFalse(parsed.containsKey("Uses RF"));
        assertEquals(List.of(), parsed.get("Item Blacklist"));
    }

    @Test
    void rejectsUnterminatedListsRatherThanSilentlyLosingSettings() {
        assertThrows(IllegalArgumentException.class, () -> LegacyConfig.parse(List.of("general {", "S:Speeds <", "500")));
    }
}
