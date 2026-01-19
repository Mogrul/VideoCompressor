package com.mogrul.videocompressor.record;

public record FileStamp(
        long size,
        long mtimeMs,
        String partialSha256
) {
}
