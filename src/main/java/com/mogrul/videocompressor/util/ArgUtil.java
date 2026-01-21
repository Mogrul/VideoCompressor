package com.mogrul.videocompressor.util;

import com.mogrul.videocompressor.record.ArgumentList;
import com.mogrul.videocompressor.record.Arguments;
import com.mogrul.videocompressor.record.Argument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ArgUtil {
    public static final Map<String, String> argMap = new HashMap<>();
    
    public static void getHelp() {
        System.out.println("Mogrul - Video Compressor tool for remote hosted video compression using Ffmpeg.\n");
        for (Argument arg : ArgumentList.arguments) {
            System.out.println("--" + arg.name() + " - " + arg.description());
        }
        
        System.exit(0);
    }

    /** Parse args[] into a map. */
    public static void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String raw = args[i];

            if (raw.startsWith("--")) {
                String key = raw.substring(2);

                // --key=value
                int eq = key.indexOf('=');
                if (eq >= 0) {
                    String k = key.substring(0, eq).trim();
                    String v = key.substring(eq + 1).trim();
                    put(k, v);
                    continue;
                }

                // --flag  OR  --key value
                if (i + 1 < args.length && !looksLikeKey(args[i + 1])) {
                    put(key.trim(), args[++i].trim());
                } else {
                    put(key.trim(), "true");
                }

            } else if (raw.startsWith("-") && raw.length() > 1) {
                // -k value OR -flag
                String key = raw.substring(1).trim();

                if (i + 1 < args.length && !looksLikeKey(args[i + 1])) {
                    put(key, args[++i].trim());
                } else {
                    put(key, "true");
                }

            } else {
                // positional arg; store as "_1", "_2", etc
                put("_" + (countPositionals() + 1), raw.trim());
            }
        }
    }

    public static boolean looksLikeKey(String s) {
        return s.startsWith("--") || (s.startsWith("-") && s.length() > 1);
    }

    public static int countPositionals() {
        int c = 0;
        for (String k : ArgUtil.argMap.keySet()) if (k.startsWith("_")) c++;
        return c;
    }

    public static void put(String key, String value) {
        if (key == null || key.isBlank()) return;
        ArgUtil.argMap.put(key, value == null ? "" : value);
    }

    // ---------------------------
    // Basic getters
    // ---------------------------

    public static Optional<String> getString(String key) {
        String v = ArgUtil.argMap.get(key);
        if (v == null) return Optional.empty();
        v = v.trim();
        return v.isEmpty() ? Optional.empty() : Optional.of(v);
    }

    public static String requireString(String key) {
        return getString(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required arg: --" + key));
    }

    public static Optional<Integer> getInt(String key) {
        return getString(key).map(v -> {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid int for --" + key + ": " + v);
            }
        });
    }

    public static int requireInt(String key) {
        return getInt(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required int arg: --" + key));
    }

    public static Optional<Boolean> getBoolean(String key) {
        return getString(key).map(v -> {
            String s = v.trim().toLowerCase(Locale.ROOT);
            return switch (s) {
                case "true", "1", "yes", "y", "on" -> true;
                case "false", "0", "no", "n", "off" -> false;
                default -> throw new IllegalArgumentException("Invalid boolean for --" + key + ": " + v);
            };
        });
    }

    public static boolean hasFlag(String key) {
        // supports --flag (stored as true) or explicit --flag=false
        return getBoolean(key).orElse(false);
    }

    // ---------------------------
    // Path / Directory helpers
    // ---------------------------

    public static Optional<Path> getPathFromArg(String key) {
        return getString(key).map(Path::of);
    }

    public static Path requirePathFromArg(String key) {
        return getPathFromArg(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required path arg: --" + key));
    }

    /** Returns an existing directory path (does not create it). */
    public static Optional<Path> getDirectoryFromArg(String key) {
        return getPathFromArg(key).map(p -> {
            if (!Files.exists(p)) {
                throw new IllegalArgumentException("Directory does not exist for --" + key + ": " + p);
            }
            if (!Files.isDirectory(p)) {
                throw new IllegalArgumentException("Not a directory for --" + key + ": " + p);
            }
            return p;
        });
    }

    public static Path requireDirectoryFromArg(String key) {
        return getDirectoryFromArg(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required directory arg: --" + key));
    }

    /** Returns a directory path, creating it if needed. */
    public static Optional<Path> getOrCreateDirectoryFromArg(String key) {
        return getPathFromArg(key).map(p -> {
            try {
                Files.createDirectories(p);
                if (!Files.isDirectory(p)) {
                    throw new IllegalArgumentException("Not a directory for --" + key + ": " + p);
                }
                return p;
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to create directory for --" + key + ": " + p + " (" + e.getMessage() + ")", e);
            }
        });
    }

    public static Path requireOrCreateDirectoryFromArg(String key) {
        return getOrCreateDirectoryFromArg(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required directory arg: --" + key));
    }

    // ---------------------------
    // Convenience: defaults
    // ---------------------------

    public static String getStringOrDefault(String key, String def) {
        return getString(key).orElse(def);
    }

    public static int getIntOrDefault(String key, int def) {
        return getInt(key).orElse(def);
    }

    public static boolean getBooleanOrDefault(String key, boolean def) {
        return getBoolean(key).orElse(def);
    }
}
