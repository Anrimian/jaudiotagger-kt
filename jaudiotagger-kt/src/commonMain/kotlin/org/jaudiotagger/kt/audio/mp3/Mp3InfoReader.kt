package org.jaudiotagger.kt.audio.mp3

import kotlin.time.Duration.Companion.seconds
import org.jaudiotagger.kt.AudioException
import org.jaudiotagger.kt.CannotReadException
import org.jaudiotagger.kt.audio.id3.Id3v2Detector
import org.jaudiotagger.kt.io.FileIo

/**
 * Finds the first MPEG audio frame (skipping a leading ID3v2 tag and any
 * garbage) and derives the audio properties, using the Xing/VBRI header for
 * exact VBR frame counts when present.
 */
internal object Mp3InfoReader {

    private const val FILE_BUFFER_SIZE = 5000
    private const val MIN_BUFFER_REMAINING_REQUIRED =
        MpegFrameHeader.HEADER_SIZE + XingFrame.MAX_BUFFER_SIZE_NEEDED

    private val channelModeNames = mapOf(
        MpegFrameHeader.MODE_STEREO to "Stereo",
        MpegFrameHeader.MODE_JOINT_STEREO to "Joint Stereo",
        MpegFrameHeader.MODE_DUAL_CHANNEL to "Dual",
        MpegFrameHeader.MODE_MONO to "Mono",
    )

    fun read(io: FileIo): Mp3AudioProperties {
        io.position = 0
        Id3v2Detector.skipId3TagIfPresent(io)
        val startByte = io.position

        var window = readWindow(io, startByte)
        var windowStart = startByte
        var index = 0

        var frameHeader: MpegFrameHeader? = null
        var xingFrame: XingFrame? = null
        var vbriFrame: VbriFrame? = null
        var headerPosition = startByte

        scan@ while (true) {
            if (window.size - index <= MIN_BUFFER_REMAINING_REQUIRED) {
                windowStart += index
                window = readWindow(io, windowStart)
                index = 0
                if (window.size <= MIN_BUFFER_REMAINING_REQUIRED) {
                    throw CannotReadException("No audio header found within the file")
                }
            }

            if (MpegFrameHeader.isMpegFrame(window, index)) {
                try {
                    val candidate = MpegFrameHeader.parse(window, index)
                    headerPosition = windowStart + index

                    val xing = XingFrame.parseIfPresent(window, index, candidate)
                    if (xing != null) {
                        frameHeader = candidate
                        xingFrame = xing
                        break@scan
                    }
                    val vbri = VbriFrame.parseIfPresent(window, index)
                    if (vbri != null) {
                        frameHeader = candidate
                        vbriFrame = vbri
                        break@scan
                    }
                    // No VBR header: confirm the sync by checking that another
                    // valid frame follows, to avoid false syncs inside tag data
                    if (isNextFrameValid(io, window, windowStart, index, candidate)) {
                        frameHeader = candidate
                        break@scan
                    }
                } catch (_: AudioException) {
                    // invalid candidate header: keep scanning
                }
            }
            index++
        }

        val header = frameHeader!!
        val fileSize = io.size
        val frameLength = header.frameLength
        if (frameLength <= 0) throw CannotReadException("Invalid frame length")

        val numberOfFramesEstimate = (fileSize - headerPosition) / frameLength
        val numberOfFrames = when {
            xingFrame != null && xingFrame.frameCount > 0 -> xingFrame.frameCount.toLong()
            vbriFrame != null && vbriFrame.frameCount > 0 -> vbriFrame.frameCount.toLong()
            else -> numberOfFramesEstimate
        }

        var timePerFrame = header.noOfSamples / header.samplingRate.toDouble()
        // mirrors the frame-length halving quirk for MPEG-2(.5) Layer II/III mono
        if (header.version == MpegFrameHeader.VERSION_2 || header.version == MpegFrameHeader.VERSION_2_5) {
            if ((header.layer == MpegFrameHeader.LAYER_II || header.layer == MpegFrameHeader.LAYER_III) &&
                header.numberOfChannels == 1
            ) {
                timePerFrame /= 2
            }
        }
        val trackLength = numberOfFrames * timePerFrame

        val isVbr = (xingFrame != null && xingFrame.isVbr) || vbriFrame != null
        val bitRate: Int = when {
            xingFrame != null && xingFrame.isVbr && xingFrame.audioSize > 0 ->
                (xingFrame.audioSize * 8 / (timePerFrame * numberOfFrames * 1000)).toInt()

            xingFrame != null && xingFrame.isVbr ->
                ((fileSize - headerPosition) * 8 / (timePerFrame * numberOfFrames * 1000)).toInt()

            vbriFrame != null && vbriFrame.audioSize > 0 ->
                (vbriFrame.audioSize * 8 / (timePerFrame * numberOfFrames * 1000)).toInt()

            vbriFrame != null ->
                ((fileSize - headerPosition) * 8 / (timePerFrame * numberOfFrames * 1000)).toInt()

            else -> header.bitRate
        }

        val encoder = xingFrame?.encoder ?: vbriFrame?.encoder ?: ""

        return Mp3AudioProperties(
            encodingType = "${header.versionAsString} ${header.layerAsString}",
            sampleRate = header.samplingRate,
            channels = header.numberOfChannels,
            bitRate = bitRate,
            isVariableBitRate = isVbr,
            duration = trackLength.seconds,
            audioDataStartPosition = headerPosition,
            audioDataEndPosition = fileSize,
            numberOfFrames = numberOfFrames,
            encoder = encoder,
            channelMode = channelModeNames.getValue(header.channelMode),
        )
    }

    private fun readWindow(io: FileIo, position: Long): ByteArray {
        val length = minOf(FILE_BUFFER_SIZE.toLong(), io.size - position).coerceAtLeast(0)
        if (length == 0L) return ByteArray(0)
        io.position = position
        val buffer = ByteArray(length.toInt())
        var read = 0
        while (read < buffer.size) {
            val n = io.read(buffer, read, buffer.size - read)
            if (n <= 0) break
            read += n
        }
        return if (read == buffer.size) buffer else buffer.copyOf(read)
    }

    /** Checks that a parseable frame follows the candidate frame. */
    private fun isNextFrameValid(
        io: FileIo,
        window: ByteArray,
        windowStart: Long,
        index: Int,
        header: MpegFrameHeader,
    ): Boolean {
        val frameLength = header.frameLength
        if (frameLength > FILE_BUFFER_SIZE - MIN_BUFFER_REMAINING_REQUIRED) return false
        if (frameLength <= 0) return false

        var buffer = window
        var offset = index
        if (buffer.size - offset <= MIN_BUFFER_REMAINING_REQUIRED + frameLength) {
            buffer = readWindow(io, windowStart + index)
            offset = 0
            if (buffer.size <= MIN_BUFFER_REMAINING_REQUIRED + frameLength) return false
        }

        val next = offset + frameLength
        if (next + MpegFrameHeader.HEADER_SIZE > buffer.size) return false
        if (!MpegFrameHeader.isMpegFrame(buffer, next)) return false
        return try {
            MpegFrameHeader.parse(buffer, next)
            true
        } catch (_: AudioException) {
            false
        }
    }
}
