package org.jaudiotagger.kt.mp4

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jaudiotagger.kt.AudioTagger
import org.jaudiotagger.kt.audio.mp4.Mp4Atoms
import org.jaudiotagger.kt.copyToTemp
import org.jaudiotagger.kt.io.readFully
import org.jaudiotagger.kt.io.readInt32BE
import org.jaudiotagger.kt.io.withFileIo
import org.jaudiotagger.kt.tag.Artwork
import org.jaudiotagger.kt.tag.FieldKey
import org.jaudiotagger.kt.tag.mp4.Mp4Tag

class Mp4WriteTest {

    /**
     * Reads the audio bytes addressed by the first chunk offset of every stco
     * box; used to prove chunk offsets still point at the same audio after a
     * rewrite moved things around.
     */
    private fun firstChunkBytes(path: Path): List<ByteArray> = withFileIo(path) { io ->
        val result = mutableListOf<ByteArray>()
        val moov = Mp4Atoms.requirePath(io, "moov")
        fun walk(start: Long, end: Long) {
            Mp4Atoms.forEachChild(io, start, end) { atom ->
                when (atom.id) {
                    "trak", "mdia", "minf", "stbl" -> walk(atom.dataStart, atom.dataEnd)
                    "stco" -> {
                        io.position = atom.dataStart
                        val head = io.readFully(12)
                        if (head.readInt32BE(4) > 0) {
                            val offset = head.readInt32BE(8).toUInt().toLong()
                            io.position = offset
                            result += io.readFully(minOf(64, (io.size - offset).toInt()))
                        }
                    }
                }
            }
        }
        walk(moov.dataStart, moov.dataEnd)
        result
    }

    @Test
    fun rewriteFieldsAndGrowTag() {
        val path = copyToTemp("test.m4a", "mp4-write")
        val originalDuration = AudioTagger.read(path).properties.duration
        val audioBefore = firstChunkBytes(path)
        assertTrue(audioBefore.isNotEmpty(), "sanity: stco must be found")

        val tag = AudioTagger.read(path).tag as Mp4Tag
        tag.set(FieldKey.TITLE, "New Tïtle 日本語")
        tag.set(FieldKey.ARTIST, "New Artist")
        tag.set(FieldKey.MUSICBRAINZ_RELEASEID, "uuid-1234")
        tag.set(FieldKey.TRACK, "3")
        tag.set(FieldKey.TRACK_TOTAL, "12")
        // large artwork forces the moov to grow beyond any free atom
        val imageBytes = ByteArray(60_000) { (it * 11).toByte() }
        tag.setArtwork(Artwork(data = imageBytes, mimeType = "image/jpeg"))
        AudioTagger.write(path, tag)

        val reread = AudioTagger.read(path)
        val rereadTag = assertIs<Mp4Tag>(reread.tag)
        assertEquals("New Tïtle 日本語", rereadTag.first(FieldKey.TITLE))
        assertEquals("New Artist", rereadTag.first(FieldKey.ARTIST))
        assertEquals("uuid-1234", rereadTag.first(FieldKey.MUSICBRAINZ_RELEASEID))
        assertEquals("3", rereadTag.first(FieldKey.TRACK))
        assertEquals("12", rereadTag.first(FieldKey.TRACK_TOTAL))
        assertEquals("Album", rereadTag.first(FieldKey.ALBUM), "untouched field must survive")
        assertEquals(1, rereadTag.artworks.size)
        assertTrue(rereadTag.artworks[0].data.contentEquals(imageBytes))
        assertEquals(originalDuration, reread.properties.duration)

        // chunk offsets must still point at the same audio bytes
        assertEquals(audioBefore.size, firstChunkBytes(path).size)
        audioBefore.zip(firstChunkBytes(path)).forEachIndexed { index, (before, after) ->
            assertTrue(before.contentEquals(after), "audio chunk $index moved incorrectly")
        }

        SystemFileSystem.delete(path)
    }

    @Test
    fun shrinkTagAndDelete() {
        val path = copyToTemp("test.m4a", "mp4-shrink")
        val originalDuration = AudioTagger.read(path).properties.duration
        val audioBefore = firstChunkBytes(path)

        val tag = AudioTagger.read(path).tag as Mp4Tag
        tag.clear()
        tag.set(FieldKey.TITLE, "t")
        AudioTagger.write(path, tag)

        val reread = AudioTagger.read(path)
        assertEquals("t", reread.tag.first(FieldKey.TITLE))
        assertEquals(null, reread.tag.first(FieldKey.ALBUM))
        assertEquals(originalDuration, reread.properties.duration)

        AudioTagger.deleteTag(path)
        val deleted = AudioTagger.read(path)
        assertEquals(null, deleted.tag.first(FieldKey.TITLE))
        assertEquals(originalDuration, deleted.properties.duration)

        audioBefore.zip(firstChunkBytes(path)).forEachIndexed { index, (before, after) ->
            assertTrue(before.contentEquals(after), "audio chunk $index moved incorrectly")
        }

        SystemFileSystem.delete(path)
    }

    @Test
    fun roundTripSeveralSamples() {
        for (sample in listOf("test2.m4a", "test4.m4a", "test8.m4a", "test164.m4a", "test.stem.mp4")) {
            if (SystemFileSystem.metadataOrNull(org.jaudiotagger.kt.testDataPath(sample)) == null) continue
            val path = copyToTemp(sample, "mp4-roundtrip")
            val originalDuration = AudioTagger.read(path).properties.duration
            val audioBefore = firstChunkBytes(path)

            val tag = AudioTagger.read(path).tag as Mp4Tag
            tag.set(FieldKey.ALBUM, "Round Trip Album")
            AudioTagger.write(path, tag)

            val reread = AudioTagger.read(path)
            assertEquals("Round Trip Album", reread.tag.first(FieldKey.ALBUM), sample)
            assertEquals(originalDuration, reread.properties.duration, sample)
            audioBefore.zip(firstChunkBytes(path)).forEachIndexed { index, (before, after) ->
                assertTrue(before.contentEquals(after), "$sample audio chunk $index moved incorrectly")
            }

            SystemFileSystem.delete(path)
        }
    }
}
