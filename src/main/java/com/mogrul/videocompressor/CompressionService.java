package com.mogrul.videocompressor;

import com.mogrul.videocompressor.inter.StampStore;
import com.mogrul.videocompressor.inter.Transcoder;
import com.mogrul.videocompressor.inter.Validator;
import com.mogrul.videocompressor.record.FileStamp;
import com.mogrul.videocompressor.record.JobConfig;
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
    private final JobConfig config;
    private final VideoFileScanner scanner;
    private final StampCalculator stamper;
    private final StampStore store;
    private final Transcoder transcoder;
    private final Validator validator;

    private final StagePlanner planner;
    private final FileStager stager;

    public CompressionService(
            JobConfig config,
            VideoFileScanner scanner,
            StampCalculator stamper,
            StampStore store,
            Transcoder transcoder,
            Validator validator
    ) {
        this.config = config;
        this.scanner = scanner;
        this.stamper = stamper;
        this.store = store;
        this.transcoder = transcoder;
        this.validator = validator;

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
            // 1) download remote -> local
            stager.download(remoteInput, paths.localInput());
            Files.createDirectories(paths.localTmpOutput().getParent());

            // 2) transcode locally (fast disk, GPU happier)
            transcoder.transcodeTo720p(paths.localInput(), paths.localTmpOutput());

            // 3) validate local output
            validator.validate(paths.localTmpOutput());

            // 4) move local tmp -> local final (optional neatness)
            Files.createDirectories(paths.localFinalOutput().getParent());
            Files.move(paths.localTmpOutput(), paths.localFinalOutput(),
                    StandardCopyOption.REPLACE_EXISTING);

            // 5) upload back to remote atomically via remote tmp then rename
            stager.uploadAtomic(paths.localFinalOutput(), paths.remoteTmpOut(), paths.remoteFinalOut());

            store.markDone(sourceKey, stamp, paths.remoteFinalOut().toString());

            if (config.deleteSourceAfterSuccess()) {
                Files.deleteIfExists(remoteInput);
                logger.info("[DELETE] {}", remoteInput);
            }

            logger.info("[DONE] {} -> {}", remoteInput, paths.remoteFinalOut());

        } catch (Exception e) {
            store.markFailed(sourceKey, stamp, e.toString());
            throw e;
        } finally {
            // cleanup local staged files to save space
            logger.info("[DELETE] {}", paths.localInput());
            stager.cleanup(paths.localInput(), paths.localTmpOutput(), paths.localFinalOutput());
        }
    }
}
