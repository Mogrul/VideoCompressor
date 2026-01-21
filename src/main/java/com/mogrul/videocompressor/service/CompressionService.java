package com.mogrul.videocompressor.service;

import com.mogrul.videocompressor.ffmpeg.FfmpegTranscoder;
import com.mogrul.videocompressor.ffmpeg.Ffprobe;
import com.mogrul.videocompressor.inter.StampStore;
import com.mogrul.videocompressor.record.Config;
import com.mogrul.videocompressor.record.FileStamp;
import com.mogrul.videocompressor.record.JobConfig;
import com.mogrul.videocompressor.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CompressionService {
    private final Logger logger = LoggerFactory.getLogger(CompressionService.class);
    private final Config config;
    private final VideoFileScanner scanner;
    private final StampCalculator stamper;
    private final StampStore store;
    private final FfmpegTranscoder transcoder;
    private final Ffprobe ffprobe;

    private final StagePlanner planner;
    private final FileStager stager;

    public CompressionService(
            Config config,
            VideoFileScanner scanner,
            StampCalculator stamper,
            StampStore store,
            FfmpegTranscoder transcoder,
            Ffprobe ffprobe
    ) {
        this.config = config;
        this.scanner = scanner;
        this.stamper = stamper;
        this.store = store;
        this.transcoder = transcoder;
        this.ffprobe = ffprobe;

        this.planner = new StagePlanner(config.inputRoot(), config.outputRoot(), config.localStageRoot());
        this.stager = new FileStager();
    }

    public void run() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(
                config.workers(),
                new NamedThreadFactory("CompressionService")
        );
        List<Future<?>> futures = new ArrayList<>();

        try {
            scanner.scan(config.inputRoot(), input -> {
                futures.add(pool.submit(() -> {
                    try {
                        processOne(input);
                    } catch (Exception e) {
                        logger.error("[FAIL] {} :: {}", input, e.getMessage());
                    }
                }));
            });

            for (Future<?> f : futures) f.get();

        } finally {
            pool.shutdown();
            pool.awaitTermination(365, TimeUnit.DAYS);
            store.close();
        }
    }

    private void processOne(Path remoteInput) throws Exception {
        FileStamp stamp = stamper.stamp(remoteInput);
        String sourceKey = remoteInput.toAbsolutePath().toString();

        if (store.isUpToDate(sourceKey, stamp)) {
            logger.info("[SKIP] {}", remoteInput);
            return;
        }

        var paths = planner.plan(remoteInput);
        store.markRunning(sourceKey, stamp);

        try {
            Path input;
            Path outputTmp;
            Path output;
            Path remoteTmpOutput = null;
            Path remoteOutput = null;

            if (config.downloadFromRemote()) {
                input = paths.localInput();
                outputTmp = paths.localTmpOutput();
                output = paths.localFinalOutput();
                remoteOutput = paths.remoteFinalOut();
                remoteTmpOutput = paths.remoteTmpOut();

                // Downloads locally and transcode from local directory
                stager.download(remoteInput, input);
            } else {
                input = remoteInput;
                outputTmp = paths.remoteTmpOut();
                output = paths.remoteFinalOut();
            }

            Files.createDirectories(outputTmp.getParent());
            transcoder.transcode(input, outputTmp, ffprobe);
            Files.createDirectories(output.getParent());
            Files.move(outputTmp, output,
                    StandardCopyOption.REPLACE_EXISTING
            );

            if (config.downloadFromRemote()) {
                if (remoteTmpOutput == null || remoteOutput == null) throw new IllegalArgumentException(
                        "remoteTmpOutput or remoteOutput is null"
                );

                stager.uploadAtomic(output, remoteTmpOutput, remoteOutput);
            } else {
                // Cleanup local after success.
                stager.cleanup(input, output);
            }

            if (config.deleteSourceAfterSuccess()) {
                stager.cleanup(remoteInput);
            }

            store.markDone(sourceKey, stamp, paths.remoteFinalOut().toString());

            logger.info("[DONE] {} -> {}", remoteInput, paths.remoteFinalOut());

        } catch (Exception e) {
            store.markFailed(sourceKey, stamp, e.toString());
            throw e;
        }
    }
}
