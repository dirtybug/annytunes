package com.app.annytunes.uart.channels;

import android.util.Log;

import com.app.annytunes.uart.Bank;
import com.app.annytunes.uart.CommsThread;

import java.io.IOException;
import java.util.List;


public class ChannelIo {
    private static final String TAG = "ChannelIo";
    // Moved from ModelProfile
    public static final int CH_OFFSET = 0x40; // 64 bytes per channel record
    public static final Bank[] BANKS = new Bank[] {
        new Bank(0x00800000L, 0x2000, 128,   1, 128 ),
        new Bank(0x00840000L, 0x2000, 128, 129, 256 ),
        new Bank(0x00880000L, 0x2000, 128, 257, 384 ),
        new Bank(0x008C0000L, 0x2000, 128, 385, 512 ),
        new Bank(0x00900000L, 0x2000, 128, 513, 640 ),
        new Bank(0x00940000L, 0x2000, 128, 641, 768 ),
        new Bank(0x00980000L, 0x2000, 128, 769, 896 ),
        new Bank(0x009C0000L, 0x2000, 128, 897,1024 ),
        new Bank(0x00A00000L, 0x2000, 128, 1025,1152),
        new Bank(0x00A40000L, 0x2000, 128, 1153,1280),
        new Bank(0x00A80000L, 0x2000, 128, 1281,1408),
        new Bank(0x00AC0000L, 0x2000, 128, 1409,1536),
        new Bank(0x00B00000L, 0x2000, 128, 1537,1664),
        new Bank(0x00B40000L, 0x2000, 128, 1665,1792),
        new Bank(0x00B80000L, 0x2000, 128, 1793,1920),
        new Bank(0x00BC0000L, 0x2000, 128, 1921,2048),
        new Bank(0x00C00000L, 0x2000, 128, 2049,2176),
        new Bank(0x00C40000L, 0x2000, 128, 2177,2304),
        new Bank(0x00C80000L, 0x2000, 128, 2305,2432),
        new Bank(0x00CC0000L, 0x2000, 128, 2433,2560),
        new Bank(0x00D00000L, 0x2000, 128, 2561,2688),
        new Bank(0x00D40000L, 0x2000, 128, 2689,2816),
        new Bank(0x00D80000L, 0x2000, 128, 2817,2944),
        new Bank(0x00DC0000L, 0x2000, 128, 2945,3072),
        new Bank(0x00E00000L, 0x2000, 128, 3073,3200),
        new Bank(0x00E40000L, 0x2000, 128, 3201,3328),
        new Bank(0x00E80000L, 0x2000, 128, 3329,3456),
        new Bank(0x00EC0000L, 0x2000, 128, 3457,3584),
        new Bank(0x00F00000L, 0x2000, 128, 3585,3712),
        new Bank(0x00F40000L, 0x2000, 128, 3713,3840),
        new Bank(0x00F80000L, 0x2000, 128, 3841,3968),
        new Bank(0x00FC0000L, 0x0880,  32, 3969,4000)
    };

    public static final boolean DEBUG = true;

    private static ChannelIo instance;

    // Change to no-arg constructor that pulls UART from AnytoneUart singleton
    public ChannelIo() {
        // Initialize singleton without requiring AnytoneUart at app startup
        synchronized (ChannelIo.class) { if (instance == null) instance = this; }
    }

    // Single static accessor that never constructs; throws if not initialized
    public static ChannelIo getObj() {
        if (instance == null) throw new IllegalStateException("ChannelIo not initialized; construct it first with new ChannelIo()");
        return instance;
    }





    // New helper methods replacing ModelProfile
    public int getBankCount() { return BANKS.length; }
    public Bank getBank(int idx) { return BANKS[idx]; }

    public int getTotalChannels() {
        int total = 0; for (Bank b : BANKS) total += b.channels; return total;
    }



    private static final Object TRANSFER_LOCK = new Object();




    public void writeAllChannels(List<Channel> chans) throws IOException {
        synchronized (TRANSFER_LOCK) {

            if (chans == null || chans.isEmpty()) {
                throw new IOException("Channel list is empty, nothing to write");
            }
            final int perChunk = 0xFF / CH_OFFSET;
            int bankCount = getBankCount();
            int globalIndex = 0;
            CommsThread comms = CommsThread.getObj();
            try {
                for (int bank = 0; bank < bankCount; bank++) {
                    Bank bk = getBank(bank);
                    long bankBase = bk.address;
                    int perBank = bk.channels;
                    for (int i = 0; i < perBank; i += perChunk) {
                        int recsThis = Math.min(perChunk, perBank - i);
                        int bytesThis = recsThis * CH_OFFSET;
                        byte[] chunk = new byte[bytesThis];
                        for (int r = 0; r < recsThis; r++) {
                            Channel c = (globalIndex < chans.size()) ? chans.get(globalIndex) : new Channel();
                            byte[] rec = encodeChannel(c, CH_OFFSET);
                            System.arraycopy(rec, 0, chunk, r * CH_OFFSET, CH_OFFSET);
                            globalIndex++;
                        }
                        long addr = bankBase + (long) i * CH_OFFSET;
                        if (DEBUG) Log.d(TAG, String.format("TX chunk submit bank=%d addr=0x%08X recs=%d bytes=%d", bank, (int) addr, recsThis, bytesThis));
                        try { comms.submitWrite(addr, chunk); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("Interrupted while submitting write chunk", e); }
                    }
                }
            } finally {
                try { comms.commitWriteSync(0); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("Interrupted finishing writes", e); }
            }
        }
    }

    public Channel decodeChannel(byte[] slab, int recOff, int recSize) {
        Channel c = new Channel();
        if (slab == null || recOff < 0 || recSize < 64 || slab.length < recOff + recSize || isEmptyRecord(slab, recOff, recSize)) {
            c.name = ""; c.rxHz = 0; c.txHz = 0; c.digital = false; c.colorCode = 0; c.timeslot = 0;
            c.contactId = 0; c.contactName = ""; c.radioIdIndex = 0; c.bandwidthKHz = 0.0; c.admit = ""; c.power = 0;
            return c;
        }
        long rxHz = bcd4_to_hz(slab, recOff);
        long shift = bcd4_to_hz(slab, recOff + 0x04);
        c.rxHz = rxHz;
        boolean ts1 = (slab[recOff + 0x34] & 0x08) != 0;
        c.timeslot = ts1 ? 1 : 2;
        c.digital = true;
        c.colorCode = slab[recOff + 0x11] & 0x0F;
        c.contactId = (slab[recOff + 0x18] & 0xFF) | ((slab[recOff + 0x19] & 0xFF) << 8) | ((slab[recOff + 0x1A] & 0xFF) & 0xFF) << 16;
        c.radioIdIndex = slab[recOff + 0x1F] & 0xFF;
        c.power = slab[recOff + 0x20] & 0xFF;
        c.bandwidthKHz = ((slab[recOff + 0x1E] & 0xFF) == 1) ? 25.0 : 12.5;
        int admit = slab[recOff + 0x1D] & 0xFF;
        switch (admit) {
            case 1: c.admit = "CC Free"; break;
            case 2: c.admit = "Channel Free"; break;
            default: c.admit = "Always"; break;
        }
        int nameEnd = recOff + 0x23;
        while (nameEnd < recOff + recSize && slab[nameEnd] != 0) nameEnd++;
        c.name = new String(slab, recOff + 0x23, Math.max(0, nameEnd - (recOff + 0x23)), java.nio.charset.StandardCharsets.ISO_8859_1).trim();
        c.txHz = (shift > 0) ? (c.rxHz - shift) : c.rxHz;
        c.contactName = "";
        return c;
    }

    // Re-added instance encodeChannel method (previously removed by refactor)
    public byte[] encodeChannel(Channel c, int recSize) {
        if (c == null) c = new Channel();
        byte[] raw = new byte[recSize];
        hz_to_bcd4(c.rxHz, raw, 0x00);
        long shift = 0;
        if (c.txHz != 0 && c.txHz != c.rxHz) {
            long d = c.rxHz - c.txHz;
            if (d > 0) shift = d; else shift = 0;
        }
        hz_to_bcd4(shift, raw, 0x04);
        raw[0x11] = (byte) (c.colorCode & 0x0F);
        if (c.timeslot == 1) raw[0x34] |= 0x08; else raw[0x34] &= ~0x08;
        raw[0x18] = (byte) (c.contactId & 0xFF);
        raw[0x19] = (byte) ((c.contactId >> 8) & 0xFF);
        raw[0x1A] = (byte) ((c.contactId >> 16) & 0xFF);
        raw[0x1F] = (byte) (c.radioIdIndex & 0xFF);
        raw[0x1E] = (byte) ((c.bandwidthKHz >= 25.0) ? 1 : 0);
        int admitVal;
        String adm = (c.admit == null) ? "" : c.admit.trim();
        if (adm.equalsIgnoreCase("CC Free")) admitVal = 1; else if (adm.equalsIgnoreCase("Channel Free")) admitVal = 2; else admitVal = 0;
        raw[0x1D] = (byte) admitVal;
        raw[0x20] = (byte) Math.max(0, Math.min(3, c.power));
        byte[] name = (c.name == null ? new byte[0] : c.name.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        int maxName = Math.max(0, recSize - 0x23);
        int nlen = Math.min(name.length, Math.max(0, maxName - 1));
        if (nlen > 0) System.arraycopy(name, 0, raw, 0x23, nlen);
        if (maxName > 0) {
            int termPos = 0x23 + nlen; if (termPos < raw.length) raw[termPos] = 0;
        }
        return raw;
    }




    // Submit read tasks for all banks without blocking; results delivered via CommsThread observer
    public void readAllChannelsAsync() throws IOException {
        synchronized (TRANSFER_LOCK) {
            CommsThread comms = CommsThread.getObj();
            final int perChunk = 0xFF / CH_OFFSET;
            int bankCount = getBankCount();
            for (int bank = 0; bank < bankCount; bank++) {
                Bank bk = getBank(bank);
                long bankBase = bk.address;
                int perBank = bk.channels;
                for (int i = 0; i < perBank; i += perChunk) {
                    int recsThis = Math.min(perChunk, perBank - i);
                    long addrChunk = bankBase + (long) i * CH_OFFSET;
                    try {
                        comms.submitReadDecode(addrChunk, recsThis, CH_OFFSET);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted submitting read task", e);
                    }
                }
            }
            // Removed comms.submitPoison(); do not poison thread after reads.
        }
    }

    // make bcd4_to_hz, hz_to_bcd4, isEmptyRecord private instance helpers
    private long bcd4_to_hz(byte[] raw, int off) {
        long v = 0;
        for (int i = 0; i < 4; i++) {
            int b = raw[off + i] & 0xFF, hi = (b >> 4) & 0xF, lo = b & 0xF;
            if (hi > 9 || lo > 9) return 0L;
            v = v * 10 + hi;
            v = v * 10 + lo;
        }
        return v * 10L;
    }

    private void hz_to_bcd4(long hz, byte[] raw, int off) {
        long v = hz / 10;
        int[] digs = new int[8];
        for (int i = 7; i >= 0; i--) {
            digs[i] = (int) (v % 10);
            v /= 10;
        }
        for (int i = 0; i < 4; i++)
            raw[off + i] = (byte) ((digs[i * 2] << 4) | (digs[i * 2 + 1]));
    }

    // single isEmptyRecord definition
    private boolean isEmptyRecord(byte[] slab, int recOff, int recSize) {
        boolean allZero = true, allFF = true;
        for (int i = 0; i < recSize; i++) {
            byte v = slab[recOff + i];
            if (v != 0) allZero = false;
            if ((v & 0xFF) != 0xFF) allFF = false;
            if (!allZero && !allFF) break;
        }
        if (allZero || allFF) return true;
        int nameEnd = recOff + 0x23;
        while (nameEnd < recOff + recSize && slab[nameEnd] != 0) nameEnd++;
        boolean nameEmpty = (nameEnd == recOff + 0x23);
        long rx = bcd4_to_hz(slab, recOff);
        return nameEmpty && rx == 0L;
    }

    public long channelIndexToAddress(int channelIndex) {
        if (channelIndex < 1) return -1L; // channels defined 1-based in Bank
        for (Bank b : BANKS) {
            if (channelIndex >= b.startChannel && channelIndex <= b.endChannel) {
                int offset = channelIndex - b.startChannel; // zero-based within bank
                return b.address + (long) offset * CH_OFFSET;
            }
        }
        return -1L; // not found
    }


}
