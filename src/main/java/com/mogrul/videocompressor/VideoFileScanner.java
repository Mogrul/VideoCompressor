package com.mogrul.videocompressor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.function.Consumer;

public final class VideoFileScanner {
    public void scan(Path root, Consumer<Path> onVideoFile) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isVideo(file)) onVideoFile.accept(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean isVideo(Path file) {
        String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".mov")
                || n.endsWith(".avi") || n.endsWith(".webm") || n.endsWith(".m4v");
    }
}
