package org.jaudiotagger.audio.wavpack;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.GenericAudioHeader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Extracts basic audio information from the first WavPack block.
 */
final class WavPackInfoReader
{
    private static final String HEADER_ID = "wvpk";
    private static final int HEADER_SIZE = 32;
    private static final int META_ID_WV_BITSTREAM = 0x0A;
    private static final int META_ID_CHANNEL_INFO = 0x1D;
    private static final int META_ID_SAMPLE_RATE = 0x27;
    private static final int[] SAMPLE_RATES = new int[]
        {
            6000, 8000, 9600, 11025,
            12000, 16000, 22050, 24000,
            32000, 44100, 48000, 64000,
            88200, 96000, 192000, 0
        };

    GenericAudioHeader read(Path path) throws CannotReadException, IOException
    {
        File file = path.toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
        {
            BlockHeader header = locateAudioBlock(raf);
            if (header == null)
            {
                throw new CannotReadException("Unable to locate WavPack audio block");
            }
            GenericAudioHeader audioHeader = new GenericAudioHeader();
            populate(header, raf, audioHeader);
            return audioHeader;
        }
    }

    private void populate(BlockHeader header, RandomAccessFile raf, GenericAudioHeader target) throws IOException
    {
        // Populate the shared GenericAudioHeader using details extracted from the first audio block.
        MetadataInfo metadata = readMetadata(header, raf);
        target.setFormat("WavPack");
        boolean hybrid = header.isHybridMode();
        target.setEncodingType(hybrid ? "WavPack Hybrid" : "WavPack Lossless");
        target.setLossless(!hybrid);
        target.setVariableBitRate(true);
        int channelCount = metadata.getChannelCount() > 0 ? metadata.getChannelCount() : (header.isMono() ? 1 : 2);
        target.setChannelNumber(channelCount);
        int bitsPerSample = header.getBitsPerSample();
        int sampleRate = metadata.getSampleRate() > 0 ? metadata.getSampleRate() : header.getSampleRateFromFlags();
        if (header.isDsd())
        {
            // WavPack signals DSD via its highest flag bit; DSD64 stores a single bit per sample but expects the
            // reported sample rate multiplied by four so downstream players display the familiar 2.8 MHz value.
            sampleRate *= 4;
            bitsPerSample = 1;
        }
        target.setBitsPerSample(bitsPerSample);
        if (sampleRate > 0)
        {
            target.setSamplingRate(sampleRate);
        }
        long totalSamples = calculateTotalSamples(raf, header);
        if (sampleRate > 0 && totalSamples > 0)
        {
            double length = (double) totalSamples / (double) sampleRate;
            target.setPreciseLength(length);
            target.setNoOfSamples(totalSamples);
        }
    }

    private long calculateTotalSamples(RandomAccessFile raf, BlockHeader first) throws IOException
    {
        if (first.getTotalSamples() > 0 && first.getBlockIndex() == 0)
        {
            // Fast path: the first block reports the total sample count for the whole file.
            return first.getTotalSamples();
        }

        // Otherwise walk subsequent blocks accumulating their block_samples counts. This matches Mutagen's fallback
        // and allows us to report a useful duration even when the total_samples field is unset.
        long samples = first.getBlockSamples();
        long pointer = first.getBlockStart() + first.getBlockSize();
        raf.seek(pointer);

        while (pointer + HEADER_SIZE <= raf.length())
        {
            long start = raf.getFilePointer();
            BlockHeader header = BlockHeader.read(raf, start);
            if (header == null)
            {
                break;
            }
            if (header.getBlockSamples() > 0)
            {
                samples += header.getBlockSamples();
            }
            pointer = header.getBlockStart() + header.getBlockSize();
            raf.seek(pointer);
        }
        return samples;
    }

    private MetadataInfo readMetadata(BlockHeader header, RandomAccessFile raf) throws IOException
    {
        MetadataInfo info = new MetadataInfo();
        long dataStart = header.getBlockStart() + HEADER_SIZE;
        long metaEnd = header.getBlockStart() + header.getBlockSize();
        raf.seek(dataStart);
        while (raf.getFilePointer() < metaEnd)
        {
            int id = raf.readUnsignedByte();
            boolean large = (id & 0x80) != 0;
            boolean odd = (id & 0x40) != 0;
            int metaId = id & 0x3F;
            int words;
            if (large)
            {
                int b0 = raf.readUnsignedByte();
                int b1 = raf.readUnsignedByte();
                int b2 = raf.readUnsignedByte();
                words = b0 | (b1 << 8) | (b2 << 16);
            }
            else
            {
                words = raf.readUnsignedByte();
            }
            int dataBytes = words * 2 + (odd ? 1 : 0);
            int storedBytes = (dataBytes & 1) == 0 ? dataBytes : dataBytes + 1;
            if (metaId == META_ID_WV_BITSTREAM)
            {
                break;
            }

            if ((metaId == META_ID_SAMPLE_RATE && dataBytes >= 3) || (metaId == META_ID_CHANNEL_INFO && dataBytes >= 2))
            {
                byte[] data = new byte[dataBytes];
                raf.readFully(data);
                int padding = storedBytes - dataBytes;
                if (padding > 0)
                {
                    raf.skipBytes(padding);
                }
                if (metaId == META_ID_SAMPLE_RATE)
                {
                    int sampleRate = ((data[2] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[0] & 0xFF);
                    info.setSampleRate(sampleRate);
                }
                else if (metaId == META_ID_CHANNEL_INFO)
                {
                    int channels = parseChannelCount(data);
                    info.setChannelCount(channels);
                }
                if (info.isComplete())
                {
                    break;
                }
            }
            else
            {
                raf.seek(raf.getFilePointer() + storedBytes);
            }
        }
        return info;
    }

    private int parseChannelCount(byte[] data)
    {
        if (data.length == 0)
        {
            return 0;
        }
        int candidate = 0;
        if (data.length >= 2)
        {
            candidate = ((data[1] & 0xFF) << 8) | (data[0] & 0xFF);
            if (candidate <= 0 || candidate > 32)
            {
                candidate = 0;
            }
        }
        if (candidate == 0)
        {
            candidate = data[0] & 0xFF;
        }
        if (candidate <= 0 || candidate > 32)
        {
            return 0;
        }
        return candidate;
    }

    private BlockHeader locateAudioBlock(RandomAccessFile raf) throws IOException
    {
        while (raf.getFilePointer() + HEADER_SIZE <= raf.length())
        {
            long start = raf.getFilePointer();
            BlockHeader header = BlockHeader.read(raf, start);
            if (header == null)
            {
                break;
            }
            if (header.getBlockSamples() > 0)
            {
                return header;
            }
            raf.seek(start + header.getBlockSize());
        }
        return null;
    }

    private static final class BlockHeader
    {
        private final long blockStart;
        private final long blockSize;
        private final int flags;
        private final int blockSamples;
        private final long totalSamples;
        private final long blockIndex;

        private BlockHeader(long blockStart, long blockSize, int flags, int blockSamples, long totalSamples, long blockIndex)
        {
            this.blockStart = blockStart;
            this.blockSize = blockSize;
            this.flags = flags;
            this.blockSamples = blockSamples;
            this.totalSamples = totalSamples;
            this.blockIndex = blockIndex;
        }

        static BlockHeader read(RandomAccessFile raf, long start) throws IOException
        {
            byte[] buffer = new byte[HEADER_SIZE];
            raf.readFully(buffer);
            if (!HEADER_ID.equals(new String(buffer, 0, 4, StandardCharsets.US_ASCII)))
            {
                return null;
            }
            ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
            bb.position(4);
            long ckSize = Integer.toUnsignedLong(bb.getInt());
            int version = bb.getShort() & 0xFFFF;
            int blockIndexMsb = bb.get() & 0xFF;
            int totalSamplesMsb = bb.get() & 0xFF;
            long totalSamplesLow = Integer.toUnsignedLong(bb.getInt());
            long blockIndexLow = Integer.toUnsignedLong(bb.getInt());
            int blockSamples = bb.getInt();
            int flags = bb.getInt();
            bb.getInt(); // crc
            long blockSize = ckSize + 8;
            if (version < 0x402 || blockSize < HEADER_SIZE)
            {
                return null;
            }
            // total_samples and block_index are 40-bit counters (upper 8 bits stored separately). Reconstruct them so
            // callers do not need to reason about the split representation.
            long totalSamples = combineCounter(totalSamplesMsb, totalSamplesLow);
            long blockIndex = combineCounter(blockIndexMsb, blockIndexLow);
            return new BlockHeader(start, blockSize, flags, blockSamples, totalSamples, blockIndex);
        }

        private static long combineCounter(int msb, long low)
        {
            if (low == 0xFFFFFFFFL)
            {
                return -1;
            }
            return low | ((long) msb << 32);
        }

        boolean isHybridMode()
        {
            return (flags & 0x8) != 0;
        }

        boolean isMono()
        {
            return (flags & 0x4) != 0;
        }

        boolean isDsd()
        {
            return (flags & (1 << 31)) != 0;
        }

        int getBitsPerSample()
        {
            int stored = flags & 0x3;
            return (stored + 1) * 8;
        }

        long getTotalSamples()
        {
            return totalSamples;
        }

        long getBlockIndex()
        {
            return blockIndex;
        }

        int getBlockSamples()
        {
            return blockSamples;
        }

        long getBlockStart()
        {
            return blockStart;
        }

        long getBlockSize()
        {
            return blockSize;
        }

        int getSampleRateFromFlags()
        {
            int index = (flags >>> 23) & 0xF;
            if (index >= 0 && index < SAMPLE_RATES.length)
            {
                return SAMPLE_RATES[index];
            }
            return 0;
        }
    }

    private static final class MetadataInfo
    {
        private int sampleRate;
        private int channelCount;

        int getSampleRate()
        {
            return sampleRate;
        }

        void setSampleRate(int value)
        {
            if (value > 0)
            {
                this.sampleRate = value;
            }
        }

        int getChannelCount()
        {
            return channelCount;
        }

        void setChannelCount(int value)
        {
            if (value > 0)
            {
                this.channelCount = value;
            }
        }

        boolean isComplete()
        {
            return sampleRate > 0 && channelCount > 0;
        }
    }
}
