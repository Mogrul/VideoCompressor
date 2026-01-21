package com.mogrul.videocompressor.record;

import java.nio.file.Path;

public record Arguments(
        Path inputRoot,
        Path outputRoot,
        Path ffmpegPath,
        Path ffprobePath,
        int targetFps,
        int workers,
        boolean deleteSource,
        boolean downloadFromRemote,
        int outputWidth,
        int outputHeight
) {
}
