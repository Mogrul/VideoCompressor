package com.mogrul.videocompressor;

import com.mogrul.videocompressor.inter.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FfprobeValidator implements Validator {
    private final Logger logger = LoggerFactory.getLogger(FfprobeValidator.class);
    private final Path ffprobePath;
    private final ToolRunner runner;

    public FfprobeValidator(Path ffprobePath, ToolRunner runner) {
        this.ffprobePath = ffprobePath;
        this.runner = runner;
    }

    @Override
    public void validate(Path outputFile) throws Exception {
        if (!Files.exists(outputFile)) {
            throw new ValidationException("Output file does not exist: " + outputFile);
        }
        if (Files.size(outputFile) < 1024) {
            throw new ValidationException("Output file is suspiciously small (<1KB): " + outputFile);
        }

        logger.info("[VALIDATING] {}", outputFile);

        // 1) Ensure ffprobe can read duration
        var durationRes = runner.run(List.of(
                ffprobePath.toString(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=nw=1:nk=1",
                outputFile.toString()
        ));

        if (durationRes.exitCode() != 0) {
            throw new ValidationException("ffprobe failed reading duration\n" + durationRes.outputTail());
        }

        double duration = parseFirstDouble(durationRes.outputTail());
        if (!(duration > 0.1)) {
            throw new ValidationException("Invalid duration from ffprobe: " + durationRes.outputTail().trim());
        }

        // 2) Ensure at least one video stream exists
        var videoStreamRes = runner.run(List.of(
                ffprobePath.toString(),
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name,width,height,r_frame_rate",
                "-of", "default=nw=1",
                outputFile.toString()
        ));

        if (videoStreamRes.exitCode() != 0) {
            throw new ValidationException("ffprobe failed reading video stream\n" + videoStreamRes.outputTail());
        }
        if (videoStreamRes.outputTail().trim().isEmpty()) {
            throw new ValidationException("No video stream detected by ffprobe.");
        }
    }

    private static double parseFirstDouble(String text) {
        String s = text.trim();
        // ffprobe often prints just the number, but we’ll be forgiving
        String firstLine = s.contains("\n") ? s.substring(0, s.indexOf('\n')).trim() : s;
        return Double.parseDouble(firstLine);
    }

    public static final class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }
}
