package com.app.annytunes.uart.zones;

import java.util.Arrays;

/**
 * Represents a Zone (a named grouping of channel numbers) on an Anytone radio.
 * <p>
 * ASSUMPTION (needs verification against AMMD / OpenRTX memory map):
 * - Each zone record has fixed size (ZoneIo.ZONE_RECORD_SIZE), default 256 bytes.
 * - Name is ASCII (ISO-8859-1) null-terminated at offset 0, max 16 bytes (15 + null).
 * - Channel list begins at offset 0x10. Each entry is a 2-byte little-endian unsigned
 * 1-based channel number. 0x0000 or 0xFFFF marks end of list. Unused trailing bytes
 * may be 0xFF.
 * <p>
 * This structure was chosen to mirror typical vendor layouts; adjust offsets once the
 * actual AMMD map is confirmed. See ZoneIo for encoding/decoding logic.
 */
public class Zone {
    public static boolean changedName;    // UI flag: name edited by user (applies to all zones)
    public String name;            // zone name
    public int[] channelNumbers;   // 1-based channel numbers included in zone (no duplicates ideally)
    public boolean changedChannels;// UI flag: membership changed by user

    public Zone() {
        this.name = "";
        this.channelNumbers = new int[0];
        this.changedChannels = false;
    }

    public Zone(String name, int[] chans) {
        this.name = (name == null ? "" : name.trim());
        this.channelNumbers = (chans == null ? new int[0] : chans.clone());
        this.changedChannels = false;
    }


    @Override
    public String toString() {
        return "Zone{name='" + name + "', channels=" + Arrays.toString(channelNumbers) + "}";
    }
}
