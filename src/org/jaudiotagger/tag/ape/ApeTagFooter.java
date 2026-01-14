package org.jaudiotagger.tag.ape;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents the 32-byte APEv2 footer/header structure.
 */
public final class ApeTagFooter
{
    public static final int FOOTER_SIZE = 32;
    public static final int CURRENT_VERSION = 2000;
    private static final byte[] IDENTIFIER = "APETAGEX".getBytes(StandardCharsets.US_ASCII);
    private static final int FLAG_HAS_HEADER = 1 << 31;
    private static final int FLAG_HAS_NO_FOOTER = 1 << 30;
    private static final int FLAG_IS_HEADER = 1 << 29;

    private final int version;
    private final int size;
    private final int itemCount;
    private final int flags;

    private ApeTagFooter(int version, int size, int itemCount, int flags)
    {
        this.version = version;
        this.size = size;
        this.itemCount = itemCount;
        this.flags = flags;
    }

    public int getVersion()
    {
        return version;
    }

    public int getSize()
    {
        return size;
    }

    public int getItemCount()
    {
        return itemCount;
    }

    public int getFlags()
    {
        return flags;
    }

    public boolean isValid()
    {
        return version >= 2000 && size >= FOOTER_SIZE && itemCount >= 0;
    }

    public boolean hasHeader()
    {
        return (flags & FLAG_HAS_HEADER) != 0;
    }

    public boolean isHeader()
    {
        return (flags & FLAG_IS_HEADER) != 0;
    }

    public boolean hasFooter()
    {
        return (flags & FLAG_HAS_NO_FOOTER) == 0;
    }

    public long calculateTagStart(long footerOffset)
    {
        return footerOffset - (size - FOOTER_SIZE);
    }

    public static ApeTagFooter read(RandomAccessFile raf) throws IOException
    {
        byte[] buffer = new byte[FOOTER_SIZE];
        raf.readFully(buffer);
        if (!Arrays.equals(Arrays.copyOf(buffer, IDENTIFIER.length), IDENTIFIER))
        {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.position(8);
        int version = bb.getInt();
        int size = bb.getInt();
        int itemCount = bb.getInt();
        int flags = bb.getInt();
        return new ApeTagFooter(version, size, itemCount, flags);
    }

    public static byte[] buildHeader(int totalSize, int fieldCount)
    {
        return build(totalSize, fieldCount, FLAG_HAS_HEADER | FLAG_IS_HEADER);
    }

    public static byte[] buildFooter(int totalSize, int fieldCount, boolean includeHeaderFlag)
    {
        int flags = includeHeaderFlag ? FLAG_HAS_HEADER : 0;
        return build(totalSize, fieldCount, flags);
    }

    private static byte[] build(int totalSize, int fieldCount, int flags)
    {
        ByteBuffer bb = ByteBuffer.allocate(FOOTER_SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(IDENTIFIER);
        bb.putInt(CURRENT_VERSION);
        bb.putInt(totalSize);
        bb.putInt(fieldCount);
        bb.putInt(flags);
        bb.putLong(0L);
        return bb.array();
    }
}
