# Video Compressor
A multi-threaded Ffmpeg Java video compressing tool that downloads a video from a remote directory, compresses the file locally and then uploads the compressed file back into the remote directory.

Created after running out of space with a lot of video files in 4K 60FPS, compressing them down to 720P 25FPS and saving 5TB+ of data.

### Flags
The program supports a variety of flags.
+ **--help** : *Displays all available flags with descriptions*
+ **--input** : *The input directory where the video files are found (recursive scanning)*
+ **--output** : *The output directory where the video files are uploaded after compression.*
+ **--ffmpeg** : *The absolute path to the ffmpeg.exe file*
+ **--ffprobe** : *The absolute path to the ffprobe.exe file*
+ **--fps** : *The FPS for the compressor to compress to*
+ **--workers** : *The amount of worker threads to activate simultaneously*

### Install Guide
1. Download the appropriate file from the releases section.
2. Download ffmpeg.exe, ffprobe.exe into a desired location from: https://www.gyan.dev/ffmpeg/builds/
3. Run the file using `java -jar {{name}}` followed by the appropriate arguments