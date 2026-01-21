package com.mogrul.videocompressor.record;

import java.util.List;

public record ArgumentList() {
    public static Argument help = new Argument("help", "An argument to display all valid program arguments");
    public static Argument inputRoot = new Argument("input", "[String] The directory where video files are found (scans recursively).");
    public static Argument outputRoot = new Argument("output", "[String] The directory where the compressed files will be uploaded to.");
    public static Argument ffmpegPath = new Argument("ffmpeg", "[String] The absolute path of ffmpeg.exe");
    public static Argument ffprobePath = new Argument("ffprobe", "[String] The absolute path of ffprobe.exe");
    public static Argument targetFps = new Argument("fps", "[Number] The target FPS the compressor will compress to.");
    public static Argument workers = new Argument("workers", "[Number] The amount of worker threads to be running simultaneously.");
    public static Argument deleteSource = new Argument("delete-source", "[True/False] whether or not the original file should be deleted after compression.");
    public static Argument downloadFromRemote = new Argument("download-remote", "[True/False] Whether the remote file should be downloaded to a staged local path or not.");
    public static Argument outputWidth = new Argument("output-width", "[Number] The width of the output files to be compressed.");
    public static Argument outputHeight = new Argument("output-height", "[Number] The height of the output files.");

    public static List<Argument> arguments = List.of(
            help, inputRoot, outputRoot, ffmpegPath, ffprobePath, targetFps, workers, deleteSource, downloadFromRemote,
            outputWidth, outputHeight
    );
}
