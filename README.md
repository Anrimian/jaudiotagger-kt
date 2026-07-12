# jaudiotagger-kt

[![JitPack](https://jitpack.io/v/Anrimian/jaudiotagger.svg)](https://jitpack.io/#Anrimian/jaudiotagger)
[![License: LGPL v2.1](https://img.shields.io/badge/License-LGPL_v2.1-blue.svg)](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html)
![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20JVM%20%7C%20iOS%20%7C%20macOS-orange)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-purple)

A **Kotlin Multiplatform rewrite of [jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger)**,
the venerable Java audio tagging library — reading and writing metadata for
12 audio formats with no reflection and no temp-file copies.

> **Full disclosure:** this library was written by **Claude Fable 5**
> (Anthropic's `claude-fable-5` model), porting the original Java sources of
> jaudiotagger 3.0.2 to Kotlin format by format. Parser and writer logic was ported faithfully from the original,
> behaviour was verified against the original test suite's expected values on
> the original test audio files, and a few genuine bugs in the source material
> were found and fixed along the way (see
> [PORTING.md](jaudiotagger-kt/PORTING.md) for the full engineering log).

## Why a rewrite?

The original jaudiotagger is a great library with two decades of format
knowledge baked in, but it shows its age on modern Android:

| | jaudiotagger (Java) | jaudiotagger-kt |
|---|---|---|
| **Reflection** | `Class.forName("FrameBody" + id)` for ID3 frames, reflective ASF reader registration — needs ProGuard/R8 keep rules | **None.** All factories are explicit `when`/map lookups; R8 shrinks the library with no keep rules |
| **File access** | `java.io.File` / `RandomAccessFile` paths only | Everything goes through a small **`FileIo`** interface — on Android you edit **MediaStore/SAF files in place through a `FileDescriptor`**, no temp copies |
| **Platforms** | JVM only | Kotlin Multiplatform: **Android, JVM, iOS, macOS** (all format logic in `commonMain`) |
| **Writes** | Some formats rewrite through temp files | All writers work **in place**: shift audio only when the metadata doesn't fit, patch what must be patched (Ogg page CRCs, MP4 `stco` chunk offsets, ...) |
| **API** | `AudioFileIO.read(file).getTag().getFirst(FieldKey.ARTIST)` | Idiomatic Kotlin: null-safety, `Duration`, sealed frame models |

## Supported formats

| Format | Extensions | Properties | Tag read | Tag write | Tag type |
|---|---|---|---|---|---|
| MP3 | `.mp3` | MPEG header, Xing/VBRI/LAME | ✅ | ✅ | ID3v2.2/2.3/2.4 + ID3v1(.1) |
| FLAC | `.flac` | ✅ | ✅ | ✅ | Vorbis Comment + PICTURE blocks |
| Ogg Vorbis | `.ogg` | ✅ | ✅ | ✅ | Vorbis Comment (base64 artwork) |
| MP4 / AAC / ALAC | `.m4a .mp4 .m4b .m4p` | ✅ | ✅ | ✅ | iTunes `ilst` (incl. reverse-DNS) |
| WAV | `.wav` | ✅ | ✅ | ✅ | ID3v2 chunk + LIST-INFO (synced) |
| AIFF | `.aif .aiff .aifc` | ✅ | ✅ | ✅ | ID3v2 chunk |
| WMA | `.wma` | ✅ | ✅ | ✅ | ASF descriptors (WM/Picture art) |
| Monkey's Audio | `.ape` | ✅ | ✅ | ✅ | APEv2 |
| WavPack | `.wv` | ✅ | ✅ | ✅ | APEv2 |
| DSF | `.dsf` | ✅ | ✅ | ✅ | ID3v2 |
| DFF | `.dff` | ✅ | ✅ | read-only | ID3v2 |
| RealAudio | `.ra .rm` | ✅ | ✅ | read-only | CONT chunk |

## Installation (JitPack)

Add JitPack to your repositories:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then add the dependency:

```kotlin
// Android / KMP common
implementation("com.github.Anrimian.jaudiotagger:jaudiotagger-kt:<version>")

// or explicitly the Android artifact
implementation("com.github.Anrimian.jaudiotagger:jaudiotagger-kt-android:<version>")
```

> JitPack builds the Android, JVM and multiplatform-metadata artifacts
> (its Linux builders cannot compile the Apple targets — build from source
> on a Mac for iOS/macOS).

## Quick start

### Read and write by path

```kotlin
import kotlinx.io.files.Path
import org.jaudiotagger.kt.AudioTagger
import org.jaudiotagger.kt.tag.FieldKey

val file = AudioTagger.read(Path("/music/track.flac"))

println(file.properties.duration)     // kotlin.time.Duration
println(file.properties.sampleRate)   // 44100
println(file.tag.first(FieldKey.ARTIST))

file.tag.set(FieldKey.TITLE, "New title")
file.tag.add(FieldKey.GENRE, "Second genre")
AudioTagger.write(Path("/music/track.flac"), file.tag)
```

### Android: edit MediaStore/SAF files in place — no temp copies

```kotlin
import org.jaudiotagger.kt.AudioFormat
import org.jaudiotagger.kt.AudioTagger
import org.jaudiotagger.kt.io.FileDescriptorIo
import org.jaudiotagger.kt.tag.FieldKey

contentResolver.openFileDescriptor(uri, "rw")!!.use { pfd ->
    val io = FileDescriptorIo(pfd.fileDescriptor)
    val file = AudioTagger.read(io, AudioFormat.MP3)
    file.tag.set(FieldKey.TITLE, "New title")
    AudioTagger.write(io, file.tag, AudioFormat.MP3)
}
```

`FileDescriptorIo` performs positioned reads/writes with `Os.pread`/`Os.pwrite`
and resizes with `Os.ftruncate`, so the tag is edited directly inside the
file MediaStore handed you.

### Artwork

```kotlin
import org.jaudiotagger.kt.tag.Artwork

val cover: Artwork? = file.tag.artworks.firstOrNull()
cover?.data       // raw image bytes — decode with your platform's image API
cover?.mimeType   // "image/jpeg"

file.tag.setArtwork(Artwork(data = jpegBytes, mimeType = "image/jpeg"))
```

## Design notes

- **All format logic lives in `commonMain`** on top of
  [kotlinx-io](https://github.com/Kotlin/kotlinx-io); the only platform code is
  the `FileIo` random-access implementation (`FileChannel` on JVM/Android,
  POSIX descriptors on Apple targets, `Os.pread/pwrite` for Android file
  descriptors).
- **No runtime reflection anywhere** — the mechanical mapping tables
  (582 ID3 field mappings, 189 MP4, 189 ASF, ...) are generated from the
  original Java sources by scripts in [`jaudiotagger-kt/tools/`](jaudiotagger-kt/tools).
- **Tested against the original**: the test suite runs on the original
  project's real audio samples (`testdata/`), asserting the values the
  original JUnit tests expect, plus structural invariants after writes
  (Ogg page CRCs, MP4 chunk-offset integrity, FLAC audio MD5).
- The original Java sources are kept in [`src/`](src) as the porting
  reference; the legacy readme is [README-java.md](README-java.md).

### Known limitations

- ID3v2 compressed frames are preserved as opaque bytes, not decoded
  (no zlib in common Kotlin).
- WMA: binary descriptors larger than 64 KB are dropped on write (they would
  require the ASF Metadata Library object, which is read but not yet written).
- DFF and RealAudio are read-only (the original library did not write them
  either).

## Building

```bash
./gradlew :jaudiotagger-kt:jvmTest          # run the test suite
./gradlew :jaudiotagger-kt:assembleRelease  # Android AAR
./gradlew :jaudiotagger-kt:publishToMavenLocal
```

## License

LGPL-2.1 — this is a derivative work of
[jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger)
© Paul Taylor, Raphaël Slinckx and contributors, and inherits its license.
See [license.txt](license.txt).
