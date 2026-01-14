package org.jaudiotagger.tag.ape;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Represents a binary APEv2 field.
 */
public class ApeTagBinaryField extends ApeTagField
{
    private byte[] data;

    public ApeTagBinaryField(String id, byte[] data, boolean common)
    {
        super(id, FLAG_TYPE_BINARY, common);
        this.data = data == null ? new byte[0] : data;
    }

    @Override
    public byte[] getRawContent() throws UnsupportedEncodingException
    {
        return data;
    }

    @Override
    public boolean isBinary()
    {
        return true;
    }

    public byte[] getBinaryData()
    {
        return data;
    }

    public void setBinaryData(byte[] data)
    {
        this.data = data == null ? new byte[0] : data;
    }

    @Override
    protected void copyValueFrom(ApeTagField field)
    {
        if (field instanceof ApeTagBinaryField binaryField)
        {
            this.data = Arrays.copyOf(binaryField.data, binaryField.data.length);
        }
    }

    @Override
    public String toString()
    {
        return getId() + " (" + data.length + " bytes)";
    }
}
