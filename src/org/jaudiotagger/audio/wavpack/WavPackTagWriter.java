package org.jaudiotagger.audio.wavpack;

import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.ape.ApeTag;
import org.jaudiotagger.tag.ape.ApeTagField;
import org.jaudiotagger.tag.ape.ApeTagFooter;
import org.jaudiotagger.tag.ape.ApeTagTextField;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.TagTextField;

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
 * Writes APEv2 tags to WavPack files.
 */
final class WavPackTagWriter
{
    void write(Tag tag, Path path) throws CannotWriteException
    {
        if (!(tag instanceof ApeTag apeTag))
        {
            throw new CannotWriteException("WavPack files require an APEv2 tag instance");
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
            // Resolve trailing metadata once so we can truncate at the right place and re-append in the original order.
            WavPackTagHelper.ScanResult scan = ensureNoLeadingTagIsRemoved(raf);
            long insertionOffset = scan.metadataInsertionOffset();
            raf.setLength(insertionOffset);
            raf.seek(insertionOffset);
            raf.write(payload);
            writeTrailingMetadata(raf, scan);
        }
        catch (IOException e)
        {
            throw new CannotWriteException("Unable to write WavPack tag: " + e.getMessage(), e);
        }
    }

    void delete(Tag tag, Path path) throws CannotWriteException
    {
        File file = path.toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw"))
        {
            // A delete is just a truncated write: cut the APE region then re-attach Lyrics3/ID3 bytes if they existed.
            WavPackTagHelper.ScanResult scan = WavPackTagHelper.scan(raf);
            WavPackTagLocation location = scan.getLocation();
            if (location == null)
            {
                return;
            }
            if (location.isAtStart())
            {
                WavPackTagHelper.removeRange(raf, location.getRemovalStart(), location.getRemovalEnd());
                return;
            }
            long writeOffset = location.getRemovalStart();
            raf.setLength(writeOffset);
            raf.seek(writeOffset);
            writeTrailingMetadata(raf, scan);
        }
        catch (IOException e)
        {
            throw new CannotWriteException("Unable to delete WavPack tag: " + e.getMessage(), e);
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
        // Write an explicit header so start-of-file tags can be recreated verbatim when needed.
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

    private WavPackTagHelper.ScanResult ensureNoLeadingTagIsRemoved(RandomAccessFile raf) throws IOException
    {
        // WavPack stores an optional tag before the audio block; drop it by shifting bytes so subsequent rewrites
        // only target the canonical trailing location.
        WavPackTagHelper.ScanResult scan = WavPackTagHelper.scan(raf);
        WavPackTagLocation location = scan.getLocation();
        if (location != null && location.isAtStart())
        {
            // Preserve audio by physically removing the bytes in-place, then rescan so subsequent logic sees a clean slate.
            WavPackTagHelper.removeRange(raf, location.getRemovalStart(), location.getRemovalEnd());
            return WavPackTagHelper.scan(raf);
        }
        return scan;
    }

    private void writeTrailingMetadata(RandomAccessFile raf, WavPackTagHelper.ScanResult scan) throws IOException
    {
        // Append preserved padding first, followed by Lyrics3 then ID3. This keeps offsets stable for other tools.
        byte[] padding = scan.getPadding();
        if (padding.length > 0)
        {
            raf.write(padding);
        }
        WavPackTagHelper.Lyrics3Info lyrics = scan.getLyrics3();
        if (lyrics != null)
        {
            raf.write(lyrics.data);
        }
        WavPackTagHelper.Id3Info id3 = scan.getId3();
        if (id3 != null)
        {
            raf.write(id3.data);
        }
    }
}
