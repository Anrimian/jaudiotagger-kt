package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp4.Mp4FileReader;
import org.jaudiotagger.tag.mp4.Mp4Tag;

import java.io.File;

/**
 * Test trying to read non existent mp3 file
 */
public class Issue665Test extends AbstractTestCase
{
    public void testReadFileDiskAtomStoringTextInsteadOfNumber()
    {
        File orig = new File("testdata", "test665.m4a");
        if (!orig.isFile())
        {
            System.err.println("Unable to test file - not available");
            return;
        }

        Exception exceptionCaught = null;
        try
        {
            File testFile = AbstractTestCase.copyAudioToTmp("test665.m4a");
            AudioFile f = AudioFileIO.read(testFile);
            System.out.println(f.getAudioHeader());


            assertEquals("160", f.getAudioHeader().getBitRate());
            assertEquals("Aac", f.getAudioHeader().getEncodingType());
            assertEquals("2", f.getAudioHeader().getChannels());
            assertEquals("44100", f.getAudioHeader().getSampleRate());
            assertEquals(233, f.getAudioHeader().getTrackLength());
            assertEquals(233.336, f.getAudioHeader().getPreciseTrackLength());
            assertTrue(f.getTag() instanceof Mp4Tag);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            exceptionCaught = e;
        }
        assertNull(exceptionCaught);
    }

}