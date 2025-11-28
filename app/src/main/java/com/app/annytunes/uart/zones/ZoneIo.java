package com.app.annytunes.uart.zones;

import com.app.annytunes.uart.CommsThread;

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
    public static final int DEFAULT_ZONE_RECORD_SIZE = 32; // 32-byte zone name record
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
        if (raw == null || raw.length < DEFAULT_ZONE_RECORD_SIZE) return z;
        boolean allFF = true, allZero = true;
        for (int i = 0; i < DEFAULT_ZONE_RECORD_SIZE; i++) {
            int b = raw[i] & 0xFF;
            if (b != 0xFF) allFF = false;
            if (b != 0x00) allZero = false;
        }

        int nameLen = 0;
        while (nameLen < (DEFAULT_ZONE_RECORD_SIZE - 1) && raw[nameLen] != 0) nameLen++;

        z.name = new String(raw, 0, nameLen, java.nio.charset.StandardCharsets.ISO_8859_1).trim();
        // channelNumbers populated separately
        z.channelNumbers = new int[0];
        return z;
    }

    public static byte[] encodeZone(Zone z, int recSize) {
        if (recSize <= 0) recSize = DEFAULT_ZONE_RECORD_SIZE;
        byte[] raw = new byte[recSize];
        byte[] nameBytes = (z == null || z.name == null) ? new byte[0] : z.name.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        int nlen = Math.min(recSize - 1, nameBytes.length); // leave room for null terminator
        if (nlen > 0) System.arraycopy(nameBytes, 0, raw, 0, nlen);
        raw[nlen] = 0; // terminator
        return raw;
    }

    public static void writeAllZones(java.util.List<Zone> zones) {
        CommsThread.getObj();
        if (zones == null || zones.isEmpty()) {
            throw new IllegalArgumentException("No zones to write");
        }

        // Calculate the total size required for all zones
        int totalSize = zones.stream()
                .mapToInt(zone -> DEFAULT_ZONE_RECORD_SIZE)
                .sum();
        int paddedSize = (totalSize + 0xFF) & ~0xFF; // Align to 0xFF boundary

        byte[] block = new byte[paddedSize];
        int offset = 0;
        if (!Zone.changedName) {
            return;
        }

        for (Zone zone : zones) {

            byte[] zoneBytes = encodeZone(zone, DEFAULT_ZONE_RECORD_SIZE);
            System.arraycopy(zoneBytes, 0, block, offset, zoneBytes.length);
            offset += zoneBytes.length;

        }

        // Fill the remaining space with padding (e.g., 0xFF)
        for (int i = offset; i < block.length; i++) {
            block[i] = (byte) 0xFF;
        }

        // Write the encoded zones in chunks of maximum size

        long address = DEFAULT_ZONE_BASE;
        try {
            CommsThread.getObj().submitWriteZoneActivity(address, block);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Checks if a zone name is empty (all bytes are 0xFF).
     *
     * @param zoneName The byte array representing the zone name.
     * @return True if the zone name is empty, false otherwise.
     */
    public static boolean isZoneEmpty(byte[] zoneName) {
        if (zoneName == null) return true;
        for (byte b : zoneName) {
            if (b != (byte) 0xFF) {
                return false;
            }
        }
        return true;
    }
}
