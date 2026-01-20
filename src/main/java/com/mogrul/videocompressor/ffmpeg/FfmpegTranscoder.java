package com.mogrul.videocompressor.ffmpeg;

import com.mogrul.videocompressor.inter.Transcoder;
import com.mogrul.videocompressor.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FfmpegTranscoder implements Transcoder {
    private final Logger logger = LoggerFactory.getLogger(FfmpegTranscoder.class);
    private final Path ffmpegPath;
    private final int fps;
    private final ToolRunner runner;

    public FfmpegTranscoder(Path ffmpegPath, int fps, ToolRunner runner) {
        this.ffmpegPath = ffmpegPath;
        this.fps = fps;
        this.runner = runner;
    }

    @Override
    public void transcodeTo720p(Path input, Path outputTmp) throws Exception {
        logger.info("[TRANSCODING] {}", input);

        String vf = "scale=w=1280:h=720:force_original_aspect_ratio=decrease," +
                "pad=1280:720:(ow-iw)/2:(oh-ih)/2," +
                "fps=" + fps;

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath.toString());
        cmd.addAll(List.of(
                "-y",
                "-i", input.toString(),
                "-vf", vf,
                "-c:v", "h264_nvenc",
                "-preset", "p5",
                "-cq", "22",
                "-b:v", "0",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-b:a", "96k",
                "-ac", "2",
                "-ar", "48000",
                "-movflags", "+faststart",
                outputTmp.toString()
        ));

        var result = runner.run(cmd);
        if (result.exitCode() != 0) {
            throw new RuntimeException("ffmpeg failed (exit " + result.exitCode() + ")\n" + result.outputTail());
        }
    }
}
