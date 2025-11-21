package com.app.annytunes.uart.zones;

import com.app.annytunes.uart.channels.ChannelIo;

/**
 * Zone read/write helpers. Record layout ASSUMED (needs confirmation with AMMD dump):
 * 0x00..0x0F  Name (16 bytes, null-terminated, ASCII ISO-8859-1)
 * 0x10..end   Channel list: sequence of 2-byte little-endian unsigned integers (1-based channel numbers).
 * Terminator: 0x0000 or 0xFFFF. Remaining bytes may be 0xFF padding.
 * <p>
 * Default constants point to a speculative base address. Adjust via parameters if different.
 */
public class ZoneIo {
    // Change default record size to 128 (must be <=255 to fit single frame on write)
    public static final int DEFAULT_ZONE_RECORD_SIZE = 32; // reverted from 128 per user request
    public static final long DEFAULT_ZONE_BASE = 0x02540000L; // zone name records base restored
    // Added total zones constant and accessor (required by ZoneActivity)
    private static final int TOTAL_ZONES = 250; // provisional upper bound

    public static int getTotalZones() {
        return TOTAL_ZONES;
    }

    /**
     * Compute address for 1-based zone number
     */
    public static long addressOfZone(int zoneIndex1Based) {
        if (zoneIndex1Based < 1) throw new IllegalArgumentException("zoneIndex<1");
        return DEFAULT_ZONE_BASE + (long) (zoneIndex1Based - 1) * DEFAULT_ZONE_RECORD_SIZE;
    }

    public static Zone decodeZone(byte[] raw) {
        Zone z = new Zone();
        if (raw == null || raw.length < 32) return z;
        boolean allFF = true;
        boolean allZero = true;
        for (int i = 0; i < 16 && i < raw.length; i++) {
            int b = raw[i] & 0xFF;
            if (b != 0xFF) allFF = false;
            if (b != 0x00) allZero = false;
        }
        if (allFF || allZero) {
            z.name = "";
            z.channelNumbers = new int[0];
            return z;
        }
        int nameLen = 0;
        while (nameLen < 16 && nameLen < raw.length && raw[nameLen] != 0) nameLen++;
        z.name = new String(raw, 0, nameLen, java.nio.charset.StandardCharsets.ISO_8859_1).trim();
        // Do NOT parse channels here; membership comes from ZoneChannelsIo (separate area)
        z.channelNumbers = new int[0];
        return z;
    }

    public static byte[] encodeZone(Zone z, int recSize) {
        if (recSize <= 0) recSize = DEFAULT_ZONE_RECORD_SIZE;
        byte[] raw = new byte[recSize];
        // name
        byte[] nameBytes = (z == null || z.name == null) ? new byte[0] : z.name.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        int nlen = Math.min(15, nameBytes.length); // reserve 1 for null
        if (nlen > 0) System.arraycopy(nameBytes, 0, raw, 0, nlen);
        if (nlen < 16) raw[nlen] = 0; // terminator
        // channels
        int[] chans = (z == null || z.channelNumbers == null) ? new int[0] : z.channelNumbers;
        int maxEntries = (recSize - 0x10) / 2;
        int count = Math.min(chans.length, maxEntries - 1); // leave space for terminator
        int pos = 0x10;
        for (int i = 0; i < count; i++) {
            int v = chans[i];
            if (v < 0 || v > 0xFFFF) v = 0; // clamp
            raw[pos] = (byte) (v & 0xFF);
            raw[pos + 1] = (byte) ((v >> 8) & 0xFF);
            pos += 2;
        }
        // terminator 0x0000
        if (pos + 1 < raw.length) {
            raw[pos] = 0x00;
            raw[pos + 1] = 0x00;
        }
        return raw;
    }

    private static int safeTotalChannels() {
        try {
            return ChannelIo.getObj().getTotalChannels();
        } catch (Throwable ignored) {
            return Integer.MAX_VALUE;
        }
    }
}
