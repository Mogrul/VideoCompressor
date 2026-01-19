package com.mogrul.videocompressor.record;

import java.nio.file.Path;

public record JobConfig(
        Path inputRoot,
        Path outputRoot,
        Path localStageRoot,
        Path ffmpegPath,
        Path ffprobePath,
        int targetFps,
        int workers,
        Path dbPath,
        boolean deleteSourceAfterSuccess
) {}
