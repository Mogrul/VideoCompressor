# Video Compressor
A multi-threaded Ffmpeg Java tool that can compress a mass of video files from a remote or local server.

Created after running out of space with a lot of video files in 4K 60FPS, compressing them down to 720P 25FPS and saving 5TB+ of data.

### Flags
The program supports a variety of flags.
+ **--help** : *Displays all available flags with descriptions*
+ **--input** : [String] *The input directory where the video files are found (recursive scanning)*
+ **--output** : [String] *The output directory where the video files are uploaded after compression.*
+ **--ffmpeg** : [String] (ffmpeg/ffmpeg) *The absolute path to the ffmpeg executable*
+ **--ffprobe** : [String] (ffmpeg/ffprobe) *The absolute path to the ffprobe executable*
+ **--fps** : [Number] (25) *The FPS for the compressor to compress to*
+ **--workers** : [Number] (4) *The amount of worker threads to activate simultaneously*
+ **--delete-source** : [True/False] (false) *Whether the source file should be deleted after successful transcoding.*
+ **--download-remote** : [True/False] (true) *Whether the tool should download the file remotely to transcode locally before re-uploading to the remote*
+ **--output-width** : [Number] (720) *The outputted video file width for every transcoded file*
+ **--output-height** : [Number] (1280) *The outputted video file height for every transcoded file*

### Install Guide
1. Download the appropriate file from the releases section.
2. Download ffmpeg.exe, ffprobe.exe into a desired location from: https://www.gyan.dev/ffmpeg/builds/
3. Run the file using `java -jar {{name}}` followed by the appropriate arguments

<p style = "text-align:center;">
    <img src = "https://github.com/Mogrul/VideoCompressor/blob/master/images/banner.png" alt = "banner">
</p>