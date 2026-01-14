package org.jaudiotagger.tag.ape;

import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Specialized binary field that stores cover art.
 */
public class ApeTagCoverField extends ApeTagBinaryField
{
    private static final byte NULL_TERMINATOR = 0;

    public ApeTagCoverField(String id, byte[] raw)
    {
        super(id, raw, false);
    }

    public ApeTagCoverField(Artwork artwork) throws FieldDataInvalidException
    {
        super(ApeFieldKey.COVER_ART_FRONT.getFieldName(), buildValue(artwork), false);
    }

    private static byte[] buildValue(Artwork artwork) throws FieldDataInvalidException
    {
        if (artwork == null)
        {
            throw new FieldDataInvalidException(ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.getMsg());
        }
        byte[] imageData = artwork.getBinaryData();
        if (imageData == null || imageData.length == 0)
        {
            throw new FieldDataInvalidException(ErrorMessage.ARTWORK_CANNOT_BE_CREATED_WITH_THIS_METHOD.getMsg());
        }
        String mime = artwork.getMimeType();
        if (mime == null || mime.isEmpty())
        {
            mime = "application/octet-stream";
        }
        byte[] descriptor = mime.getBytes(StandardCharsets.UTF_8);
        byte[] buffer = new byte[descriptor.length + 1 + imageData.length];
        System.arraycopy(descriptor, 0, buffer, 0, descriptor.length);
        buffer[descriptor.length] = NULL_TERMINATOR;
        System.arraycopy(imageData, 0, buffer, descriptor.length + 1, imageData.length);
        return buffer;
    }

    public Artwork toArtwork()
    {
        byte[] raw = getBinaryData();
        int split = findSeparator(raw);
        String mime = "application/octet-stream";
        byte[] data = raw;
        if (split >= 0)
        {
            mime = new String(Arrays.copyOf(raw, split), StandardCharsets.UTF_8);
            data = Arrays.copyOfRange(raw, split + 1, raw.length);
        }
        Artwork artwork = ArtworkFactory.getNew();
        artwork.setMimeType(mime);
        artwork.setBinaryData(data);
        artwork.setDescription("Cover Art (Front)");
        return artwork;
    }

    private int findSeparator(byte[] raw)
    {
        for (int i = 0; i < raw.length; i++)
        {
            if (raw[i] == NULL_TERMINATOR)
            {
                return i;
            }
        }
        return -1;
    }

}
