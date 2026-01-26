package org.jaudiotagger.audio.monkey;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.AudioFileReader2;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Reader for Monkey's Audio (.ape) files (audio properties + tags).
 */
public final class MonkeyFileReader extends AudioFileReader2
{
    private final MonkeyInfoReader infoReader = new MonkeyInfoReader();
    private final MonkeyTagReader tagReader = new MonkeyTagReader();

    @Override
    protected GenericAudioHeader getEncodingInfo(Path path) throws CannotReadException, IOException
    {
        return infoReader.read(path);
    }

    @Override
    protected Tag getTag(Path path) throws CannotReadException, IOException
    {
        return tagReader.read(path);
    }
}
