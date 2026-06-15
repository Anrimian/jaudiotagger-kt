package org.jaudiotagger.issues;

import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagTextField;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;

import java.io.File;

/**
 * Hide the differences between the two genre fields used by the mp4 format
 */
public class Issue418Test extends AbstractTestCase
{
    public void testGetCustomGenreField() throws Exception
    {
        //Starts with a single custom genre called 'Genre'
        File testFile = AbstractTestCase.copyAudioToTmp("test.m4a");
        AudioFile f = AudioFileIO.read(testFile);
        Tag tag = f.getTag();
        Mp4Tag mp4tag = (Mp4Tag)f.getTag();

        //only have custom genre
        assertEquals("Genre", mp4tag.getFirst(Mp4FieldKey.GENRE_CUSTOM));
        assertEquals("", mp4tag.getFirst(Mp4FieldKey.GENRE));

        //Generic Interface checks both values so return custom genre as regular genre
        assertEquals("Genre", tag.getFirst(FieldKey.GENRE));
        assertEquals(1, tag.getFields(FieldKey.GENRE).size());

        //Create a standard genre field
        mp4tag.setField(mp4tag.createField(Mp4FieldKey.GENRE,"Rock"));

        //Get standard
        assertEquals("Rock", mp4tag.getFirst(Mp4FieldKey.GENRE));

        //Get Custom
        assertEquals("Genre", mp4tag.getFirst(Mp4FieldKey.GENRE_CUSTOM));

        //Because we now have two genre fields stored in the file returns the standard genre field by default
        //because this is checked first
        assertEquals("Rock", tag.getFirst(FieldKey.GENRE));

        assertEquals(1, mp4tag.getFields(Mp4FieldKey.GENRE.getFieldName()).size());
        assertEquals(1, mp4tag.getFields(Mp4FieldKey.GENRE_CUSTOM.getFieldName()).size());
        assertEquals(2, tag.getFields(FieldKey.GENRE).size());

        assertEquals("Rock", ((TagTextField)tag.getFields(FieldKey.GENRE).get(0)).getContent());
        assertEquals("Genre", ((TagTextField)tag.getFields(FieldKey.GENRE).get(1)).getContent());

        assertEquals(2, tag.getAll(FieldKey.GENRE).size());
        assertEquals(2,tag.getFields(FieldKey.GENRE).size());


        f.commit();

        f = AudioFileIO.read(testFile);
        tag = f.getTag();
        mp4tag = (Mp4Tag)f.getTag();
        assertEquals("Rock", mp4tag.getFirst(Mp4FieldKey.GENRE));
        assertEquals("Genre", mp4tag.getFirst(Mp4FieldKey.GENRE_CUSTOM));
        //Because we still have two genre fields stored in the file returns the standard genre field by default
        assertEquals("Rock", tag.getFirst(FieldKey.GENRE));
        assertEquals(2, tag.getFields(FieldKey.GENRE).size());

        mp4tag.addField(FieldKey.GENRE,"Pop");
        assertEquals("Rock", mp4tag.getFirst(Mp4FieldKey.GENRE));
        assertEquals("Genre", mp4tag.getFirst(Mp4FieldKey.GENRE_CUSTOM));
        assertEquals("Rock", tag.getFirst(FieldKey.GENRE));
        assertEquals("Rock", ((TagTextField)tag.getFields(FieldKey.GENRE).get(0)).getContent());
        assertEquals("Pop", ((TagTextField)tag.getFields(FieldKey.GENRE).get(1)).getContent());
        assertEquals("Genre", ((TagTextField)tag.getFields(FieldKey.GENRE).get(2)).getContent());
        assertEquals(3, tag.getFields(FieldKey.GENRE).size());

        tag.setField(FieldKey.GENRE,"Jazz");
        assertEquals("Jazz", tag.getFirst(FieldKey.GENRE));
        assertEquals("Jazz", ((TagTextField)tag.getFields(FieldKey.GENRE).get(0)).getContent());
        assertEquals("Pop", ((TagTextField)tag.getFields(FieldKey.GENRE).get(1)).getContent());
        assertEquals("Genre", ((TagTextField)tag.getFields(FieldKey.GENRE).get(2)).getContent());
        assertEquals(3, tag.getFields(FieldKey.GENRE).size());

        tag.setField(FieldKey.GENRE,"NewCustomGenre");
        assertEquals("Jazz", tag.getFirst(FieldKey.GENRE));
        assertEquals("Jazz", ((TagTextField)tag.getFields(FieldKey.GENRE).get(0)).getContent());
        assertEquals("Pop", ((TagTextField)tag.getFields(FieldKey.GENRE).get(1)).getContent());
        assertEquals("NewCustomGenre", ((TagTextField)tag.getFields(FieldKey.GENRE).get(2)).getContent());
        assertEquals(3, tag.getFields(FieldKey.GENRE).size());
    }
}
