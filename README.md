# Image Averager

A Java program that turns a JPEG file into a mosaic. It splits the image into squares, replaces every square with the average color of its pixels, and shows the result **progressively** in a window while it works. The finished image is saved as `result.jpg`.

Single-threaded (`S`) and multi-threaded (`M`) modes are available so the two can be compared.

## Result

| Original | Modified |
| --- | --- |
|<img width="554" height="554" alt="photo" src="https://github.com/user-attachments/assets/9901f441-4bed-4d24-ab6b-1a5fb02a0077" /> | <img width="554" height="554" alt="result" src="https://github.com/user-attachments/assets/460694a2-bb86-402f-917a-8070176e39e8" /> |

The result above uses the following command: `java -cp out Main photo.jpg 9 M`

## Requirements

- JDK 11 or newer (check with `java -version` and `javac -version`)
- No external libraries or build tools

Works on macOS, Windows and Linux.

## Build

From the project root:

```
javac -d out src/*.java
```

## Run

```
java -cp out Main <file.jpg> <squareSize> <S|M>
```

| Argument | Meaning |
|---|---|
| `file.jpg` | Path to the input JPEG (any size) |
| `squareSize` | Side of the averaging square in pixels (whole number, at least 1) |
| `S` / `M` | `S` = single-threaded, `M` = multi-threaded |

Examples:

```
java -cp out Main photo.jpg 20 S
java -cp out Main photo.jpg 20 M
```

For very large images, give Java more memory (a 10000x10000 image needs about 400 MB):

```
java -Xmx3g -cp out Main huge.jpg 20 M
```

The result is written to `result.jpg` in the folder you ran the command from. Do not use `result.jpg` as the input file.

## How it works

1. The image is loaded at full resolution.
2. A window opens showing the image, scaled down to fit the screen if it is bigger than about 90% of the screen. The window repaints every 30 ms.
3. A background thread processes the **original, full-size image**. Only the drawing is scaled, so resizing never affects the result.
4. Each square is replaced by one flat color. Squares at the right and bottom edges are clipped to the image border, so every pixel is processed and each edge square is averaged over its real pixel count.
5. When processing finishes, the image is saved as `result.jpg`.

### Averaging method

Each square gets the per-channel **root mean square** of its pixels: square every red, green and blue value, take the mean of the squares, then take the square root. Compared with a plain mean, this weights brighter pixels more heavily, so blocks mixing dark and light pixels come out slightly brighter.

### Single-threaded mode (`S`)

One thread walks the image left-to-right, top-to-bottom.

### Multi-threaded mode (`M`)

The number of threads equals the number of available CPU cores (`Runtime.getRuntime().availableProcessors()`). The image is divided into horizontal bands:

- Each band's height is a multiple of the square size, so no square is ever split between two threads.
- Each thread walks its own band left-to-right, top-to-bottom.
- Bands never overlap, so threads never write the same pixels and no locking is needed.
- The main worker waits for all threads to finish before saving.

### Progress pause

Modern CPUs average a photo in a fraction of a second, which would make the result appear instantly. To make the progress visible, each thread pauses briefly after every row of squares. The total pause is aimed at `TARGET_SWEEP_MS` (16 seconds by default) and is capped per row so that huge squares do not cause long idle waits. The pause applies equally to both modes. In `M` mode the pauses overlap, so the sweep finishes sooner and several bands fill in at the same time.

To benchmark real processing speed, set `TARGET_SWEEP_MS = 0` in `Main.java` and recompile.

## Benchmark

Measured with the progress pause disabled (`TARGET_SWEEP_MS = 0`), three runs each, on a 12-thread laptop CPU:

| Image | Square | Mode | Runs (ms) | Typical |
|---|---|---|---|---|
| 10000x10000 | 100 | S | 2751, 2684, 2695 | ~2700 ms |
| 10000x10000 | 100 | M | 1656, 1598, 1653 | ~1630 ms |

Speedup: about **1.65x**.

The speedup is well below the thread count because the workload is memory-bound: each pixel is read and written once with very little arithmetic, so the memory bus saturates after a few threads. `BufferedImage.getRGB`/`setRGB` conversion overhead, the viewer repainting a large image, and the mix of performance and efficiency cores on my CPU might also limit scaling.

## Project structure

```
.
├── README.md
├── .gitignore
└── src/
    ├── Main.java          Argument parsing, processing (S and M), saving
    └── ImageViewer.java   Swing window that scales the image for display and keeps repainting
```

## Notes and limitations

- Input must be a real JPEG. A PNG or HEIC file renamed to `.jpg` might not load.
- Closing the window exits the program immediately. If you close it before processing finishes, `result.jpg` is not saved.
- Very large images can raise `OutOfMemoryError` with the default heap size. Use the `-Xmx` option shown above.
- The timing printed by the program covers processing only, not loading or saving.
