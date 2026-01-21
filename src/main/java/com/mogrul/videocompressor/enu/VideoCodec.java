package com.mogrul.videocompressor.enu;

import com.mogrul.videocompressor.ffmpeg.Ffprobe;

public enum VideoCodec {
    H264, HEVC, AV1, OTHER;

    public static VideoCodec fromCodecName(String codecName) {
        if (codecName == null) return OTHER;
        return switch (codecName.trim().toLowerCase()) {
            case "h264", "avc1" -> H264;
            case "hevc", "h265" -> HEVC;
            case "av1" -> AV1;
            default -> OTHER;
        };
    }
}
