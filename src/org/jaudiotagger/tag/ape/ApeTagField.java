package org.jaudiotagger.tag.ape;

import org.jaudiotagger.tag.TagField;

/**
 * Base implementation shared by all APEv2 tag fields.
 */
public abstract class ApeTagField implements TagField
{
    public static final int FLAG_TYPE_TEXT = 0x0;
    public static final int FLAG_TYPE_BINARY = 0x1;

    private final String id;
    private final boolean common;
    private int flags;

    protected ApeTagField(String id, int flags, boolean common)
    {
        this.id = id;
        this.flags = flags;
        this.common = common;
    }

    public int getFlags()
    {
        return flags;
    }

    protected void setFlags(int flags)
    {
        this.flags = flags;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public boolean isCommon()
    {
        return common;
    }

    @Override
    public boolean isEmpty()
    {
        try
        {
            return getRawContent().length == 0;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    @Override
    public void isBinary(boolean b)
    {
        if (b)
        {
            flags = FLAG_TYPE_BINARY;
        }
        else
        {
            flags = FLAG_TYPE_TEXT;
        }
    }

    @Override
    public void copyContent(TagField field)
    {
        if (field instanceof ApeTagField )
        {
            ApeTagField apeField = (ApeTagField)field;
            this.flags = apeField.flags;
            copyValueFrom(apeField);
        }
    }

    protected abstract void copyValueFrom(ApeTagField field);
}
