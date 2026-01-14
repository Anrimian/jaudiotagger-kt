package org.jaudiotagger.audio.monkey;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.generic.AudioFileWriter2;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writer for Monkey's Audio tags.
 */
public final class MonkeyFileWriter extends AudioFileWriter2
{
    private final MonkeyTagWriter writer = new MonkeyTagWriter();

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
