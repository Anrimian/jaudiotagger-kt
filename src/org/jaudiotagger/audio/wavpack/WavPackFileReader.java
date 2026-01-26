package org.jaudiotagger.audio.wavpack;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.AudioFileReader2;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.Tag;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Reader for WavPack files (audio properties + tags).
 */
public class WavPackFileReader extends AudioFileReader2
{
    private final WavPackInfoReader infoReader = new WavPackInfoReader();
    private final WavPackTagReader tagReader = new WavPackTagReader();

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
