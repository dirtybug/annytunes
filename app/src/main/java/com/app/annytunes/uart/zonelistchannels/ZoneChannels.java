package com.app.annytunes.uart.zonelistchannels;

/**
 * Represents the channel list membership for a zone.
 * Layout (512-byte block at base 0x01000000 + (zoneIndex-1)*0x200):
 * - Sequence of 16-bit little-endian unsigned values, each storing (channelNumber-1).
 * - Terminator can be 0x0000 or 0xFFFF. Remaining bytes are padding (0x00/0xFF).
 * - The zone index is not stored in the block; it's implied by the address stride.
 */
public class ZoneChannels {
    public int zoneIndex;          // 1-based zone index (implied by address; kept here for convenience)
    public int[] channelNumbers;   // 1-based channel numbers in the zone
    public boolean changed;        // UI/logic flag: set to true when membership edited

    public ZoneChannels() {
        this.zoneIndex = 0;
        this.channelNumbers = new int[0];
        this.changed = false;
    }

    public ZoneChannels(int zoneIndex, int[] chans) {
        this.zoneIndex = zoneIndex;
        this.channelNumbers = chans == null ? new int[0] : chans.clone();
        this.changed = false;
    }
}
