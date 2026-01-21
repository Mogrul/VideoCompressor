package com.mogrul.videocompressor.record;

import java.nio.file.Path;

public record DefaultArguments() {
    public static final Path ffmpegPath = Path.of("ffmpeg/ffmpeg");
    public static final Path ffprobePath = Path.of("ffmpeg/ffprobe");
    public static final int fps = 25;
    public static final int workers = 4;
    public static final boolean deleteSource = false;
    public static final boolean downloadFromRemote = true;
    public static final int outputHeight = 720;
    public static final int outputWidth = 1280;
}
