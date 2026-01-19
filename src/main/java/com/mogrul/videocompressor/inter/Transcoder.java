package com.mogrul.videocompressor.inter;

import java.nio.file.Path;

public interface Transcoder {
    void transcodeTo720p(Path input, Path outputTmp) throws Exception;
}
