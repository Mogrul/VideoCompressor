package com.mogrul.videocompressor.record;

import java.nio.file.Path;

public record Config(
        Path inputRoot,
        Path outputRoot,
        Path localStageRoot,
        Path ffmpegPath,
        Path ffprobePath,
        Path dbPath,
        int fps,
        int workers,
        int outputHeight,
        int outputWidth,
        boolean deleteSourceAfterSuccess,
        boolean downloadFromRemote
) {
}
