package org.jaudiotagger.audio.monkey;

import org.jaudiotagger.tag.ape.ApeTagFooter;

/**
 * Stores the resolved byte offsets for a Monkey's Audio APEv2 tag so helpers can rewrite without re-scanning.
 */
final class MonkeyTagLocation
{
    private final long dataStart;
    private final long dataEnd;
    private final long headerOffset;
    private final long footerOffset;
    private final ApeTagFooter descriptor;
    private final boolean atStart;

    MonkeyTagLocation(long dataStart, long dataEnd, long headerOffset, long footerOffset, ApeTagFooter descriptor, boolean atStart)
    {
        this.dataStart = dataStart;
        this.dataEnd = dataEnd;
        this.headerOffset = headerOffset;
        this.footerOffset = footerOffset;
        this.descriptor = descriptor;
        this.atStart = atStart;
    }

    long getDataStart()
    {
        return dataStart;
    }

    long getDataEnd()
    {
        return dataEnd;
    }

    long getHeaderOffset()
    {
        return headerOffset;
    }

    long getFooterOffset()
    {
        return footerOffset;
    }

    ApeTagFooter getDescriptor()
    {
        return descriptor;
    }

    boolean isAtStart()
    {
        return atStart;
    }

    boolean hasHeader()
    {
        return headerOffset >= 0;
    }

    boolean hasFooter()
    {
        return footerOffset >= 0;
    }

    long getRemovalStart()
    {
        return hasHeader() ? headerOffset : dataStart;
    }

    long getRemovalEnd()
    {
        if (hasFooter())
        {
            return footerOffset + ApeTagFooter.FOOTER_SIZE;
        }
        return dataEnd;
    }
}
