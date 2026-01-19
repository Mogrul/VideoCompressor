package com.mogrul.videocompressor;

import java.nio.file.Path;

public final class StagePlanner {
    private final Path inputRoot;
    private final Path outputRoot;
    private final Path localStageRoot;

    public StagePlanner(Path inputRoot, Path outputRoot, Path localStageRoot) {
        this.inputRoot = inputRoot;
        this.outputRoot = outputRoot;
        this.localStageRoot = localStageRoot.toAbsolutePath();    }

    public StagePaths plan(Path remoteInput) {
        Path rel = inputRoot.relativize(remoteInput);

        // Remote final output (choose .mkv or .mp4)
        Path remoteFinal = outputRoot.resolve(replaceExt(rel, ".mkv"));

        // Remote temp output MUST end with .mkv so ffmpeg/muxer understands if ever used
        Path remoteTmp = remoteFinal.resolveSibling("tmp_" + remoteFinal.getFileName().toString());

        // Local staged input: keep original extension
        Path localIn = localStageRoot.resolve("in").resolve(rel);

        // Local temp output with container extension (mkv) for ffmpeg
        Path localOut = localStageRoot.resolve("out").resolve(replaceExt(rel, ".mkv"));
        Path localTmpOut = localOut.resolveSibling("tmp_" + localOut.getFileName().toString()); // tmp_video.mkv

        return new StagePaths(remoteFinal, remoteTmp, localIn, localTmpOut, localOut);
    }

    private static Path replaceExt(Path p, String newExt) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot >= 0) ? name.substring(0, dot) : name;

        Path parent = p.getParent();
        String newName = base + newExt;
        return parent == null ? Path.of(newName) : parent.resolve(newName);
    }

    public record StagePaths(
            Path remoteFinalOut,
            Path remoteTmpOut,
            Path localInput,
            Path localTmpOutput,
            Path localFinalOutput
    ) {}
}
