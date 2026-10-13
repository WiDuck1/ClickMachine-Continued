package dev.shadowsoffire.clickmachine.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads the existing Click Machine .cfg format without depending on Placebo. */
public final class LegacyConfig {
    private LegacyConfig() {}

    public static Map<String, List<String>> parse(List<String> lines) {
        Map<String, List<String>> values = new HashMap<>();
        String listName = null;
        List<String> list = null;
        boolean general = false;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (listName != null) {
                if (line.equals(">")) {
                    values.put(listName, List.copyOf(list));
                    listName = null;
                } else list.add(unquote(line));
                continue;
            }
            if (line.equals("general {")) { general = true; continue; }
            if (line.equals("}")) { general = false; continue; }
            if (!general || line.length() < 3 || line.charAt(1) != ':') continue;
            int equals = line.indexOf('=');
            if (equals >= 0) {
                values.put(unquote(line.substring(2, equals).trim()), List.of(unquote(line.substring(equals + 1).trim())));
            } else if (line.endsWith("<")) {
                listName = unquote(line.substring(2, line.length() - 1).trim());
                list = new ArrayList<>();
            }
        }
        if (listName != null) throw new IllegalArgumentException("Unterminated config list: " + listName);
        return values;
    }

    public static int[] integers(List<String> values, int[] defaults, int minimum) {
        int[] result = defaults.clone();
        if (values == null) return result;
        for (int i = 0; i < result.length && i < values.size(); i++) {
            try {
                int value = Integer.parseInt(values.get(i));
                if (value >= minimum) result[i] = value;
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private static String unquote(String text) {
        return text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"") ? text.substring(1, text.length() - 1) : text;
    }
}
