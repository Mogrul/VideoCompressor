package com.mogrul.videocompressor.ffmpeg;

import com.mogrul.videocompressor.enu.VideoCodec;
import com.mogrul.videocompressor.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Ffprobe {
    private final Logger logger = LoggerFactory.getLogger(Ffprobe.class);
    private final Path ffprobePath;
    private final ToolRunner runner;

    public Ffprobe(Path ffprobePath, ToolRunner runner) {
        this.ffprobePath = ffprobePath;
        this.runner = runner;
    }

    public void validate(Path outputFile) throws Exception{
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

    public VideoCodec getVideoCodec(Path videoFile) throws Exception {
        List<String> cmd = List.of(
                ffprobePath.toString(),
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=nw=1:nk=1",
                videoFile.toString()
        );

        ToolRunner.ToolResult result = runner.run(cmd);
        if (result.exitCode() != 0) {
            return VideoCodec.OTHER;
        }
        String codecName = result.outputTail().trim();

        VideoCodec videoCodec = VideoCodec.fromCodecName(codecName);
        logger.info("[CODEC] Found codec {} for {}", videoCodec.name(), videoFile);

        return videoCodec;
    }

    private static double parseFirstDouble(String text) {
        String s = text.trim();
        String firstLine = s.contains("\n") ? s.substring(0, s.indexOf('\n')).trim() : s;
        return Double.parseDouble(firstLine);
    }

    private static final class ValidationException extends Exception {
        public ValidationException(String message) { super(message); }
    }
}
