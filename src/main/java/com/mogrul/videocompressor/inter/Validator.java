package com.mogrul.videocompressor.inter;

import java.nio.file.Path;

public interface Validator {
    void validate(Path outputFile) throws Exception;
}
