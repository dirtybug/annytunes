package com.app.annytunes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.app.annytunes.uart.zones.Zone;

import org.junit.Test;

public class ZoneTest {

    @Test
    public void testDefaultZoneCreation() {
        Zone zone = new Zone();
        assertNotNull(zone);
        assertEquals("", zone.name);
        assertNotNull(zone.channelNumbers);
        assertEquals(0, zone.channelNumbers.length);
        assertFalse(zone.changedChannels);
    }

    @Test
    public void testZoneWithChannels() {
        int[] channels = new int[]{1, 2, 5, 10, 15};
        Zone zone = new Zone("VHF Local", channels);

        assertEquals("VHF Local", zone.name);
        assertArrayEquals(channels, zone.channelNumbers);
        assertFalse(zone.changedChannels);

        // Modifying original array should not affect the zone's internal clone
        channels[0] = 999;
        assertEquals(1, zone.channelNumbers[0]);
    }

    @Test
    public void testZoneNameTrimming() {
        Zone zone = new Zone("   Repeater Net   ", new int[]{1});
        assertEquals("Repeater Net", zone.name);
    }

    @Test
    public void testZoneToString() {
        Zone zone = new Zone("DMR Group", new int[]{1, 2});
        String str = zone.toString();
        assertNotNull(str);
        assertTrue(str.contains("DMR Group"));
        assertTrue(str.contains("1, 2"));
    }
}
