package org.jaudiotagger.audio.wavpack;

import org.jaudiotagger.tag.ape.ApeTagFooter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Utility methods shared by the WavPack tag reader/writer to locate existing APEv2 tags and preserve
 * trailing metadata (Lyrics3, ID3v1, padding) during rewrites.
 */
final class WavPackTagHelper
{
    private static final long MAX_TRAILING_SCAN = 64L * 1024 * 1024;
    private static final byte[] LYRICS_MARKER = "LYRICS200".getBytes(StandardCharsets.US_ASCII);
    private static final int LYRICS_MARKER_LENGTH = 9;
    private static final int LYRICS_SIZE_LENGTH = 6;

    private WavPackTagHelper()
    {
    }

    /**
     * Performs a single pass over the file tail gathering every metadata fragment we need to preserve when rewriting
     * the tag (Lyrics3, ID3v1, padding, existing APE header/footer, etc.).
     */
    static ScanResult scan(RandomAccessFile raf) throws IOException
    {
        Id3Info id3 = readId3(raf);
        long nextMetadataOffset = id3 != null ? id3.startOffset : raf.length();
        Lyrics3Info lyrics3 = readLyrics3(raf, nextMetadataOffset);
        if (lyrics3 != null)
        {
            nextMetadataOffset = lyrics3.startOffset;
        }
        WavPackTagLocation location = locateTrailingTag(raf, nextMetadataOffset);
        byte[] padding = readPaddingBytes(raf, location, nextMetadataOffset);
        if (location == null)
        {
            location = locateStartingTag(raf);
        }
        return new ScanResult(location, id3, lyrics3, padding, nextMetadataOffset, raf.length());
    }

    /**
     * Returns any zero/non-zero padding between the APE tag and the following metadata (usually ID3) so we can
     * re-append it in the same spot after rewriting.
     */
    private static byte[] readPaddingBytes(RandomAccessFile raf, WavPackTagLocation location, long nextMetadataOffset) throws IOException
    {
        if (location == null || location.isAtStart() || !location.hasFooter())
        {
            return new byte[0];
        }
        long afterTag = location.getRemovalEnd();
        if (afterTag >= nextMetadataOffset)
        {
            return new byte[0];
        }
        long paddingLength = nextMetadataOffset - afterTag;
        if (paddingLength <= 0 || paddingLength > Integer.MAX_VALUE)
        {
            return new byte[0];
        }
        byte[] padding = new byte[(int) paddingLength];
        raf.seek(afterTag);
        raf.readFully(padding);
        return padding;
    }

    /**
     * Scans backwards from the end (or the start of trailing metadata) looking for an APE footer. The scan is limited
     * to 64 MiB to avoid pathological runtimes on corrupt files.
     */
    private static WavPackTagLocation locateTrailingTag(RandomAccessFile raf, long limit) throws IOException
    {
        if (limit < ApeTagFooter.FOOTER_SIZE)
        {
            return null;
        }
        long scanStart = Math.max(0, limit - MAX_TRAILING_SCAN);
        long offset = limit;
        while (offset - ApeTagFooter.FOOTER_SIZE >= scanStart)
        {
            long candidate = offset - ApeTagFooter.FOOTER_SIZE;
            raf.seek(candidate);
            ApeTagFooter footer = ApeTagFooter.read(raf);
            if (footer != null && footer.isValid() && !footer.isHeader())
            {
                long tagStart = footer.calculateTagStart(candidate);
                if (tagStart < 0)
                {
                    offset = candidate;
                    continue;
                }
                long dataStart = footer.hasHeader() ? tagStart + ApeTagFooter.FOOTER_SIZE : tagStart;
                long dataLength = footer.getSize() - ApeTagFooter.FOOTER_SIZE;
                if (dataLength < 0)
                {
                    offset = candidate;
                    continue;
                }
                long dataEnd = dataStart + dataLength;
                if (dataEnd != candidate)
                {
                    offset = candidate;
                    continue;
                }
                long headerOffset = footer.hasHeader() ? tagStart : -1;
                return new WavPackTagLocation(dataStart, dataEnd, headerOffset, candidate, footer, false);
            }
            offset = candidate;
        }
        return null;
    }

    /**
     * Detects tags stored at the beginning of the file (header + optional footer). We treat these specially when
     * rewriting to avoid blowing away audio data.
     */
    private static WavPackTagLocation locateStartingTag(RandomAccessFile raf) throws IOException
    {
        if (raf.length() < ApeTagFooter.FOOTER_SIZE)
        {
            return null;
        }
        raf.seek(0);
        ApeTagFooter header = ApeTagFooter.read(raf);
        if (header == null || !header.isValid() || !header.isHeader())
        {
            return null;
        }
        long dataStart = ApeTagFooter.FOOTER_SIZE;
        long dataLength = header.getSize();
        if (header.hasFooter())
        {
            dataLength -= ApeTagFooter.FOOTER_SIZE;
        }
        if (dataLength < 0)
        {
            return null;
        }
        long dataEnd = dataStart + dataLength;
        long footerOffset = header.hasFooter() ? dataEnd : -1;
        if (header.hasFooter())
        {
            if (footerOffset + ApeTagFooter.FOOTER_SIZE > raf.length())
            {
                return null;
            }
        }
        return new WavPackTagLocation(dataStart, dataEnd, 0, footerOffset, header, true);
    }

    /**
     * Lyrics3v2 blocks live between the audio/tags and the ID3v1 footer. We need to preserve the exact bytes (size +
     * marker) so that other taggers still recognise them.
     */
    private static Lyrics3Info readLyrics3(RandomAccessFile raf, long limit) throws IOException
    {
        if (limit < LYRICS_MARKER_LENGTH + LYRICS_SIZE_LENGTH)
        {
            return null;
        }
        long markerPos = limit - LYRICS_MARKER_LENGTH;
        raf.seek(markerPos);
        byte[] marker = new byte[LYRICS_MARKER_LENGTH];
        raf.readFully(marker);
        if (!Arrays.equals(marker, LYRICS_MARKER))
        {
            return null;
        }
        long sizePos = markerPos - LYRICS_SIZE_LENGTH;
        if (sizePos < 0)
        {
            return null;
        }
        raf.seek(sizePos);
        byte[] sizeBytes = new byte[LYRICS_SIZE_LENGTH];
        raf.readFully(sizeBytes);
        int payload;
        try
        {
            payload = Integer.parseInt(new String(sizeBytes, StandardCharsets.US_ASCII));
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
        long start = sizePos - payload;
        if (start < 0)
        {
            return null;
        }
        int total = (int) (limit - start);
        if (total <= 0)
        {
            return null;
        }
        raf.seek(start);
        byte[] data = new byte[total];
        raf.readFully(data);
        return new Lyrics3Info(start, data);
    }

    static Id3Info readId3(RandomAccessFile raf) throws IOException
    {
        if (raf.length() < Id3Info.ID3_LENGTH)
        {
            return null;
        }
        long start = raf.length() - Id3Info.ID3_LENGTH;
        raf.seek(start);
        byte[] data = new byte[Id3Info.ID3_LENGTH];
        raf.readFully(data);
        if (data[0] == 'T' && data[1] == 'A' && data[2] == 'G')
        {
            return new Id3Info(start, data);
        }
        return null;
    }

    /**
     * Removes a byte range by shifting subsequent bytes forward. Used to drop start-of-file tags without rewriting the
     * whole file.
     */
    static void removeRange(RandomAccessFile raf, long start, long end) throws IOException
    {
        if (start < 0 || end <= start)
        {
            return;
        }
        long readPos = end;
        long writePos = start;
        long fileLength = raf.length();
        byte[] buffer = new byte[64 * 1024];
        while (readPos < fileLength)
        {
            int chunk = (int) Math.min(buffer.length, fileLength - readPos);
            if (chunk <= 0)
            {
                break;
            }
            raf.seek(readPos);
            raf.readFully(buffer, 0, chunk);
            raf.seek(writePos);
            raf.write(buffer, 0, chunk);
            readPos += chunk;
            writePos += chunk;
        }
        raf.setLength(writePos);
    }

    static int readLittleEndianInt(RandomAccessFile raf) throws IOException
    {
        byte[] buffer = new byte[4];
        raf.readFully(buffer);
        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    static String readCString(RandomAccessFile raf) throws IOException
    {
        StringBuilder builder = new StringBuilder();
        byte value;
        while ((value = raf.readByte()) != 0)
        {
            builder.append((char) (value & 0xFF));
        }
        return builder.toString();
    }

    static final class ScanResult
    {
        private final WavPackTagLocation location;
        private final Id3Info id3;
        private final Lyrics3Info lyrics3;
        private final byte[] padding;
        private final long nextMetadataOffset;
        private final long fileLength;

        ScanResult(WavPackTagLocation location, Id3Info id3, Lyrics3Info lyrics3, byte[] padding, long nextMetadataOffset, long fileLength)
        {
            this.location = location;
            this.id3 = id3;
            this.lyrics3 = lyrics3;
            this.padding = padding != null ? padding : new byte[0];
            this.nextMetadataOffset = nextMetadataOffset;
            this.fileLength = fileLength;
        }

        WavPackTagLocation getLocation()
        {
            return location;
        }

        Id3Info getId3()
        {
            return id3;
        }

        Lyrics3Info getLyrics3()
        {
            return lyrics3;
        }

        byte[] getPadding()
        {
            return padding;
        }

        long metadataInsertionOffset()
        {
            if (location != null && !location.isAtStart())
            {
                return location.getRemovalStart();
            }
            if (lyrics3 != null)
            {
                return lyrics3.startOffset;
            }
            if (id3 != null)
            {
                return id3.startOffset;
            }
            return fileLength;
        }

        long nextMetadataOffset()
        {
            return nextMetadataOffset;
        }
    }

    static final class Id3Info
    {
        static final int ID3_LENGTH = 128;
        final long startOffset;
        final byte[] data;

        Id3Info(long startOffset, byte[] data)
        {
            this.startOffset = startOffset;
            this.data = data;
        }
    }

    static final class Lyrics3Info
    {
        final long startOffset;
        final byte[] data;

        Lyrics3Info(long startOffset, byte[] data)
        {
            this.startOffset = startOffset;
            this.data = data;
        }
    }
}
