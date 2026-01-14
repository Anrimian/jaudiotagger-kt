package org.jaudiotagger.audio.wavpack;

import org.jaudiotagger.tag.ape.ApeTagFooter;

/**
 * Represents the resolved offsets of an APE tag within a WavPack file. We keep both the raw data span and the header/
 * footer offsets so callers can trim or rewrite precisely without guessing.
 */
final class WavPackTagLocation
{
    private final long dataStart;
    private final long dataEnd;
    private final long headerOffset;
    private final long footerOffset;
    private final ApeTagFooter descriptor;
    private final boolean atStart;

    WavPackTagLocation(long dataStart, long dataEnd, long headerOffset, long footerOffset, ApeTagFooter descriptor, boolean atStart)
    {
        this.dataStart = dataStart;
        this.dataEnd = dataEnd;
        this.headerOffset = headerOffset;
        this.footerOffset = footerOffset;
        this.descriptor = descriptor;
        this.atStart = atStart;
    }

    /** Offset to the first field/value pair (skips the header when present). */
    long getDataStart()
    {
        return dataStart;
    }

    /** Offset at which tag data ends (just before the footer if one exists). */
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

    boolean hasHeader()
    {
        return headerOffset >= 0;
    }

    boolean hasFooter()
    {
        return footerOffset >= 0;
    }

    boolean isAtStart()
    {
        return atStart;
    }

    /** Inclusive start offset when removing/replacing this tag. */
    long getRemovalStart()
    {
        return hasHeader() ? headerOffset : dataStart;
    }

    /** Exclusive end offset when removing/replacing this tag. */
    long getRemovalEnd()
    {
        if (hasFooter())
        {
            return footerOffset + ApeTagFooter.FOOTER_SIZE;
        }
        return dataEnd;
    }
}
