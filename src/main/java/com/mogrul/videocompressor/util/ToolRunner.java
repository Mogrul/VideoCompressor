package com.mogrul.videocompressor.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ToolRunner {
    public ToolResult run(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process p = pb.start();

        StringBuilder tail = new StringBuilder();
        int linesKept = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                // keep only last ~200 lines
                if (linesKept++ > 200) {
                    int cut = tail.indexOf("\n");
                    if (cut > 0) tail.delete(0, cut + 1);
                }
                tail.append(line).append("\n");
            }
        }

        int code = p.waitFor();
        return new ToolResult(code, tail.toString());
    }

    public record ToolResult(int exitCode, String outputTail) {}
}
