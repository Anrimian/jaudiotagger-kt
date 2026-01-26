package org.jaudiotagger.tag.ape;

import org.jaudiotagger.audio.generic.AbstractTag;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.images.Artwork;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Logical representation of an APEv2 tag.
 */
public class ApeTag extends AbstractTag
{
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final EnumMap<FieldKey, ApeFieldKey> FIELD_MAP = new EnumMap<FieldKey, ApeFieldKey>(FieldKey.class);

    static
    {
        FIELD_MAP.put(FieldKey.ALBUM, ApeFieldKey.ALBUM);
        FIELD_MAP.put(FieldKey.ALBUM_ARTIST, ApeFieldKey.ALBUM_ARTIST);
        FIELD_MAP.put(FieldKey.ALBUM_ARTISTS, ApeFieldKey.ALBUM_ARTIST);
        FIELD_MAP.put(FieldKey.ALBUM_ARTIST_SORT, ApeFieldKey.ALBUM_ARTIST_SORT);
        FIELD_MAP.put(FieldKey.ALBUM_ARTISTS_SORT, ApeFieldKey.ALBUM_ARTIST_SORT);
        FIELD_MAP.put(FieldKey.ALBUM_SORT, ApeFieldKey.ALBUM_SORT);
        FIELD_MAP.put(FieldKey.ARTIST, ApeFieldKey.ARTIST);
        FIELD_MAP.put(FieldKey.ARTISTS, ApeFieldKey.ARTISTS);
        FIELD_MAP.put(FieldKey.ARTISTS_SORT, ApeFieldKey.ARTISTS_SORT);
        FIELD_MAP.put(FieldKey.ARTIST_SORT, ApeFieldKey.ARTIST_SORT);
        FIELD_MAP.put(FieldKey.TITLE, ApeFieldKey.TITLE);
        FIELD_MAP.put(FieldKey.TITLE_SORT, ApeFieldKey.TITLE_SORT);
        FIELD_MAP.put(FieldKey.SUBTITLE, ApeFieldKey.SUBTITLE);
        FIELD_MAP.put(FieldKey.TRACK, ApeFieldKey.TRACK);
        FIELD_MAP.put(FieldKey.TRACK_TOTAL, ApeFieldKey.TRACK_TOTAL);
        FIELD_MAP.put(FieldKey.DISC_NO, ApeFieldKey.DISC);
        FIELD_MAP.put(FieldKey.DISC_SUBTITLE, ApeFieldKey.DISC_SUBTITLE);
        FIELD_MAP.put(FieldKey.DISC_TOTAL, ApeFieldKey.DISC_TOTAL);
        FIELD_MAP.put(FieldKey.YEAR, ApeFieldKey.YEAR);
        FIELD_MAP.put(FieldKey.GENRE, ApeFieldKey.GENRE);
        FIELD_MAP.put(FieldKey.COMMENT, ApeFieldKey.COMMENT);
        FIELD_MAP.put(FieldKey.ARRANGER, ApeFieldKey.ARRANGER);
        FIELD_MAP.put(FieldKey.ARRANGER_SORT, ApeFieldKey.ARRANGER_SORT);
        FIELD_MAP.put(FieldKey.COMPOSER, ApeFieldKey.COMPOSER);
        FIELD_MAP.put(FieldKey.COMPOSER_SORT, ApeFieldKey.COMPOSER_SORT);
        FIELD_MAP.put(FieldKey.CONDUCTOR, ApeFieldKey.CONDUCTOR);
        FIELD_MAP.put(FieldKey.CONDUCTOR_SORT, ApeFieldKey.CONDUCTOR_SORT);
        FIELD_MAP.put(FieldKey.COPYRIGHT, ApeFieldKey.COPYRIGHT);
        FIELD_MAP.put(FieldKey.ENCODER, ApeFieldKey.ENCODER);
        FIELD_MAP.put(FieldKey.ENGINEER, ApeFieldKey.ENGINEER);
        FIELD_MAP.put(FieldKey.ENGINEER_SORT, ApeFieldKey.ENGINEER_SORT);
        FIELD_MAP.put(FieldKey.AUDIO_ENGINEER, ApeFieldKey.ENGINEER);
        FIELD_MAP.put(FieldKey.AUDIO_ENGINEER_SORT, ApeFieldKey.ENGINEER_SORT);
        FIELD_MAP.put(FieldKey.SOUND_ENGINEER, ApeFieldKey.ENGINEER);
        FIELD_MAP.put(FieldKey.SOUND_ENGINEER_SORT, ApeFieldKey.ENGINEER_SORT);
        FIELD_MAP.put(FieldKey.RECORDING_ENGINEER, ApeFieldKey.ENGINEER);
        FIELD_MAP.put(FieldKey.RECORDING_ENGINEER_SORT, ApeFieldKey.ENGINEER_SORT);
        FIELD_MAP.put(FieldKey.BALANCE_ENGINEER, ApeFieldKey.ENGINEER);
        FIELD_MAP.put(FieldKey.BALANCE_ENGINEER_SORT, ApeFieldKey.ENGINEER_SORT);
        FIELD_MAP.put(FieldKey.LYRICS, ApeFieldKey.LYRICS);
        FIELD_MAP.put(FieldKey.LYRICIST, ApeFieldKey.LYRICIST);
        FIELD_MAP.put(FieldKey.LYRICIST_SORT, ApeFieldKey.LYRICIST_SORT);
        FIELD_MAP.put(FieldKey.COVER_ART, ApeFieldKey.COVER_ART_FRONT);
        FIELD_MAP.put(FieldKey.IS_COMPILATION, ApeFieldKey.COMPILATION);
        FIELD_MAP.put(FieldKey.BARCODE, ApeFieldKey.BARCODE);
        FIELD_MAP.put(FieldKey.BPM, ApeFieldKey.BPM);
        FIELD_MAP.put(FieldKey.FBPM, ApeFieldKey.FBPM);
        FIELD_MAP.put(FieldKey.CATALOG_NO, ApeFieldKey.CATALOG_NO);
        FIELD_MAP.put(FieldKey.ISRC, ApeFieldKey.ISRC);
        FIELD_MAP.put(FieldKey.KEY, ApeFieldKey.KEY);
        FIELD_MAP.put(FieldKey.LANGUAGE, ApeFieldKey.LANGUAGE);
        FIELD_MAP.put(FieldKey.MEDIA, ApeFieldKey.MEDIA);
        FIELD_MAP.put(FieldKey.MOOD, ApeFieldKey.MOOD);
        FIELD_MAP.put(FieldKey.CUSTOM1, ApeFieldKey.CUSTOM1);
        FIELD_MAP.put(FieldKey.CUSTOM2, ApeFieldKey.CUSTOM2);
        FIELD_MAP.put(FieldKey.CUSTOM3, ApeFieldKey.CUSTOM3);
        FIELD_MAP.put(FieldKey.CUSTOM4, ApeFieldKey.CUSTOM4);
        FIELD_MAP.put(FieldKey.CUSTOM5, ApeFieldKey.CUSTOM5);
        FIELD_MAP.put(FieldKey.RECORD_LABEL, ApeFieldKey.LABEL);
        FIELD_MAP.put(FieldKey.PERFORMER, ApeFieldKey.PERFORMER);
        FIELD_MAP.put(FieldKey.PERFORMER_NAME, ApeFieldKey.PERFORMER);
        FIELD_MAP.put(FieldKey.PERFORMER_NAME_SORT, ApeFieldKey.PERFORMER_SORT);
        FIELD_MAP.put(FieldKey.REMIXER, ApeFieldKey.REMIXER);
        FIELD_MAP.put(FieldKey.DJMIXER, ApeFieldKey.DJMIXER);
        FIELD_MAP.put(FieldKey.DJMIXER_SORT, ApeFieldKey.DJMIXER_SORT);
        FIELD_MAP.put(FieldKey.MIXER, ApeFieldKey.MIXER);
        FIELD_MAP.put(FieldKey.MIXER_SORT, ApeFieldKey.MIXER_SORT);
        FIELD_MAP.put(FieldKey.PRODUCER, ApeFieldKey.PRODUCER);
        FIELD_MAP.put(FieldKey.PRODUCER_SORT, ApeFieldKey.PRODUCER_SORT);
        FIELD_MAP.put(FieldKey.RATING, ApeFieldKey.RATING);
        FIELD_MAP.put(FieldKey.REPLAYGAIN_TRACK_GAIN, ApeFieldKey.REPLAYGAIN_TRACK_GAIN);
        FIELD_MAP.put(FieldKey.REPLAYGAIN_TRACK_PEAK, ApeFieldKey.REPLAYGAIN_TRACK_PEAK);
        FIELD_MAP.put(FieldKey.REPLAYGAIN_ALBUM_GAIN, ApeFieldKey.REPLAYGAIN_ALBUM_GAIN);
        FIELD_MAP.put(FieldKey.REPLAYGAIN_ALBUM_PEAK, ApeFieldKey.REPLAYGAIN_ALBUM_PEAK);
    }

    public ApeTag()
    {
    }

    public static ApeTag createDefaultTag()
    {
        return new ApeTag();
    }

    @Override
    protected boolean isAllowedEncoding(Charset enc)
    {
        return UTF8.equals(enc);
    }

    @Override
    public void addField(TagField field)
    {
        storeField(field, false);
    }

    @Override
    public void setField(TagField field)
    {
        storeField(field, true);
    }

    private void storeField(TagField field, boolean replace)
    {
        if (field == null)
        {
            return;
        }
        String storageKey = determineStorageKey(field.getId());
        List<TagField> list = fields.get(storageKey);
        if (list == null)
        {
            list = new ArrayList<TagField>();
            fields.put(storageKey, list);
            if (field.isCommon())
            {
                commonNumber++;
            }
        }
        if (replace)
        {
            if (list.isEmpty())
            {
                list.add(field);
            }
            else
            {
                list.set(0, field);
            }
        }
        else
        {
            list.add(field);
        }
    }

    @Override
    public List<TagField> getFields(String id)
    {
        String key = resolveExistingKey(id);
        if (key == null)
        {
            return new ArrayList<TagField>();
        }
        List<TagField> stored = fields.get(key);
        return stored != null ? stored : new ArrayList<TagField>();
    }

    @Override
    public boolean hasField(String id)
    {
        return resolveExistingKey(id) != null;
    }

    @Override
    public void deleteField(String key)
    {
        String existing = resolveExistingKey(key);
        if (existing != null)
        {
            fields.remove(existing);
        }
    }

    private String determineStorageKey(String id)
    {
        String normalized = normalizeId(id);
        String existing = resolveExistingKey(normalized);
        return existing != null ? existing : normalized;
    }

    private String resolveExistingKey(String id)
    {
        String normalized = normalizeId(id);
        if (normalized == null)
        {
            return null;
        }
        if (fields.containsKey(normalized))
        {
            return normalized;
        }
        for (String key : fields.keySet())
        {
            if (key.equalsIgnoreCase(normalized))
            {
                return key;
            }
        }
        return null;
    }

    private String normalizeId(String id)
    {
        if (id == null)
        {
            return null;
        }
        ApeFieldKey key = ApeFieldKey.fromFieldName(id);
        return key != null ? key.getFieldName() : id;
    }

    private ApeFieldKey requireKey(FieldKey genericKey) throws KeyNotFoundException
    {
        ApeFieldKey key = FIELD_MAP.get(genericKey);
        if (key == null)
        {
            throw new KeyNotFoundException("Field " + genericKey + " is not supported for APEv2 tags");
        }
        return key;
    }

    @Override
    public TagField createField(FieldKey genericKey, String... value) throws KeyNotFoundException, FieldDataInvalidException
    {
        if (value == null || value.length == 0)
        {
            throw new FieldDataInvalidException("Value cannot be null");
        }
        ApeFieldKey key = requireKey(genericKey);
        return createField(key, value[0]);
    }

    public TagField createField(ApeFieldKey fieldKey, String value)
    {
        if (fieldKey == null)
        {
            throw new IllegalArgumentException("Field key cannot be null");
        }
        return new ApeTagTextField(fieldKey.getFieldName(), value, fieldKey.isCommon());
    }

    public TagField createField(String fieldName, String value)
    {
        return new ApeTagTextField(fieldName, value, false);
    }

    @Override
    public TagField getFirstField(FieldKey genericKey) throws KeyNotFoundException
    {
        ApeFieldKey key = requireKey(genericKey);
        return super.getFirstField(key.getFieldName());
    }

    @Override
    public List<TagField> getFields(FieldKey key) throws KeyNotFoundException
    {
        ApeFieldKey apeKey = requireKey(key);
        return super.getFields(apeKey.getFieldName());
    }

    @Override
    public List<String> getAll(FieldKey genericKey) throws KeyNotFoundException
    {
        ApeFieldKey key = requireKey(genericKey);
        return super.getAll(key.getFieldName());
    }

    @Override
    public String getValue(FieldKey id, int n)
    {
        try
        {
            ApeFieldKey key = requireKey(id);
            return getItem(key.getFieldName(), n);
        }
        catch (KeyNotFoundException e)
        {
            return "";
        }
    }

    @Override
    public void deleteField(FieldKey fieldKey) throws KeyNotFoundException
    {
        ApeFieldKey key = requireKey(fieldKey);
        deleteField(key.getFieldName());
    }

    @Override
    public List<Artwork> getArtworkList()
    {
        List<Artwork> artwork = new ArrayList<Artwork>();
        appendArtworkForField(artwork, ApeFieldKey.COVER_ART_FRONT.getFieldName());
        appendArtworkForField(artwork, ApeFieldKey.COVER_ART_BACK.getFieldName());
        return artwork;
    }

    private void appendArtworkForField(List<Artwork> target, String id)
    {
        List<TagField> fields = super.getFields(id);
        for (TagField field : fields)
        {
            if (field instanceof ApeTagCoverField coverField)
            {
                target.add(coverField.toArtwork());
            }
        }
    }

    @Override
    public TagField createField(Artwork artwork) throws FieldDataInvalidException
    {
        return new ApeTagCoverField(artwork);
    }

    @Override
    public String toString()
    {
        return "APE " + super.toString();
    }

    @Override
    public TagField createCompilationField(boolean value) throws KeyNotFoundException, FieldDataInvalidException
    {
        return createField(ApeFieldKey.COMPILATION, value ? "1" : "0");
    }
}
