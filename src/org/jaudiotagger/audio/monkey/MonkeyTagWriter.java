package org.jaudiotagger.audio.monkey;

import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.TagTextField;
import org.jaudiotagger.tag.ape.ApeTag;
import org.jaudiotagger.tag.ape.ApeTagField;
import org.jaudiotagger.tag.ape.ApeTagFooter;
import org.jaudiotagger.tag.ape.ApeTagTextField;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Serialises Monkey's Audio APEv2 tags while preserving Lyrics3/ID3 blocks located past the audio frames.
 */
final class MonkeyTagWriter
{
    void write(Tag tag, Path path) throws CannotWriteException
    {
        if (!(tag instanceof ApeTag apeTag))
        {
            throw new CannotWriteException("Monkey's Audio files require an APEv2 tag instance");
        }
        if (apeTag.isEmpty())
        {
            delete(tag, path);
            return;
        }
        byte[] payload = buildPayload(apeTag);
        if (payload.length == 0)
        {
            delete(tag, path);
            return;
        }
        File file = path.toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw"))
        {
            MonkeyTagHelper.ScanResult scan = ensureNoLeadingTagIsRemoved(raf);
            long insertionOffset = scan.metadataInsertionOffset();
            raf.setLength(insertionOffset);
            raf.seek(insertionOffset);
            raf.write(payload);
            writeTrailingMetadata(raf, scan);
        }
        catch (IOException e)
        {
            throw new CannotWriteException("Unable to write Monkey's Audio tag: " + e.getMessage(), e);
        }
    }

    void delete(Tag tag, Path path) throws CannotWriteException
    {
        File file = path.toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw"))
        {
            MonkeyTagHelper.ScanResult scan = MonkeyTagHelper.scan(raf);
            MonkeyTagLocation location = scan.getLocation();
            if (location == null)
            {
                return;
            }
            if (location.isAtStart())
            {
                MonkeyTagHelper.removeRange(raf, location.getRemovalStart(), location.getRemovalEnd());
                return;
            }
            long writeOffset = location.getRemovalStart();
            raf.setLength(writeOffset);
            raf.seek(writeOffset);
            writeTrailingMetadata(raf, scan);
        }
        catch (IOException e)
        {
            throw new CannotWriteException("Unable to delete Monkey's Audio tag: " + e.getMessage(), e);
        }
    }

    private byte[] buildPayload(ApeTag tag) throws CannotWriteException
    {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int fieldCount = 0;
        Iterator<TagField> iterator = tag.getFields();
        while (iterator.hasNext())
        {
            TagField field = iterator.next();
            ApeTagField apeField = toApeField(field);
            if (apeField == null)
            {
                continue;
            }
            byte[] value;
            try
            {
                value = apeField.getRawContent();
            }
            catch (Exception ex)
            {
                throw new CannotWriteException("Unable to encode field " + apeField.getId(), ex);
            }
            byte[] id = apeField.getId().getBytes(StandardCharsets.UTF_8);
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(value.length);
            header.putInt(apeField.getFlags());
            body.write(header.array(), 0, header.array().length);
            body.write(id, 0, id.length);
            body.write(0);
            body.write(value, 0, value.length);
            fieldCount++;
        }
        if (fieldCount == 0)
        {
            return new byte[0];
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int totalSize = body.size() + ApeTagFooter.FOOTER_SIZE;
        output.write(ApeTagFooter.buildHeader(totalSize, fieldCount), 0, ApeTagFooter.FOOTER_SIZE);
        output.write(body.toByteArray(), 0, body.size());
        output.write(ApeTagFooter.buildFooter(totalSize, fieldCount, true), 0, ApeTagFooter.FOOTER_SIZE);
        return output.toByteArray();
    }

    private ApeTagField toApeField(TagField field)
    {
        if (field instanceof ApeTagField apeField)
        {
            return apeField;
        }
        if (field instanceof TagTextField textField)
        {
            return new ApeTagTextField(field.getId(), textField.getContent(), false);
        }
        return null;
    }

    private MonkeyTagHelper.ScanResult ensureNoLeadingTagIsRemoved(RandomAccessFile raf) throws IOException
    {
        MonkeyTagHelper.ScanResult scan = MonkeyTagHelper.scan(raf);
        MonkeyTagLocation location = scan.getLocation();
        if (location != null && location.isAtStart())
        {
            MonkeyTagHelper.removeRange(raf, location.getRemovalStart(), location.getRemovalEnd());
            return MonkeyTagHelper.scan(raf);
        }
        return scan;
    }

    private void writeTrailingMetadata(RandomAccessFile raf, MonkeyTagHelper.ScanResult scan) throws IOException
    {
        byte[] padding = scan.getPadding();
        if (padding.length > 0)
        {
            raf.write(padding);
        }
        MonkeyTagHelper.Lyrics3Info lyrics = scan.getLyrics();
        if (lyrics != null)
        {
            raf.write(lyrics.data);
        }
        MonkeyTagHelper.Id3Info id3 = scan.getId3();
        if (id3 != null)
        {
            raf.write(id3.data);
        }
    }
}
