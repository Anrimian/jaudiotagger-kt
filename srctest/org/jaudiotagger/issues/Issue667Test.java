package org.jaudiotagger.issues;

import junit.framework.TestCase;
import org.jaudiotagger.AbstractTestCase;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.wav.WavOptions;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.aiff.AiffTag;
import org.jaudiotagger.tag.ape.ApeTag;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.jaudiotagger.tag.asf.AsfTag;
import org.jaudiotagger.tag.wav.WavTag;

import java.io.File;

public class Issue667Test extends TestCase {
    
    public void testID3v24() throws Exception {
        Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", tag.getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testID3v23() throws Exception {
        Tag tag = new ID3v23Tag();
        tag.setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", tag.getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testVorbisComment() throws Exception {
        Tag tag = new VorbisCommentTag();
        tag.setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", tag.getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testVorbisCommentFile() throws Exception {
        File testFile = AbstractTestCase.copyAudioToTmp("test.ogg", new File("AlbumComposerTest.ogg"));
        AudioFile f = AudioFileIO.read(testFile);
        f.getTag().setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", f.getTag().getFirst(FieldKey.ALBUM_COMPOSER));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals("Test Album Composer", f.getTag().getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testFlac() throws Exception {
        Tag tag = new FlacTag();
        tag.setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", tag.getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testFlacFile() throws Exception {
        File testFile = AbstractTestCase.copyAudioToTmp("test2.flac", new File("AlbumComposerTest.flac"));
        AudioFile f = AudioFileIO.read(testFile);
        f.getTag().setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", f.getTag().getFirst(FieldKey.ALBUM_COMPOSER));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals("Test Album Composer", f.getTag().getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testMp4() throws Exception {
        Tag tag = new Mp4Tag();
        tag.setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", tag.getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testMp4File() throws Exception {
        File testFile = AbstractTestCase.copyAudioToTmp("test.m4a", new File("AlbumComposerTest.m4a"));
        AudioFile f = AudioFileIO.read(testFile);
        f.getTag().setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", f.getTag().getFirst(FieldKey.ALBUM_COMPOSER));
        f.commit();
        f = AudioFileIO.read(testFile);
        assertEquals("Test Album Composer", f.getTag().getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testAsf() throws Exception {
        Tag tag = new AsfTag();
        tag.setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", tag.getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testAif() throws Exception {
        Tag tag = new AiffTag();
        ((AiffTag)tag).setID3Tag(AiffTag.createDefaultID3Tag());
        tag.setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", tag.getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testWav() throws Exception {
        Tag tag = new WavTag(WavOptions.READ_ID3_UNLESS_ONLY_INFO);
        ((WavTag)tag).setID3Tag(WavTag.createDefaultID3Tag());
        tag.setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", tag.getFirst(FieldKey.ALBUM_COMPOSER));
    }

    public void testApe() throws Exception {
        Tag tag = new ApeTag();
        tag.setField(FieldKey.ALBUM_COMPOSER, "Test Album Composer");
        assertEquals("Test Album Composer", tag.getFirst(FieldKey.ALBUM_COMPOSER));
    }
}
