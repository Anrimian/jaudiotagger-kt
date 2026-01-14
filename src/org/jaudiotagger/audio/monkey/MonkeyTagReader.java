package org.jaudiotagger.audio.monkey;

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
 * Reads trailing APEv2 tags embedded in Monkey's Audio files.
 */
final class MonkeyTagReader
{
    Tag read(Path path) throws CannotReadException, IOException
    {
        File file = path.toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
        {
            MonkeyTagHelper.ScanResult scan = MonkeyTagHelper.scan(raf);
            MonkeyTagLocation location = scan.getLocation();
            if (location == null)
            {
                return ApeTag.createDefaultTag();
            }
            return loadTag(raf, location);
        }
    }

    private ApeTag loadTag(RandomAccessFile raf, MonkeyTagLocation location) throws IOException
    {
        ApeTag tag = new ApeTag();
        raf.seek(location.getDataStart());
        long contentEnd = location.getDataEnd();
        while (raf.getFilePointer() < contentEnd)
        {
            long bytesRemaining = contentEnd - raf.getFilePointer();
            if (bytesRemaining < 8)
            {
                break;
            }
            int valueLength = MonkeyTagHelper.readLittleEndianInt(raf);
            int flags = MonkeyTagHelper.readLittleEndianInt(raf);
            String identifier = MonkeyTagHelper.readCString(raf);
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
