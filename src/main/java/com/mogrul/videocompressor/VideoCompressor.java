package com.mogrul.videocompressor;

import com.mogrul.videocompressor.ffmpeg.FfmpegTranscoder;
import com.mogrul.videocompressor.ffmpeg.Ffprobe;
import com.mogrul.videocompressor.record.Arguments;
import com.mogrul.videocompressor.record.Config;
import com.mogrul.videocompressor.record.JobConfig;
import com.mogrul.videocompressor.service.CompressionService;
import com.mogrul.videocompressor.util.*;

import java.nio.file.Path;

public class VideoCompressor {
    private static final Path localStage = Path.of("temp");
    private static final Path dbPath = Path.of("compressed.db");

    static void main(String[] args) throws Exception {
        ArgUtil.parse(args);
        Config config = ConfigUtil.getConfig();

        VideoFileScanner fileScanner = new VideoFileScanner();
        StampCalculator stamper = new StampCalculator(64 * 1024);
        SQLiteStampStore store = new SQLiteStampStore(config.dbPath());

        ToolRunner runner = new ToolRunner();
        FfmpegTranscoder transcoder = new FfmpegTranscoder(config.ffmpegPath(), config.fps(), runner,
                config.outputWidth(), config.outputHeight());
        Ffprobe ffprobe = new Ffprobe(config.ffprobePath(), runner);

        CompressionService service = new CompressionService(config, fileScanner, stamper, store, transcoder, ffprobe);

        service.run();
    }
}
