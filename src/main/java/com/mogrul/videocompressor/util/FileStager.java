package com.mogrul.videocompressor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import static java.nio.file.StandardOpenOption.*;

public final class FileStager {
    private final Logger logger = LoggerFactory.getLogger(FileStager.class);

    public void download(Path remoteInput, Path localInput) throws Exception {
        logger.info("[DOWNLOADING] {}", remoteInput);

        Files.createDirectories(localInput.getParent());

        long remoteSize = Files.size(remoteInput);
        long localSize = Files.exists(localInput) ? Files.size(localInput) : 0L;

        if (localSize == remoteSize && remoteSize > 0) {
            logger.info("[SKIP] Already downloaded: {}", localInput);
            return;
        }

        // If local is bigger or remote shrank/changed, start over.
        if (localSize > remoteSize) {
            logger.warn("[RESET] Local larger than remote, restarting download: {}", localInput);
            Files.deleteIfExists(localInput);
            localSize = 0L;
        }

        copyFileResumable(remoteInput, localInput, localSize, remoteSize);
    }

    public void uploadAtomic(Path localOutput, Path remoteTmp, Path remoteFinal) throws Exception {
        logger.info("[UPLOADING] {} -> {}", localOutput, remoteFinal);

        Files.createDirectories(remoteFinal.getParent());

        // copy to remote tmp first
        copyFile(localOutput, remoteTmp);

        // then rename tmp -> final on the same remote filesystem (fast + atomic-ish)
        Files.move(remoteTmp, remoteFinal, StandardCopyOption.REPLACE_EXISTING);
    }

    public void cleanup(Path... paths) {
        for (Path p : paths) {
            try {
                if (Files.exists(p)) {
                    logger.info("[CLEANUP] {}", p);
                    Files.delete(p);
                }
            } catch (Exception e) {
                logger.error("[CLEANUP] {}\n{}", p, e.getMessage());
            }
        }
    }

    private static void copyFile(Path src, Path dst) throws Exception {
        // Replace existing to allow reruns
        try (InputStream in = Files.newInputStream(src);
             OutputStream out = Files.newOutputStream(dst, CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }
    }

    private void copyFileResumable(Path src, Path dst, long startOffset, long expectedTotal) throws IOException {
        // Open destination for append (create if missing)
        try (SeekableByteChannel in = Files.newByteChannel(src, READ);
             SeekableByteChannel out = Files.newByteChannel(dst, CREATE, WRITE)) {

            // Seek to where we want to resume
            if (startOffset > 0) {
                logger.info("[RESUME] {} ({} / {})",
                        dst.getFileName(),
                        SizeFormatUtil.humanBytes(startOffset),
                        SizeFormatUtil.humanBytes(expectedTotal)
                );
                in.position(startOffset);
                out.position(startOffset); // overwrite/continue exactly at the offset
            } else {
                out.truncate(0); // fresh download
            }

            ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 1024 * 4); // 4 MiB buffer

            while (true) {
                buf.clear();
                int read = in.read(buf);
                if (read < 0) break;

                buf.flip();
                while (buf.hasRemaining()) {
                    out.write(buf);
                }
            }
        }

        // Verify size at the end (important for resume correctness)
        long finalSize = Files.size(dst);
        if (expectedTotal > 0 && finalSize != expectedTotal) {
            throw new IOException("Resume download incomplete: " + dst + " is " + finalSize + " bytes, expected " + expectedTotal);
        }

        logger.info("[DOWNLOADED] {}", dst);
    }
}
