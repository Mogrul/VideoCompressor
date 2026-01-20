package com.mogrul.videocompressor.util;

import com.mogrul.videocompressor.record.Arg;
import com.mogrul.videocompressor.record.Argument;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Args {
    private final Map<String, String> argMap = new HashMap<>();

    // DEFAULTS
    private final Path defaultFfmpegPath = Path.of("ffmpeg/ffmpeg");
    private final Path defaultFfprobePath = Path.of("ffmpeg/ffprobe");
    private final int defaultFps = 15;
    private final int defaultWorkers = 4;
    private final boolean defaultDeleteSource = false;

    public final Arg arg;

    private class ArgList {
        public static Argument help = new Argument("help", "An argument to display all valid program arguments");
        public static Argument inputRoot = new Argument("input", "[String] The directory where video files are found (scans recursively).");
        public static Argument outputRoot = new Argument("output", "[String] The directory where the compressed files will be uploaded to.");
        public static Argument ffmpegPath = new Argument("ffmpeg", "[String] The absolute path of ffmpeg.exe");
        public static Argument ffprobePath = new Argument("ffprobe", "[String] The absolute path of ffprobe.exe");
        public static Argument targetFps = new Argument("fps", "[Number] The target FPS the compressor will compress to.");
        public static Argument workers = new Argument("workers", "[Number] The amount of worker threads to be running simultaneously.");
        public static Argument deleteSource = new Argument("delete-source", "[True/False] whether or not the original file should be deleted after compression.");

        public static List<Argument> arguments = List.of(
                help, inputRoot, outputRoot, ffmpegPath, ffprobePath, targetFps, workers
        );
    }

    public Args(String[] args) {
        parse(args);

        if (hasFlag(ArgList.help.name())) {
            getHelp();
        }

        Path inputRoot = requireDirectoryFromArg(ArgList.inputRoot.name());
        Path outputRoot = requireDirectoryFromArg(ArgList.outputRoot.name());
        Path ffmpegPath  = getPathFromArg(ArgList.ffmpegPath.name()).orElse(defaultFfmpegPath);
        Path ffprobePath = getPathFromArg(ArgList.ffprobePath.name()).orElse(defaultFfprobePath);

        int targetFps = getIntOrDefault(ArgList.targetFps.name(), defaultFps);
        int workers = getIntOrDefault(ArgList.workers.name(), defaultWorkers);

        boolean deleteSource = getBooleanOrDefault(ArgList.deleteSource.name(), defaultDeleteSource);
        
        this.arg = new Arg(
                inputRoot,
                outputRoot,
                ffmpegPath,
                ffprobePath,
                targetFps,
                workers,
                deleteSource
        );
    }
    
    private void getHelp() {
        System.out.println("Mogrul - Video Compressor tool for remote hosted video compression using Ffmpeg.\n");
        for (Argument arg : ArgList.arguments) {
            System.out.println("--" + arg.name() + " - " + arg.description());
        }
        
        System.exit(0);
    }

    /** Parse args[] into a map. */
    private void parse(String[] args) {
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

    private boolean looksLikeKey(String s) {
        return s.startsWith("--") || (s.startsWith("-") && s.length() > 1);
    }

    private int countPositionals() {
        int c = 0;
        for (String k : argMap.keySet()) if (k.startsWith("_")) c++;
        return c;
    }

    private void put(String key, String value) {
        if (key == null || key.isBlank()) return;
        argMap.put(key, value == null ? "" : value);
    }

    // ---------------------------
    // Basic getters
    // ---------------------------

    private Optional<String> getString(String key) {
        String v = argMap.get(key);
        if (v == null) return Optional.empty();
        v = v.trim();
        return v.isEmpty() ? Optional.empty() : Optional.of(v);
    }

    private String requireString(String key) {
        return getString(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required arg: --" + key));
    }

    private Optional<Integer> getInt(String key) {
        return getString(key).map(v -> {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid int for --" + key + ": " + v);
            }
        });
    }

    private int requireInt(String key) {
        return getInt(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required int arg: --" + key));
    }

    private Optional<Boolean> getBoolean(String key) {
        return getString(key).map(v -> {
            String s = v.trim().toLowerCase(Locale.ROOT);
            return switch (s) {
                case "true", "1", "yes", "y", "on" -> true;
                case "false", "0", "no", "n", "off" -> false;
                default -> throw new IllegalArgumentException("Invalid boolean for --" + key + ": " + v);
            };
        });
    }

    private boolean hasFlag(String key) {
        // supports --flag (stored as true) or explicit --flag=false
        return getBoolean(key).orElse(false);
    }

    // ---------------------------
    // Path / Directory helpers
    // ---------------------------

    private Optional<Path> getPathFromArg(String key) {
        return getString(key).map(Path::of);
    }

    private Path requirePathFromArg(String key) {
        return getPathFromArg(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required path arg: --" + key));
    }

    /** Returns an existing directory path (does not create it). */
    private Optional<Path> getDirectoryFromArg(String key) {
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

    private Path requireDirectoryFromArg(String key) {
        return getDirectoryFromArg(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required directory arg: --" + key));
    }

    /** Returns a directory path, creating it if needed. */
    private Optional<Path> getOrCreateDirectoryFromArg(String key) {
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

    private Path requireOrCreateDirectoryFromArg(String key) {
        return getOrCreateDirectoryFromArg(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing required directory arg: --" + key));
    }

    // ---------------------------
    // Convenience: defaults
    // ---------------------------

    private String getStringOrDefault(String key, String def) {
        return getString(key).orElse(def);
    }

    private int getIntOrDefault(String key, int def) {
        return getInt(key).orElse(def);
    }

    private boolean getBooleanOrDefault(String key, boolean def) {
        return getBoolean(key).orElse(def);
    }
}
