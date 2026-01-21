package com.mogrul.videocompressor.ffmpeg;

import com.mogrul.videocompressor.enu.VideoCodec;
import com.mogrul.videocompressor.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FfmpegTranscoder {
    private final Logger logger = LoggerFactory.getLogger(FfmpegTranscoder.class);
    private final Path ffmpegPath;
    private final ToolRunner runner;

    private final int fps;
    private final int outputWidth;
    private final int outputHeight;

    public FfmpegTranscoder(Path ffmpegPath, int fps, ToolRunner runner, int outputWidth, int outputHeight) {
        this.ffmpegPath = ffmpegPath;
        this.fps = fps;
        this.runner = runner;
        this.outputWidth = outputWidth;
        this.outputHeight = outputHeight;
    }

    public void transcode(Path input, Path outputTmp, Ffprobe ffprobe) throws Exception {
        ToolRunner.ToolResult run;
        VideoCodec codec = ffprobe.getVideoCodec(input);
        boolean canGpuDecode = (codec == VideoCodec.H264 || codec == VideoCodec.HEVC);

        if (canGpuDecode) {
            logger.info("[GPU TRANSCODING] {}", input);
            run = runner.run(buildGpuCmd(input, outputTmp));
        } else {
            logger.info("[CPU TRANSCODING] {}", input);
            run = runner.run(buildCpuCmd(input, outputTmp));
        }

        if (run.exitCode() != 0) {
            throw new RuntimeException("ffmpeg failed (exit " + run.exitCode() + ")\n" + run.outputTail());
        }
    }

    private String cpuVf() {
        return "scale=w=" + outputWidth + ":h=" + outputHeight + ":force_original_aspect_ratio=decrease," +
                "pad=" + outputWidth + ":" + outputHeight + ":(ow-iw)/2:(oh-ih)/2," +
                "fps=" + fps;
    }

    private String gpuVfNoPad() {
        return "scale_cuda=w=" + outputWidth + ":h=" + outputHeight + ":force_original_aspect_ratio=decrease," +
                "hwdownload,format=nv12," +
                "pad=" + outputWidth + ":" + outputHeight + ":(ow-iw)/2:(oh-ih)/2," +
                "fps=" + fps + "," +
                "format=yuv420p";
    }

    private List<String> buildGpuCmd(Path input, Path output) {
        return List.of(
                ffmpegPath.toString(),
                "-y",
                "-hwaccel", "cuda",
                "-hwaccel_output_format", "cuda",
                "-i", input.toString(),
                "-vf", gpuVfNoPad(),
                "-c:v", "h264_nvenc",
                "-preset", "p5",
                "-rc:v", "vbr_hq",
                "-cq:v", "22",
                "-b:v", "0",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-b:a", "96k",
                "-ac", "2",
                "-ar", "48000",
                output.toString()
        );
    }

    private List<String> buildCpuCmd(Path input, Path output) {
        return List.of(
                ffmpegPath.toString(),
                "-y",
                "-i", input.toString(),
                "-vf", cpuVf(),
                "-c:v", "h264_nvenc",
                "-preset", "p5",
                "-rc:v", "vbr_hq",
                "-cq:v", "22",
                "-b:v", "0",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-b:a", "96k",
                "-ac", "2",
                "-ar", "48000",
                output.toString()
        );
    }
}
