package org.jaudiotagger.audio.wavpack;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.generic.AudioFileWriter2;
import org.jaudiotagger.tag.Tag;

import java.nio.file.Path;

/**
 * Writer for WavPack tags.
 */
public class WavPackFileWriter extends AudioFileWriter2
{
    private final WavPackTagWriter writer = new WavPackTagWriter();

    @Override
    protected void writeTag(Tag tag, Path file) throws CannotWriteException
    {
        writer.write(tag, file);
    }

    @Override
    protected void deleteTag(Tag tag, Path file) throws CannotReadException, CannotWriteException
    {
        writer.delete(tag, file);
    }
}
