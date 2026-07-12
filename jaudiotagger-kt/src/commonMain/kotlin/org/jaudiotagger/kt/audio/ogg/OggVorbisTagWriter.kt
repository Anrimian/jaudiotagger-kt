package org.jaudiotagger.kt.audio.ogg

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import org.jaudiotagger.kt.CannotWriteException
import org.jaudiotagger.kt.io.FileIo
import org.jaudiotagger.kt.io.ShiftData
import org.jaudiotagger.kt.io.readFully
import org.jaudiotagger.kt.tag.vorbiscomment.VorbisCommentCodec
import org.jaudiotagger.kt.tag.vorbiscomment.VorbisCommentTag

/**
 * Writes a Vorbis Comment tag into an Ogg Vorbis stream in place.
 *
 * The original jaudiotagger rewrote the whole file into a temp file; here the
 * new header pages (comment + setup) are built in memory, the audio is shifted
 * only when the header area changes size, and page sequence numbers/CRCs of
 * subsequent pages are only rewritten when the header page count changes.
 * This allows editing through a plain file descriptor (Android MediaStore/SAF).
 */
internal object OggVorbisTagWriter {

    fun delete(io: FileIo) {
        write(io, VorbisCommentTag())
    }

    fun write(io: FileIo, tag: VorbisCommentTag) {
        val sizes = OggVorbisTagReader.readOggVorbisHeaderSizes(io)
        val newComment = encodeCommentPacket(tag)

        // Setup header plus any packets sharing its last page; page layout of the
        // original does not matter, we get one contiguous byte blob
        val setupData = OggVorbisTagReader.readSetupHeaderAndExtraPackets(io, sizes.setupHeaderStartPosition)

        val newPages = buildNewHeaderPages(sizes, newComment, setupData)

        val oldStart = sizes.commentHeaderStartPosition
        val oldEnd = sizes.lastHeaderPageEndPosition
        val delta = newPages.data.size - (oldEnd - oldStart).toInt()

        if (delta > 0) {
            io.position = oldEnd
            ShiftData.shiftDataByOffsetToMakeSpace(io, delta)
        } else if (delta < 0) {
            io.position = oldEnd
            ShiftData.shiftDataByOffsetToShrinkSpace(io, -delta)
        }

        io.position = oldStart
        io.write(newPages.data)

        if (newPages.pageCount != sizes.oldHeaderPageCount) {
            renumberSubsequentPages(
                io,
                startPosition = oldStart + newPages.data.size,
                nextSequence = sizes.secondPageHeader.pageSequence + newPages.pageCount,
            )
        }
        io.flush()
    }

    /** Comment packet: [type 0x03]["vorbis"][comment data][framing bit 0x01]. */
    private fun encodeCommentPacket(tag: VorbisCommentTag): ByteArray {
        val buffer = Buffer()
        buffer.writeByte(VorbisPacketType.COMMENT_HEADER.type.toByte())
        buffer.write(VorbisHeader.CAPTURE_PATTERN.encodeToByteArray())
        buffer.write(VorbisCommentCodec.encode(tag))
        buffer.writeByte(0x01)
        return buffer.readByteArray()
    }

    private class NewHeaderPages(val data: ByteArray, val pageCount: Int)

    /**
     * Builds the replacement pages holding the comment and setup headers,
     * following the same pagination strategy as the original jaudiotagger.
     */
    private fun buildNewHeaderPages(
        sizes: OggVorbisTagReader.OggVorbisHeaderSizes,
        newComment: ByteArray,
        setupData: ByteArray,
    ): NewHeaderPages {
        val template = sizes.secondPageHeader
        val setupSize = sizes.setupHeaderSize
        val extraPackets = sizes.extraPackets
        val out = Buffer()
        var pageCount = 0
        var pageSequence = template.pageSequence

        if (fitsOnASinglePage(newComment.size, setupSize, extraPackets)) {
            // comment and setup (and extras) all go on one page
            val segmentTable = createSegmentTable(newComment.size, setupSize, extraPackets)
            val page = buildPage(template, segmentTable, pageSequence, continued = false) {
                it.write(newComment)
                it.write(setupData)
            }
            out.write(page)
            pageCount++
            return NewHeaderPages(out.readByteArray(), pageCount)
        }

        // comment does not fit: spread it over complete pages first
        val completePages = newComment.size / OggPageHeader.MAXIMUM_PAGE_DATA_SIZE
        var commentOffset = 0
        for (i in 0 until completePages) {
            val segmentTable = createSegments(OggPageHeader.MAXIMUM_PAGE_DATA_SIZE, quitStream = false)
            val page = buildPage(template, segmentTable, pageSequence, continued = i != 0) {
                it.write(newComment, commentOffset, commentOffset + OggPageHeader.MAXIMUM_PAGE_DATA_SIZE)
            }
            out.write(page)
            pageCount++
            pageSequence++
            commentOffset += OggPageHeader.MAXIMUM_PAGE_DATA_SIZE
        }

        val lastCommentPartSize = newComment.size % OggPageHeader.MAXIMUM_PAGE_DATA_SIZE

        if (!fitsOnASinglePage(lastCommentPartSize, setupSize, extraPackets)) {
            // comment tail and setup header go on separate pages
            run {
                val segmentTable = createSegments(lastCommentPartSize, quitStream = true)
                val page = buildPage(template, segmentTable, pageSequence, continued = completePages > 0) {
                    it.write(newComment, commentOffset, newComment.size)
                }
                out.write(page)
                pageCount++
                pageSequence++
            }
            run {
                val segmentTable = createSegmentTable(setupSize, extraPackets)
                val page = buildPage(template, segmentTable, pageSequence, continued = false) {
                    it.write(setupData)
                }
                out.write(page)
                pageCount++
                pageSequence++
            }
        } else {
            // comment tail, setup header and extras all fit on the final page
            val segmentTable = createSegmentTable(lastCommentPartSize, setupSize, extraPackets)
            val page = buildPage(template, segmentTable, pageSequence, continued = true) {
                it.write(newComment, commentOffset, newComment.size)
                it.write(setupData)
            }
            out.write(page)
            pageCount++
            pageSequence++
        }

        return NewHeaderPages(out.readByteArray(), pageCount)
    }

    /**
     * Builds one page: fixed header copied from [template], the given segment
     * table, data written by [writeData], patched sequence number/flags and a
     * freshly stamped CRC.
     */
    private fun buildPage(
        template: OggPageHeader,
        segmentTable: ByteArray,
        pageSequence: Int,
        continued: Boolean,
        writeData: (Buffer) -> Unit,
    ): ByteArray {
        val buffer = Buffer()
        buffer.write(template.rawHeaderData, 0, OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH - 1)
        buffer.writeByte(segmentTable.size.toByte())
        buffer.write(segmentTable)
        writeData(buffer)

        val page = buffer.readByteArray()
        writeInt32LE(page, OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, pageSequence)
        if (continued) {
            page[OggPageHeader.FIELD_HEADER_TYPE_FLAG_POS] =
                OggPageHeader.HeaderTypeFlag.CONTINUED_PACKET.fileValue
        }
        OggCrc.stampCrc(page)
        return page
    }

    /**
     * Rewrites the page sequence numbers (and therefore CRCs) of all pages from
     * [startPosition] to the end of the file. An invalid trailing ID3v1 tag is
     * truncated away (files in the wild sometimes carry one).
     */
    private fun renumberSubsequentPages(io: FileIo, startPosition: Long, nextSequence: Int) {
        var position = startPosition
        var sequence = nextSequence

        while (position < io.size) {
            io.position = position
            val header = try {
                OggPageHeader.read(io)
            } catch (e: Exception) {
                io.position = position
                val trailing = io.readFully(minOf(3, (io.size - position).toInt()))
                if (trailing.decodeToString() == "TAG") {
                    io.truncate(position)
                    return
                }
                throw CannotWriteException("Error rewriting page sequence numbers", e)
            }

            val pageSize = header.headerLength + header.pageLength
            io.position = position
            val page = io.readFully(pageSize)
            writeInt32LE(page, OggPageHeader.FIELD_PAGE_SEQUENCE_NO_POS, sequence)
            OggCrc.stampCrc(page)
            io.position = position
            io.write(page)

            position += pageSize
            sequence++
        }
    }

    private fun writeInt32LE(target: ByteArray, offset: Int, value: Int) {
        target[offset] = value.toByte()
        target[offset + 1] = (value ushr 8).toByte()
        target[offset + 2] = (value ushr 16).toByte()
        target[offset + 3] = (value ushr 24).toByte()
    }

    /** Segment table for a page holding the comment (or its tail), setup header and extras. */
    private fun createSegmentTable(
        commentLength: Int,
        setupHeaderLength: Int,
        extraPackets: List<OggPageHeader.PacketStartAndLength>,
    ): ByteArray {
        val buffer = Buffer()
        // comment ends on this page so a length that is a multiple of 255 needs a
        // terminating zero lacing value
        buffer.write(createSegments(commentLength, quitStream = true))
        // matches jaudiotagger: without extras the setup segments are left "open"
        buffer.write(createSegments(setupHeaderLength, quitStream = extraPackets.isNotEmpty()))
        for (packet in extraPackets) {
            buffer.write(createSegments(packet.length, quitStream = false))
        }
        return buffer.readByteArray()
    }

    /** Segment table for a page holding only the setup header and extras. */
    private fun createSegmentTable(
        setupHeaderLength: Int,
        extraPackets: List<OggPageHeader.PacketStartAndLength>,
    ): ByteArray {
        val buffer = Buffer()
        buffer.write(createSegments(setupHeaderLength, quitStream = true))
        for (packet in extraPackets) {
            buffer.write(createSegments(packet.length, quitStream = false))
        }
        return buffer.readByteArray()
    }

    /**
     * Lacing values summing to [length]: 255s followed by the remainder. With
     * [quitStream] a length that is an exact multiple of 255 gets a terminating
     * zero so the packet does not appear to continue.
     */
    private fun createSegments(length: Int, quitStream: Boolean): ByteArray {
        if (length == 0) {
            return ByteArray(1)
        }
        val size = length / OggPageHeader.MAXIMUM_SEGMENT_SIZE +
            (if (length % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0 && !quitStream) 0 else 1)
        val result = ByteArray(size)
        for (i in 0 until size - 1) {
            result[i] = 0xFF.toByte()
        }
        result[size - 1] = (length - (size - 1) * OggPageHeader.MAXIMUM_SEGMENT_SIZE).toByte()
        return result
    }

    /** Number of lacing values [length] needs when the packet terminates on the page. */
    private fun segmentsNeeded(length: Int): Int {
        if (length == 0) return 1
        var count = length / OggPageHeader.MAXIMUM_SEGMENT_SIZE + 1
        if (length % OggPageHeader.MAXIMUM_SEGMENT_SIZE == 0) count++
        return count
    }

    private fun fitsOnASinglePage(
        commentLength: Int,
        setupHeaderLength: Int,
        extraPackets: List<OggPageHeader.PacketStartAndLength>,
    ): Boolean {
        var totalSegments = segmentsNeeded(commentLength) + segmentsNeeded(setupHeaderLength)
        for (packet in extraPackets) {
            totalSegments += segmentsNeeded(packet.length)
        }
        return totalSegments <= OggPageHeader.MAXIMUM_NO_OF_SEGMENT_SIZE
    }
}
