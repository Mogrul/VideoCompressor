package com.mogrul.videocompressor;

import com.mogrul.videocompressor.record.FileStamp;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;

public class StampCalculator {
    private final int partialBytes;

    public StampCalculator(int partialBytes) {
        if (partialBytes <= 0) throw new IllegalArgumentException("partialBytes must be > 0");
        this.partialBytes = partialBytes;
    }

    public FileStamp stamp(Path input) throws Exception {
        BasicFileAttributes a = Files.readAttributes(input, BasicFileAttributes.class);
        long size = a.size();
        long mtime = a.lastModifiedTime().toMillis();

        String partial = partialSha256(input, partialBytes, size);
        return new FileStamp(size, mtime, partial);
    }

    private static String partialSha256(Path file, int bytes, long size) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // include some metadata to reduce collisions across identical headers
        md.update(file.toString().getBytes(StandardCharsets.UTF_8));
        md.update(ByteBuffer.allocate(8).putLong(size).array());

        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int remaining = bytes;
            while (remaining > 0) {
                int r = in.read(buf, 0, Math.min(buf.length, remaining));
                if (r < 0) break;
                md.update(buf, 0, r);
                remaining -= r;
            }
        }

        return hex(md.digest());
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}
