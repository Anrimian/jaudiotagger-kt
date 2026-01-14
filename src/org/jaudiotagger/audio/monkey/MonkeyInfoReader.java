package org.jaudiotagger.audio.monkey;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.GenericAudioHeader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Extracts basic audio information from the Monkey's Audio header.
 */
final class MonkeyInfoReader
{
    GenericAudioHeader read(Path path) throws CannotReadException, IOException
    {
        File file = path.toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
        {
            Descriptor descriptor = Descriptor.read(raf);
            if (descriptor == null)
            {
                throw new CannotReadException("Not a Monkey's Audio file");
            }
            Header header = Header.read(raf, descriptor);
            if (header == null)
            {
                throw new CannotReadException("Invalid Monkey's Audio header");
            }
            if (header.sampleRate <= 0)
            {
                throw new CannotReadException("Invalid sample rate in Monkey's Audio header");
            }
            GenericAudioHeader info = new GenericAudioHeader();
            info.setFormat("Monkey's Audio");
            info.setEncodingType("Monkey's Audio " + descriptor.getVersionString());
            info.setLossless(true);
            info.setVariableBitRate(true);
            info.setChannelNumber(header.channels);
            info.setBitsPerSample(header.bitsPerSample);
            info.setSamplingRate((int) header.sampleRate);
            long totalSamples = header.getTotalSamples();
            if (totalSamples > 0)
            {
                info.setNoOfSamples(totalSamples);
                double seconds = (double) totalSamples / (double) header.sampleRate;
                info.setPreciseLength(seconds);
            }
            long frameBytes = descriptor.getFrameBytes();
            if (frameBytes > 0)
            {
                info.setAudioDataLength(frameBytes);
                if (totalSamples > 0)
                {
                    double seconds = (double) totalSamples / (double) header.sampleRate;
                    if (seconds > 0)
                    {
                        int bitRate = (int) Math.round((frameBytes * 8.0d) / seconds / 1000.0d);
                        info.setBitRate(bitRate);
                    }
                }
            }
            return info;
        }
    }

    private static final class Descriptor
    {
        private static final byte[] SIGNATURE = "MAC ".getBytes(StandardCharsets.US_ASCII);

        private final int version;
        private final long descriptorBytes;
        private final long headerBytes;
        private final long seekTableBytes;
        private final long headerDataBytes;
        private final long apeFrameBytesLow;
        private final long apeFrameBytesHigh;
        private final long terminatingDataBytes;

        private Descriptor(int version,
                           long descriptorBytes,
                           long headerBytes,
                           long seekTableBytes,
                           long headerDataBytes,
                           long apeFrameBytesLow,
                           long apeFrameBytesHigh,
                           long terminatingDataBytes)
        {
            this.version = version;
            this.descriptorBytes = descriptorBytes;
            this.headerBytes = headerBytes;
            this.seekTableBytes = seekTableBytes;
            this.headerDataBytes = headerDataBytes;
            this.apeFrameBytesLow = apeFrameBytesLow;
            this.apeFrameBytesHigh = apeFrameBytesHigh;
            this.terminatingDataBytes = terminatingDataBytes;
        }

        static Descriptor read(RandomAccessFile raf) throws IOException
        {
            long start = raf.getFilePointer();
            byte[] marker = new byte[4];
            raf.readFully(marker);
            if (!Arrays.equals(marker, SIGNATURE))
            {
                return null;
            }
            int version = Short.toUnsignedInt(readShortLE(raf));
            long descriptorBytes = Integer.toUnsignedLong(readIntLE(raf));
            long headerBytes = Integer.toUnsignedLong(readIntLE(raf));
            long seekTableBytes = Integer.toUnsignedLong(readIntLE(raf));
            long headerDataBytes = Integer.toUnsignedLong(readIntLE(raf));
            long apeFrameBytes = Integer.toUnsignedLong(readIntLE(raf));
            long apeFrameBytesHigh = Integer.toUnsignedLong(readIntLE(raf));
            long terminatingDataBytes = Integer.toUnsignedLong(readIntLE(raf));
            byte[] md5 = new byte[16];
            raf.readFully(md5);
            long bytesRead = raf.getFilePointer() - start;
            if (descriptorBytes > bytesRead)
            {
                long skip = descriptorBytes - bytesRead;
                if (skip > 0)
                {
                    raf.seek(raf.getFilePointer() + skip);
                }
            }
            return new Descriptor(version,
                    descriptorBytes,
                    headerBytes,
                    seekTableBytes,
                    headerDataBytes,
                    apeFrameBytes,
                    apeFrameBytesHigh,
                    terminatingDataBytes);
        }

        long getFrameBytes()
        {
            return (apeFrameBytesHigh << 32) | (apeFrameBytesLow & 0xFFFFFFFFL);
        }

        long getHeaderBytes()
        {
            return headerBytes;
        }

        long getSeekTableBytes()
        {
            return seekTableBytes;
        }

        long getHeaderDataBytes()
        {
            return headerDataBytes;
        }

        long getTerminatingDataBytes()
        {
            return terminatingDataBytes;
        }

        String getVersionString()
        {
            int major = version / 1000;
            int minor = version % 1000;
            if (minor % 10 == 0)
            {
                minor /= 10;
            }
            return major + "." + String.format("%03d", minor);
        }
    }

    private static final class Header
    {
        private final long compressionLevel;
        private final long formatFlags;
        private final long blocksPerFrame;
        private final long finalFrameBlocks;
        private final long totalFrames;
        private final int bitsPerSample;
        private final int channels;
        private final long sampleRate;

        private Header(long compressionLevel,
                       long formatFlags,
                       long blocksPerFrame,
                       long finalFrameBlocks,
                       long totalFrames,
                       int bitsPerSample,
                       int channels,
                       long sampleRate)
        {
            this.compressionLevel = compressionLevel;
            this.formatFlags = formatFlags;
            this.blocksPerFrame = blocksPerFrame;
            this.finalFrameBlocks = finalFrameBlocks;
            this.totalFrames = totalFrames;
            this.bitsPerSample = bitsPerSample;
            this.channels = channels;
            this.sampleRate = sampleRate;
        }

        static Header read(RandomAccessFile raf, Descriptor descriptor) throws IOException
        {
            long headerBytes = descriptor.getHeaderBytes();
            if (headerBytes < 24)
            {
                return null;
            }
            byte[] headerData = new byte[(int) headerBytes];
            raf.readFully(headerData);
            ByteBuffer bb = ByteBuffer.wrap(headerData).order(ByteOrder.LITTLE_ENDIAN);
            long compressionLevel = Integer.toUnsignedLong(bb.getInt());
            long formatFlags = Integer.toUnsignedLong(bb.getInt());
            long blocksPerFrame = Integer.toUnsignedLong(bb.getInt());
            long finalFrameBlocks = Integer.toUnsignedLong(bb.getInt());
            long totalFrames = Integer.toUnsignedLong(bb.getInt());
            int bitsPerSample = Short.toUnsignedInt(bb.getShort());
            int channels = Short.toUnsignedInt(bb.getShort());
            long sampleRate = Integer.toUnsignedLong(bb.getInt());
            // Advance file pointer past seek table + header data so subsequent reads (if any) align
            long skip = descriptor.getSeekTableBytes() + descriptor.getHeaderDataBytes();
            if (skip > 0)
            {
                raf.seek(raf.getFilePointer() + skip);
            }
            return new Header(compressionLevel,
                    formatFlags,
                    blocksPerFrame,
                    finalFrameBlocks,
                    totalFrames,
                    bitsPerSample,
                    channels,
                    sampleRate);
        }

        long getTotalSamples()
        {
            if (totalFrames == 0)
            {
                return 0;
            }
            if (totalFrames == 1)
            {
                return finalFrameBlocks;
            }
            return (totalFrames - 1) * blocksPerFrame + finalFrameBlocks;
        }
    }

    private static int readIntLE(RandomAccessFile raf) throws IOException
    {
        byte[] buffer = new byte[4];
        raf.readFully(buffer);
        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    private static short readShortLE(RandomAccessFile raf) throws IOException
    {
        byte[] buffer = new byte[2];
        raf.readFully(buffer);
        ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }
}
