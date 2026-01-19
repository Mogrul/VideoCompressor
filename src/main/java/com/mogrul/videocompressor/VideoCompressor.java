package com.mogrul.videocompressor;

import com.mogrul.videocompressor.record.JobConfig;

import java.nio.file.Path;

public class VideoCompressor {
    private static final Path inputRoot = Path.of("Y:\\Videos\\Recordings");
    private static final Path outputRoot = Path.of("Y:\\Videos\\Recordings-Compressed");
    private static final Path ffmpegPath = Path.of("ffmpeg/ffmpeg");
    private static final Path ffprobePath = Path.of("ffmpeg/ffprobe");
    private static final Path localStage = Path.of("temp");
    private static final Path dbPath = Path.of("compressed.db");

    private static final int targetFps = 15;
    private static final int workers = 4;

    static void main() throws Exception {
        JobConfig config = new JobConfig(
                inputRoot, outputRoot, localStage, ffmpegPath, ffprobePath,
                targetFps, workers, dbPath, true
        );

        VideoFileScanner fileScanner = new VideoFileScanner();
        StampCalculator stamper = new StampCalculator(64 * 1024);
        SQLiteStampStore store = new SQLiteStampStore(config.dbPath());

        var runner = new ToolRunner();
        FfmpegTranscoder transcoder = new FfmpegTranscoder(config.ffmpegPath(), config.targetFps(), runner);
        var validator = new FfprobeValidator(config.ffprobePath(), runner);

        var service = new CompressionService(config, fileScanner, stamper, store, transcoder, validator);

        service.run();
    }
}
