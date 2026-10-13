package org.jaudiotagger.kt

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jaudiotagger.kt.tag.FieldKey
import org.jaudiotagger.kt.tag.ape.ApeTag
import org.jaudiotagger.kt.tag.asf.AsfTag
import org.jaudiotagger.kt.tag.flac.FlacTag
import org.jaudiotagger.kt.tag.generic.GenericTag
import org.jaudiotagger.kt.tag.id3.Id3v1Tag
import org.jaudiotagger.kt.tag.id3.Id3v2Tag
import org.jaudiotagger.kt.tag.id3.Id3v2Version
import org.jaudiotagger.kt.tag.mp4.Mp4Tag
import org.jaudiotagger.kt.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.kt.tag.wav.WavTag

class SupportsMultipleValuesTest {

    @Test
    fun `VorbisCommentTag allows multiple values for plain text fields`() {
        val tag = VorbisCommentTag()
        assertTrue(tag.supportsMultipleValues(FieldKey.GENRE))
        assertTrue(tag.supportsMultipleValues(FieldKey.ARTIST))
        assertTrue(tag.supportsMultipleValues(FieldKey.ALBUM_ARTIST))
    }

    @Test
    fun `VorbisCommentTag rejects single-value and special fields`() {
        val tag = VorbisCommentTag()
        assertFalse(tag.supportsMultipleValues(FieldKey.ENCODER))
        assertFalse(tag.supportsMultipleValues(FieldKey.COVER_ART))
        assertFalse(tag.supportsMultipleValues(FieldKey.TRACK))
        assertFalse(tag.supportsMultipleValues(FieldKey.DISC_NO))
        assertFalse(tag.supportsMultipleValues(FieldKey.TRACK_TOTAL))
        assertFalse(tag.supportsMultipleValues(FieldKey.DISC_TOTAL))
    }

    @Test
    fun `FlacTag delegates supportsMultipleValues to its Vorbis comment block`() {
        val tag = FlacTag()
        assertTrue(tag.supportsMultipleValues(FieldKey.GENRE))
        assertFalse(tag.supportsMultipleValues(FieldKey.TRACK))
    }

    @Test
    fun `Id3v2Tag V24 allows multiple values for plain T text frames`() {
        val tag = Id3v2Tag(Id3v2Version.V24)
        assertTrue(tag.supportsMultipleValues(FieldKey.GENRE))
        assertTrue(tag.supportsMultipleValues(FieldKey.ARTIST))
    }

    @Test
    fun `Id3v2Tag V24 rejects comments lyrics numbers and user text frames`() {
        val tag = Id3v2Tag(Id3v2Version.V24)
        assertFalse(tag.supportsMultipleValues(FieldKey.COMMENT))
        assertFalse(tag.supportsMultipleValues(FieldKey.LYRICS))
        assertFalse(tag.supportsMultipleValues(FieldKey.TRACK))
        assertFalse(tag.supportsMultipleValues(FieldKey.DISC_TOTAL))
        assertFalse(tag.supportsMultipleValues(FieldKey.MUSICBRAINZ_RELEASEID))
    }

    @Test
    fun `Id3v2Tag V23 and V22 report no multi-value text frames`() {
        assertFalse(Id3v2Tag(Id3v2Version.V23).supportsMultipleValues(FieldKey.GENRE))
        assertFalse(Id3v2Tag(Id3v2Version.V22).supportsMultipleValues(FieldKey.GENRE))
    }

    @Test
    fun `AsfTag allows multiple values only for multi-valued descriptor names`() {
        val tag = AsfTag()
        assertTrue(tag.supportsMultipleValues(FieldKey.GENRE))
        assertFalse(tag.supportsMultipleValues(FieldKey.TITLE))
    }

    @Test
    fun `formats without multi-value storage report false for genre`() {
        assertFalse(Mp4Tag().supportsMultipleValues(FieldKey.GENRE))
        assertFalse(WavTag().supportsMultipleValues(FieldKey.GENRE))
        assertFalse(WavTag(Id3v2Tag(Id3v2Version.V24)).supportsMultipleValues(FieldKey.GENRE))
        assertFalse(ApeTag().supportsMultipleValues(FieldKey.GENRE))
        assertFalse(GenericTag().supportsMultipleValues(FieldKey.GENRE))
        assertFalse(Id3v1Tag().supportsMultipleValues(FieldKey.GENRE))
    }

    @Test
    fun `Id3v1Tag returns false for unmapped keys without throwing`() {
        assertFalse(Id3v1Tag().supportsMultipleValues(FieldKey.ALBUM_ARTIST))
    }
}
