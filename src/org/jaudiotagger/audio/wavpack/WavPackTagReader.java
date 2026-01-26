package org.jaudiotagger.audio.wavpack;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.ape.ApeTag;
import org.jaudiotagger.tag.ape.ApeTagField;
import org.jaudiotagger.tag.ape.ApeTagUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

/**
 * Reads trailing APEv2 tags from WavPack files.
 */
final class WavPackTagReader
{
    Tag read(Path path) throws CannotReadException, IOException
    {
        File file = path.toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
        {
            // Scan once so we can honour padding/Lyrics3/ID3 positioning while extracting the tag.
            WavPackTagHelper.ScanResult scan = WavPackTagHelper.scan(raf);
            WavPackTagLocation location = scan.getLocation();
            if (location == null)
            {
                return ApeTag.createDefaultTag();
            }
            return loadTag(raf, location);
        }
    }

    private ApeTag loadTag(RandomAccessFile raf, WavPackTagLocation location) throws IOException
    {
        ApeTag tag = new ApeTag();
        // The helper already told us exactly where the tag data lives (header/footer trimmed as needed).
        raf.seek(location.getDataStart());
        long contentEnd = location.getDataEnd();
        while (raf.getFilePointer() < contentEnd)
        {
            long bytesRemaining = contentEnd - raf.getFilePointer();
            if (bytesRemaining < 8)
            {
                break;
            }
            int valueLength = WavPackTagHelper.readLittleEndianInt(raf);
            int flags = WavPackTagHelper.readLittleEndianInt(raf);
            String identifier = WavPackTagHelper.readCString(raf);
            if (valueLength < 0 || valueLength > contentEnd - raf.getFilePointer())
            {
                break;
            }
            byte[] value = new byte[valueLength];
            raf.readFully(value);
            ApeTagField field = ApeTagUtil.buildField(identifier, value, flags);
            tag.addField(field);
        }
        return tag;
    }
}
