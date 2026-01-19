package com.mogrul.videocompressor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class FileStager {
    private final Logger logger = LoggerFactory.getLogger(FileStager.class);

    public void download(Path remoteInput, Path localInput) throws Exception {
        logger.info("[DOWNLOADING] {}", remoteInput);

        Files.createDirectories(localInput.getParent());
        copyFile(remoteInput, localInput);
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
            try { Files.deleteIfExists(p); } catch (Exception ignore) {}
        }
    }

    private static void copyFile(Path src, Path dst) throws Exception {
        // Replace existing to allow reruns
        try (InputStream in = Files.newInputStream(src);
             OutputStream out = Files.newOutputStream(dst, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }
    }
}
