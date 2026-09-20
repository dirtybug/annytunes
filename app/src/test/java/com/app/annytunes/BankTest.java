package com.app.annytunes;

import static org.junit.Assert.assertEquals;

import com.app.annytunes.uart.Bank;

import org.junit.Test;

public class BankTest {

    @Test
    public void testBankRecordCount() {
        // 640 bytes with 64 bytes per record = 10 records
        Bank bank = new Bank(0x100000L, 640, 10, 1, 10);
        assertEquals(10, bank.recordCount());
        assertEquals(1, bank.startChannel);
        assertEquals(10, bank.endChannel);
    }

    @Test
    public void testBankAddressOfRecord() {
        long baseAddress = 0x200000L;
        Bank bank = new Bank(baseAddress, 256, 4, 1, 4);

        assertEquals(baseAddress, bank.addressOfRecord(0));
        assertEquals(baseAddress + 64, bank.addressOfRecord(1));
        assertEquals(baseAddress + 128, bank.addressOfRecord(2));
        assertEquals(baseAddress + 192, bank.addressOfRecord(3));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBankAddressOfRecordNegativeThrows() {
        Bank bank = new Bank(0x100000L, 64, 1, 1, 1);
        bank.addressOfRecord(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBankAddressOfRecordOverflowThrows() {
        Bank bank = new Bank(0x100000L, 128, 2, 1, 2);
        bank.addressOfRecord(2); // valid indexes are 0 and 1
    }
}
