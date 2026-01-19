package com.mogrul.videocompressor.inter;

import com.mogrul.videocompressor.record.FileStamp;

public interface StampStore extends AutoCloseable {
    boolean isUpToDate(String sourcePath, FileStamp stamp) throws Exception;
    void markRunning(String sourcePath, FileStamp stamp) throws Exception;
    void markDone(String sourcePath, FileStamp stamp, String outputPath) throws Exception;
    void markFailed(String sourcePath, FileStamp stamp, String error) throws Exception;

    @Override void close() throws Exception;
}
