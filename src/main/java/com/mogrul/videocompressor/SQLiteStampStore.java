package com.mogrul.videocompressor;

import com.mogrul.videocompressor.inter.StampStore;
import com.mogrul.videocompressor.record.FileStamp;

import java.nio.file.Path;
import java.sql.*;

public final class SQLiteStampStore implements StampStore {
    private final Connection connection;

    public SQLiteStampStore(Path dbPath) throws Exception {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString());
        this.connection.setAutoCommit(true);
        init();
    }

    private void init() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS compress_jobs (
                  source_path TEXT PRIMARY KEY,
                  size INTEGER NOT NULL,
                  mtime_ms INTEGER NOT NULL,
                  partial_sha256 TEXT NOT NULL,
                  output_path TEXT,
                  status TEXT NOT NULL,
                  last_error TEXT,
                  updated_at INTEGER NOT NULL
                );
            """);

            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_status ON compress_jobs(status);");
            st.executeUpdate("PRAGMA journal_mode=WAL;");
            st.executeUpdate("PRAGMA synchronous=NORMAL;");
            st.executeUpdate("PRAGMA busy_timeout=5000;");
        }
    }

    @Override
    public boolean isUpToDate(String sourcePath, FileStamp stamp) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("""
            SELECT status, size, mtime_ms, partial_sha256
            FROM compress_jobs
            WHERE source_path = ?
        """)) {
            ps.setString(1, sourcePath);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;

                String status = rs.getString("status");
                long size = rs.getLong("size");
                long mtime = rs.getLong("mtime_ms");
                String partial = rs.getString("partial_sha256");

                return "DONE".equals(status)
                        && size == stamp.size()
                        && mtime == stamp.mtimeMs()
                        && partial.equals(stamp.partialSha256());
            }
        }
    }

    @Override
    public void markRunning(String sourcePath, FileStamp stamp) throws Exception {
        upsert(sourcePath, stamp, null, "RUNNING", null);
    }

    @Override
    public void markDone(String sourcePath, FileStamp stamp, String outputPath) throws Exception {
        upsert(sourcePath, stamp, outputPath, "DONE", null);
    }

    @Override
    public void markFailed(String sourcePath, FileStamp stamp, String error) throws Exception {
        upsert(sourcePath, stamp, null, "FAILED", error);
    }

    private void upsert(String sourcePath, FileStamp stamp, String outputPath, String status, String lastError) throws Exception {
        long now = System.currentTimeMillis();

        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO compress_jobs (source_path, size, mtime_ms, partial_sha256, output_path, status, last_error, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(source_path) DO UPDATE SET
              size=excluded.size,
              mtime_ms=excluded.mtime_ms,
              partial_sha256=excluded.partial_sha256,
              output_path=excluded.output_path,
              status=excluded.status,
              last_error=excluded.last_error,
              updated_at=excluded.updated_at
        """)) {
            ps.setString(1, sourcePath);
            ps.setLong(2, stamp.size());
            ps.setLong(3, stamp.mtimeMs());
            ps.setString(4, stamp.partialSha256());
            ps.setString(5, outputPath);
            ps.setString(6, status);
            ps.setString(7, lastError);
            ps.setLong(8, now);
            ps.executeUpdate();
        }
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
