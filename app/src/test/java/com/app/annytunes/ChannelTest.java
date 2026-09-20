package com.app.annytunes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.app.annytunes.uart.channels.Channel;

import org.junit.Test;

public class ChannelTest {

    @Test
    public void testDefaultChannelCreation() {
        Channel ch = new Channel();
        assertNotNull(ch);
        assertFalse(ch.digital);
        assertFalse(ch.edited);
        assertEquals(0L, ch.rxHz);
        assertEquals(0L, ch.txHz);
    }

    @Test
    public void testChannelAnalogFields() {
        Channel ch = new Channel();
        ch.name = "VHF Calling";
        ch.rxHz = 145500000L;
        ch.txHz = 145500000L;
        ch.digital = false;
        ch.bandwidthKHz = 25.0;
        ch.power = 1; // High
        ch.admit = "Always";

        assertEquals("VHF Calling", ch.name);
        assertEquals(145500000L, ch.rxHz);
        assertEquals(145500000L, ch.txHz);
        assertFalse(ch.digital);
        assertEquals(25.0, ch.bandwidthKHz, 0.001);
        assertEquals(1, ch.power);
        assertEquals("Always", ch.admit);
    }

    @Test
    public void testChannelDmrFields() {
        Channel ch = new Channel();
        ch.name = "DMR Local 91";
        ch.rxHz = 438800000L;
        ch.txHz = 431200000L;
        ch.digital = true;
        ch.colorCode = 1;
        ch.timeslot = 2;
        ch.contactId = 91;
        ch.contactName = "Worldwide TG 91";
        ch.bandwidthKHz = 12.5;

        assertTrue(ch.digital);
        assertEquals(1, ch.colorCode);
        assertEquals(2, ch.timeslot);
        assertEquals(91, ch.contactId);
        assertEquals("Worldwide TG 91", ch.contactName);
        assertEquals(12.5, ch.bandwidthKHz, 0.001);
    }

    @Test
    public void testChannelEditedFlag() {
        Channel ch = new Channel();
        assertFalse(ch.edited);
        ch.edited = true;
        assertTrue(ch.edited);
    }
}
