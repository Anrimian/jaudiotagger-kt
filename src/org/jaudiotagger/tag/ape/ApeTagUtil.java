package org.jaudiotagger.tag.ape;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Utility helpers for dealing with raw APEv2 items.
 */
public final class ApeTagUtil
{
    private ApeTagUtil()
    {
    }

    public static ApeTagField buildField(String id, byte[] value, int flags)
    {
        ApeFieldKey fieldKey = ApeFieldKey.fromFieldName(id);
        boolean common = fieldKey != null && fieldKey.isCommon();
        boolean binary = (flags & ApeTagField.FLAG_TYPE_BINARY) == ApeTagField.FLAG_TYPE_BINARY;
        if (isCoverArt(id))
        {
            return new ApeTagCoverField(id, value);
        }
        if (binary)
        {
            return new ApeTagBinaryField(id, value, common);
        }
        String content = new String(value, StandardCharsets.UTF_8);
        if (fieldKey != null)
        {
            return new ApeTagTextField(fieldKey.getFieldName(), content, fieldKey.isCommon());
        }
        return new ApeTagTextField(id, content, false);
    }

    private static boolean isCoverArt(String id)
    {
        if (id == null)
        {
            return false;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        return normalized.equals(ApeFieldKey.COVER_ART_FRONT.getFieldName().toLowerCase(Locale.ROOT))
                || normalized.equals(ApeFieldKey.COVER_ART_BACK.getFieldName().toLowerCase(Locale.ROOT));
    }
}
