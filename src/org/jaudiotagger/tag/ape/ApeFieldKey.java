package org.jaudiotagger.tag.ape;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enumerates the well-known APEv2 field identifiers used by WavPack.
 */
public enum ApeFieldKey
{
    ALBUM("Album", true),
    ALBUM_ARTIST("Album Artist", true),
    ALBUM_ARTIST_SORT("Album Artist Sort", false),
    ALBUM_SORT("Album Sort", false),
    ALBUM_COMPOSER("Album Composer", false),
    ALBUM_COMPOSER_SORT("Album Composer Sort", false),
    ARTIST("Artist", true),
    ARTISTS("Artists", false),
    ARTISTS_SORT("Artists Sort", false),
    ARTIST_SORT("Artist Sort", false),
    TITLE("Title", true),
    TITLE_SORT("Title Sort", false),
    SUBTITLE("Subtitle", false),
    TRACK("Track", true),
    TRACK_TOTAL("Track Total", false),
    DISC("Disc", false),
    DISC_TOTAL("Disc Total", false),
    DISC_SUBTITLE("Disc Subtitle", false),
    YEAR("Year", true),
    GENRE("Genre", true),
    COMMENT("Comment", true),
    ARRANGER("Arranger", false),
    ARRANGER_SORT("Arranger Sort", false),
    COMPILATION("Compilation", false),
    COMPOSER("Composer", false),
    COMPOSER_SORT("Composer Sort", false),
    CONDUCTOR("Conductor", false),
    CONDUCTOR_SORT("Conductor Sort", false),
    COPYRIGHT("Copyright", false),
    ENCODER("Encoder", false),
    ENGINEER("Engineer", false),
    ENGINEER_SORT("Engineer Sort", false),
    ISRC("ISRC", false),
    KEY("Key", false),
    LABEL("Label", false),
    LANGUAGE("Language", false),
    LYRICIST("Lyricist", false),
    LYRICIST_SORT("Lyricist Sort", false),
    LYRICS("Lyrics", false),
    MEDIA("Media", false),
    MIXER("Mixer", false),
    MIXER_SORT("Mixer Sort", false),
    MOOD("Mood", false),
    PERFORMER("Performer", false),
    PERFORMER_SORT("Performer Sort", false),
    PRODUCER("Producer", false),
    PRODUCER_SORT("Producer Sort", false),
    RATING("Rating", false),
    REMIXER("Remixer", false),
    DJMIXER("DJ Mixer", false),
    DJMIXER_SORT("DJ Mixer Sort", false),
    CUESHEET("Cuesheet", false),
    REPLAYGAIN_TRACK_GAIN("Replaygain_Track_Gain", false),
    REPLAYGAIN_TRACK_PEAK("Replaygain_Track_Peak", false),
    REPLAYGAIN_ALBUM_GAIN("Replaygain_Album_Gain", false),
    REPLAYGAIN_ALBUM_PEAK("Replaygain_Album_Peak", false),
    BPM("BPM", false),
    FBPM("FBPM", false),
    CATALOG_NO("Catalog Number", false),
    BARCODE("Barcode", false),
    COVER_ART_FRONT("Cover Art (Front)", false),
    COVER_ART_BACK("Cover Art (Back)", false),
    LOG("Log", false),
    SETTINGS("Settings", false),
    CUSTOM1("Custom1", false),
    CUSTOM2("Custom2", false),
    CUSTOM3("Custom3", false),
    CUSTOM4("Custom4", false),
    CUSTOM5("Custom5", false);

    private static final Map<String, ApeFieldKey> LOOKUP = new HashMap<String, ApeFieldKey>();

    static
    {
        for (ApeFieldKey key : values())
        {
            LOOKUP.put(key.fieldName.toLowerCase(Locale.ROOT), key);
        }
    }

    private final String fieldName;
    private final boolean common;

    ApeFieldKey(String fieldName, boolean common)
    {
        this.fieldName = fieldName;
        this.common = common;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public boolean isCommon()
    {
        return common;
    }

    public static ApeFieldKey fromFieldName(String name)
    {
        if (name == null)
        {
            return null;
        }
        return LOOKUP.get(name.toLowerCase(Locale.ROOT));
    }
}
