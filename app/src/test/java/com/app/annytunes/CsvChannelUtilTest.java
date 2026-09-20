package com.app.annytunes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.app.annytunes.uart.channels.Channel;
import com.app.annytunes.uart.channels.CsvChannelUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvChannelUtilTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testReadNonExistentFileReturnsEmpty() throws IOException {
        File missing = new File(tempFolder.getRoot(), "missing.csv");
        List<Channel> channels = CsvChannelUtil.read(missing);
        assertNotNull(channels);
        assertTrue(channels.isEmpty());
    }

    @Test
    public void testWriteAndReadRoundTrip() throws IOException {
        File csvFile = tempFolder.newFile("channels.csv");

        List<Channel> originalList = new ArrayList<>();

        Channel ch1 = new Channel();
        ch1.name = "Calling 2m";
        ch1.rxHz = 145500000L;
        ch1.txHz = 145500000L;
        ch1.digital = false;
        ch1.power = 1;
        ch1.bandwidthKHz = 25.0;
        ch1.admit = "Always";
        originalList.add(ch1);

        Channel ch2 = new Channel();
        ch2.name = "DMR BrandM";
        ch2.rxHz = 438800000L;
        ch2.txHz = 431200000L;
        ch2.digital = true;
        ch2.colorCode = 1;
        ch2.timeslot = 1;
        ch2.contactName = "Local 268";
        ch2.contactId = 268;
        ch2.bandwidthKHz = 12.5;
        ch2.admit = "Color Code";
        originalList.add(ch2);

        // Write to CSV
        CsvChannelUtil.write(originalList, csvFile);
        assertTrue(csvFile.exists());
        assertTrue(csvFile.length() > 0);

        // Read back from CSV
        List<Channel> parsedList = CsvChannelUtil.read(csvFile);
        assertNotNull(parsedList);
        assertEquals(2, parsedList.size());

        // Verify Channel 1 (Analog)
        Channel p1 = parsedList.get(0);
        assertEquals("Calling 2m", p1.name);
        assertEquals(145500000L, p1.rxHz);
        assertEquals(145500000L, p1.txHz);
        assertFalse(p1.digital);
        assertEquals(25.0, p1.bandwidthKHz, 0.001);

        // Verify Channel 2 (DMR Digital)
        Channel p2 = parsedList.get(1);
        assertEquals("DMR BrandM", p2.name);
        assertEquals(438800000L, p2.rxHz);
        assertEquals(431200000L, p2.txHz);
        assertTrue(p2.digital);
        assertEquals(1, p2.colorCode);
        assertEquals(1, p2.timeslot);
        assertEquals("Local 268", p2.contactName);
        assertEquals(12.5, p2.bandwidthKHz, 0.001);
    }

    @Test
    public void testEmptyListWriteGeneratesHeaderOnly() throws IOException {
        File csvFile = tempFolder.newFile("empty.csv");
        CsvChannelUtil.write(new ArrayList<>(), csvFile);
        assertTrue(csvFile.exists());

        List<Channel> parsed = CsvChannelUtil.read(csvFile);
        assertNotNull(parsed);
        assertTrue(parsed.isEmpty());
    }
}
