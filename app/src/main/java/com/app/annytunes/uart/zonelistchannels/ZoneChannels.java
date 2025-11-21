package com.app.annytunes.uart.zonelistchannels;

/**
 * Represents the channel list membership for a zone.
 * Layout (128 bytes record at base 0x01000000 + (zoneIndex-1)*0x80):
 * Offset 0x00: 16-bit LE zone index (1..N)
 * Offset 0x02: sequence of 16-bit LE channel numbers (1-based). Terminator 0x0000 or 0xFFFF.
 * Remaining bytes: padding (0x00 or 0xFF).
 */
public class ZoneChannels {
    public int zoneIndex;          // 1-based zone index
    public int[] channelNumbers;   // channel numbers in the zone

    public ZoneChannels() {
        this.zoneIndex = 0;
        this.channelNumbers = new int[0];
    }

    public ZoneChannels(int zoneIndex, int[] chans) {
        this.zoneIndex = zoneIndex;
        this.channelNumbers = chans == null ? new int[0] : chans.clone();
    }
}

