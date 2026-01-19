package com.mogrul.videocompressor.record;

import java.nio.file.Path;

public record OutputPaths(
        Path finalOut,
        Path tmpOut
) {
}
