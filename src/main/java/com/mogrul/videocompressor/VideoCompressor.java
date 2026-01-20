package com.mogrul.videocompressor;

import com.mogrul.videocompressor.ffmpeg.FfmpegTranscoder;
import com.mogrul.videocompressor.ffmpeg.FfprobeValidator;
import com.mogrul.videocompressor.record.Arg;
import com.mogrul.videocompressor.record.JobConfig;
import com.mogrul.videocompressor.service.CompressionService;
import com.mogrul.videocompressor.util.*;

import java.nio.file.Path;

public class VideoCompressor {
    private static final Path localStage = Path.of("temp");
    private static final Path dbPath = Path.of("compressed.db");

    static void main(String[] args) throws Exception {
        Args arguments = new Args(args);
        Arg arg = arguments.arg;

        JobConfig config = new JobConfig(
                arg.inputRoot(), arg.outputRoot(), localStage, arg.ffmpegPath(), arg.ffprobePath(),
                arg.targetFps(), arg.workers(), dbPath, arg.deleteSource()
        );

        VideoFileScanner fileScanner = new VideoFileScanner();
        StampCalculator stamper = new StampCalculator(64 * 1024);
        SQLiteStampStore store = new SQLiteStampStore(config.dbPath());

        ToolRunner runner = new ToolRunner();
        FfmpegTranscoder transcoder = new FfmpegTranscoder(config.ffmpegPath(), config.targetFps(), runner);
        FfprobeValidator validator = new FfprobeValidator(config.ffprobePath(), runner);

        CompressionService service = new CompressionService(config, fileScanner, stamper, store, transcoder, validator);

        service.run();
    }
}
