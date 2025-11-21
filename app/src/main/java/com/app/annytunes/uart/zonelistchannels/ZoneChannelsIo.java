package com.app.annytunes.uart.zonelistchannels;

import com.app.annytunes.uart.channels.ChannelIo;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Decode/encode zone channel membership records.
 */
public class ZoneChannelsIo {
    public static final int RECORD_STRIDE = 0x200; // each zone channel block spaced 512 bytes
    public static final int RECORD_SIZE = 512;     // full zone channel block size
    public static final long BASE_ADDRESS = 0x01000000L; // channel membership base separate from ZoneIo.DEFAULT_ZONE_BASE
    private static final ConcurrentHashMap<Integer, int[]> CHANNELS_CACHE = new ConcurrentHashMap<>();

    public static long addressOf(int zoneIndex1Based) {
        if (zoneIndex1Based < 1) throw new IllegalArgumentException("zoneIndex<1");
        return BASE_ADDRESS + (long) (zoneIndex1Based - 1) * RECORD_STRIDE;
    }

    public static void putChannels(int zoneIndex, int[] channels) {
        if (zoneIndex > 0)
            CHANNELS_CACHE.put(zoneIndex, channels == null ? new int[0] : channels.clone());
    }

    public static int[] getChannels(int zoneIndex) {
        int[] v = CHANNELS_CACHE.get(zoneIndex);
        return v == null ? new int[0] : v.clone();
    }

    // New decode: record does NOT contain zone index; entire record is 16-bit LE channel numbers until 0x0000/0xFFFF terminator
    public static ZoneChannels decode(int zoneIndex, byte[] raw) {
        ZoneChannels z = new ZoneChannels();
        z.zoneIndex = zoneIndex;
        if (raw == null || raw.length < 2) {
            z.channelNumbers = new int[0];
            return z;
        }
        int maxCh = safeTotalChannels();
        java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
        for (int off = 0; off + 1 < raw.length; off += 2) {
            int lo = raw[off] & 0xFF;
            int hi = raw[off + 1] & 0xFF;
            int v = (hi << 8) | lo;
            if (v == 0xFFFF) break; // terminator
            int chNum = v + 1;    // apply +1 offset for display/use
            if (chNum >= 1 && chNum <= maxCh) list.add(chNum);
        }
        z.channelNumbers = list.stream().mapToInt(Integer::intValue).toArray();
        putChannels(zoneIndex, z.channelNumbers);
        return z;
    }

    // Legacy decode (no zone index available) retained only for backward compatibility; avoids indexing first word as zone ID.
    @Deprecated
    public static ZoneChannels decode(byte[] raw) {
        return decode(-1, raw);
    }

    public static byte[] encode(ZoneChannels z) {
        byte[] raw = new byte[RECORD_SIZE];
        int[] chans = (z == null || z.channelNumbers == null) ? new int[0] : z.channelNumbers;
        int pos = 0;
        int maxEntries = (RECORD_SIZE) / 2 - 1;
        int count = Math.min(chans.length, maxEntries);
        for (int i = 0; i < count; i++) {
            int v = chans[i];
            if (v < 1 || v > 0xFFFF) continue;
            int stored = v - 1; // store channel-1 in raw
            raw[pos] = (byte) (stored & 0xFF);
            raw[pos + 1] = (byte) ((stored >> 8) & 0xFF);
            pos += 2;
        }
        if (pos + 1 < raw.length) {
            raw[pos] = (byte) 0xFF;
            raw[pos + 1] = (byte) 0xFF;
        }
        return raw;
    }

    private static int safeTotalChannels() {
        try {
            return ChannelIo.getObj().getTotalChannels();
        } catch (Throwable ignore) {
            return Integer.MAX_VALUE;
        }
    }
}
