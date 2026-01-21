package com.mogrul.videocompressor.util;

import com.mogrul.videocompressor.record.Argument;
import com.mogrul.videocompressor.record.ArgumentList;
import com.mogrul.videocompressor.record.Config;
import com.mogrul.videocompressor.record.DefaultArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    private static final Path localStage = Path.of("temp");
    private static final Path dbPath = Path.of("compressed.db");

    public static Config getConfig() {
        if (ArgUtil.hasFlag(ArgumentList.help.name())) {
            ArgUtil.getHelp();
        }

        // Check over parsed arguments
        boolean invalidArgument = false;
        for (String key : ArgUtil.argMap.keySet()) {
            boolean known = ArgumentList.arguments.stream()
                    .map(Argument::name)
                    .anyMatch(key::equals);

            if (!known) {
                logger.error("Unknown argument: --{}", key);
                invalidArgument = true;
            }
        }

        if (invalidArgument) {
            ArgUtil.getHelp();
        }

        Path inputRoot = ArgUtil.requireDirectoryFromArg(ArgumentList.inputRoot.name());
        Path outputRoot = ArgUtil.requireDirectoryFromArg(ArgumentList.outputRoot.name());
        Path ffmpegPath = ArgUtil.getPathFromArg(ArgumentList.ffmpegPath.name()).orElse(DefaultArguments.ffmpegPath);
        Path ffprobePath = ArgUtil.getPathFromArg(ArgumentList.ffprobePath.name()).orElse(DefaultArguments.ffprobePath);

        int fps = ArgUtil.getIntOrDefault(ArgumentList.targetFps.name(), DefaultArguments.fps);
        int workers = ArgUtil.getIntOrDefault(ArgumentList.workers.name(), DefaultArguments.workers);
        int outputWidth = ArgUtil.getIntOrDefault(ArgumentList.outputWidth.name(), DefaultArguments.outputWidth);
        int outputHeight = ArgUtil.getIntOrDefault(ArgumentList.outputHeight.name(), DefaultArguments.outputHeight);

        boolean deleteSourceAfterSuccess = ArgUtil.getBooleanOrDefault(ArgumentList.deleteSource.name(), DefaultArguments.deleteSource);
        boolean downloadFromRemote = ArgUtil.getBooleanOrDefault(ArgumentList.downloadFromRemote.name(), DefaultArguments.downloadFromRemote);

        return new Config(
                inputRoot, outputRoot, localStage, ffmpegPath, ffprobePath, dbPath, fps, workers, outputHeight,
                outputWidth, deleteSourceAfterSuccess, downloadFromRemote
        );
    }
}
