package org.jaudiotagger.kt.tag.vorbiscomment

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jaudiotagger.kt.audio.flac.FlacPictureCodec
import org.jaudiotagger.kt.tag.Artwork
import org.jaudiotagger.kt.tag.FieldKey
import org.jaudiotagger.kt.tag.Tag

/**
 * A single `NAME=value` comment. Names are upper-cased as the Vorbis spec requires.
 */
class VorbisCommentField(id: String, val value: String) {
    val id: String = id.uppercase()

    override fun toString(): String = "$id=$value"

    companion object {
        /** Id assigned to malformed comments that lack the `=` separator. */
        const val ERRONEOUS_ID = "ERRONEOUS"

        /** Parses the raw `NAME=value` form found in the file. */
        fun parse(raw: String): VorbisCommentField {
            val i = raw.indexOf('=')
            return if (i == -1) {
                VorbisCommentField(ERRONEOUS_ID, raw)
            } else {
                VorbisCommentField(raw.substring(0, i), raw.substring(i + 1))
            }
        }
    }
}

/**
 * Vorbis Comment tag, used by FLAC and Ogg Vorbis/Opus.
 *
 * Fields keep file order. The vendor string is kept separate from user comments,
 * as in the binary format; [FieldKey.ENCODER] reads and writes it.
 *
 * Artwork is not stored here: FLAC keeps pictures in separate metadata blocks and
 * Ogg embeds them as base64 `METADATA_BLOCK_PICTURE` comments, so the containing
 * tag ([org.jaudiotagger.kt.tag.flac.FlacTag] for FLAC) implements the artwork API.
 */
class VorbisCommentTag(
    var vendor: String = DEFAULT_VENDOR,
) : Tag {

    private val fields = mutableListOf<VorbisCommentField>()

    val allFields: List<VorbisCommentField> get() = fields

    // ---- string-keyed access (also usable for custom/unmapped field names) ----

    fun firstRaw(id: String): String? {
        val upper = id.uppercase()
        return fields.firstOrNull { it.id == upper }?.value
    }

    fun allRaw(id: String): List<String> {
        val upper = id.uppercase()
        return fields.filter { it.id == upper }.map { it.value }
    }

    fun addRaw(id: String, value: String) {
        fields += VorbisCommentField(id, value)
    }

    fun setRaw(id: String, value: String) {
        val upper = id.uppercase()
        val insertAt = fields.indexOfFirst { it.id == upper }
        fields.removeAll { it.id == upper }
        if (insertAt >= 0) {
            fields.add(insertAt, VorbisCommentField(upper, value))
        } else {
            fields += VorbisCommentField(upper, value)
        }
    }

    fun removeRaw(id: String) {
        val upper = id.uppercase()
        fields.removeAll { it.id == upper }
    }

    internal fun addParsedField(field: VorbisCommentField) {
        fields += field
    }

    // ---- FieldKey access ----

    private fun vorbisKey(key: FieldKey): VorbisCommentFieldKey =
        fieldKeyToVorbisKey[key]
            ?: throw IllegalArgumentException("FieldKey $key is not supported by VorbisComment")

    override fun first(key: FieldKey): String? {
        if (key == FieldKey.ENCODER) return vendor.ifEmpty { null }
        if (key == FieldKey.ALBUM_ARTIST) return albumArtistValues().firstOrNull()
        return firstRaw(vorbisKey(key).fieldName)
    }

    override fun all(key: FieldKey): List<String> {
        if (key == FieldKey.ENCODER) return if (vendor.isEmpty()) emptyList() else listOf(vendor)
        if (key == FieldKey.ALBUM_ARTIST) return albumArtistValues()
        return allRaw(vorbisKey(key).fieldName)
    }

    override fun set(key: FieldKey, value: String) {
        if (key == FieldKey.ENCODER) {
            vendor = value
            return
        }
        if (key == FieldKey.ALBUM_ARTIST) {
            setRaw(VorbisCommentFieldKey.ALBUMARTIST.fieldName, value)
            removeRaw(VorbisCommentFieldKey.ALBUMARTIST_JRIVER.fieldName)
            return
        }
        setRaw(vorbisKey(key).fieldName, value)
    }

    override fun add(key: FieldKey, value: String) {
        if (key == FieldKey.ENCODER) {
            vendor = value
            return
        }
        if (key == FieldKey.ALBUM_ARTIST) {
            addRaw(VorbisCommentFieldKey.ALBUMARTIST.fieldName, value)
            return
        }
        addRaw(vorbisKey(key).fieldName, value)
    }

    override fun remove(key: FieldKey) {
        if (key == FieldKey.ENCODER) {
            vendor = DEFAULT_VENDOR
            return
        }
        if (key == FieldKey.ALBUM_ARTIST) {
            removeRaw(VorbisCommentFieldKey.ALBUMARTIST.fieldName)
            removeRaw(VorbisCommentFieldKey.ALBUMARTIST_JRIVER.fieldName)
            return
        }
        removeRaw(vorbisKey(key).fieldName)
    }

    /** Java default: read ALBUMARTIST, then JRiver's `ALBUM ARTIST`. */
    private fun albumArtistValues(): List<String> {
        val standard = allRaw(VorbisCommentFieldKey.ALBUMARTIST.fieldName)
        if (standard.isNotEmpty()) return standard
        return allRaw(VorbisCommentFieldKey.ALBUMARTIST_JRIVER.fieldName)
    }

    override val fieldCount: Int get() = fields.size

    override val isEmpty: Boolean get() = fields.isEmpty()

    override fun clear() {
        fields.clear()
    }

    // Artwork is carried as base64-encoded FLAC picture blocks in
    // METADATA_BLOCK_PICTURE comments (plus the legacy COVERART field), which is
    // how Ogg embeds pictures. FLAC files use separate PICTURE metadata blocks
    // instead — FlacTag overrides the artwork API accordingly.
    @OptIn(ExperimentalEncodingApi::class)
    override val artworks: List<Artwork>
        get() {
            val result = mutableListOf<Artwork>()
            for (encoded in allRaw(VorbisCommentFieldKey.METADATA_BLOCK_PICTURE.fieldName)) {
                try {
                    result += FlacPictureCodec.decode(Base64.decode(encoded))
                } catch (_: Exception) {
                    // unparsable picture comment: ignore, the rest of the tag is fine
                }
            }
            // legacy pre-METADATA_BLOCK_PICTURE style
            val legacyData = firstRaw(VorbisCommentFieldKey.COVERART.fieldName)
            if (legacyData != null) {
                try {
                    result += Artwork(
                        data = Base64.decode(legacyData),
                        mimeType = firstRaw(VorbisCommentFieldKey.COVERARTMIME.fieldName) ?: "",
                    )
                } catch (_: Exception) {
                }
            }
            return result
        }

    @OptIn(ExperimentalEncodingApi::class)
    override fun addArtwork(artwork: Artwork) {
        addRaw(
            VorbisCommentFieldKey.METADATA_BLOCK_PICTURE.fieldName,
            Base64.encode(FlacPictureCodec.encode(artwork)),
        )
    }

    override fun clearArtworks() {
        removeRaw(VorbisCommentFieldKey.METADATA_BLOCK_PICTURE.fieldName)
        removeRaw(VorbisCommentFieldKey.COVERART.fieldName)
        removeRaw(VorbisCommentFieldKey.COVERARTMIME.fieldName)
    }

    override fun toString(): String = "VorbisCommentTag(vendor=$vendor, fields=$fields)"

    companion object {
        const val DEFAULT_VENDOR = "jaudiotagger"
    }
}
