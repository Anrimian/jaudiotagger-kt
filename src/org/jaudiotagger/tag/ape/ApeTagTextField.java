package org.jaudiotagger.tag.ape;

import org.jaudiotagger.tag.TagTextField;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Represents a textual APEv2 field.
 */
public class ApeTagTextField extends ApeTagField implements TagTextField
{
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private String value;
    private Charset encoding = UTF8;

    public ApeTagTextField(String id, String value, boolean common)
    {
        super(id, FLAG_TYPE_TEXT, common);
        this.value = value == null ? "" : value;
    }

    @Override
    public byte[] getRawContent() throws UnsupportedEncodingException
    {
        return value.getBytes(encoding);
    }

    @Override
    public boolean isBinary()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return value == null ? "" : value.replace('\u0000', '/');
    }

    @Override
    public String getContent()
    {
        return value;
    }

    @Override
    public Charset getEncoding()
    {
        return encoding;
    }

    @Override
    public void setContent(String content)
    {
        this.value = content == null ? "" : content;
    }

    @Override
    public void setEncoding(Charset encoding)
    {
        this.encoding = UTF8;
    }

    @Override
    protected void copyValueFrom(ApeTagField field)
    {
        if (field instanceof ApeTagTextField)
        {
            ApeTagTextField textField = (ApeTagTextField)field;
            this.value = textField.value;
            this.encoding = textField.encoding;
        }
    }
}
