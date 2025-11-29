package com.app.annytunes.uart;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation using felHR85 UsbSerial library.
 * Port selector formats:
 * - "vendor:product" hex (e.g. 28e9:0189)
 * - "index:N" zero-based index among enumeration order
 * - empty/null -> first
 */
public class AnytoneUart implements AutoCloseable {
    // Expose constants for CommsThread
    static final byte ACK = 0x06; // was private
    static final int FRAME_HEADER_LEN = 7; // 'W' + addr(4) + size + checksum
    private static final String TAG = "AnytoneUart";
    private static AnytoneUart instance; // singleton instance
    private final UsbManager usbManager;
    private final String selector; // remember selector used
    // RX buffering for async callback
    private final Object rxLock = new Object();
    // Protocol state fields restored for CommsThread parsing access
    public boolean framed;
    public int address;
    public int size;
    public byte[] data;
    public int checksum;
    public int crc;
    public boolean checksumOk;
    public boolean ackPresent;
    private UsbSerialDevice port;
    private UsbDeviceConnection connection;
    private UsbDevice device;
    private byte[] rxBuf = new byte[4096];
    private int rxCount = 0;

    // Constructor sets/overwrites singleton
    public AnytoneUart(Context ctx, String selector) throws IOException {
        this.selector = selector;
        this.usbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) throw new IOException("UsbManager unavailable");
        this.device = pickDevice(selector);
        if (device == null) throw new IOException("No USB device matches selector: " + selector);
        if (!usbManager.hasPermission(device))
            throw new IOException("Missing USB permission for device");
        connection = usbManager.openDevice(device);
        if (connection == null) throw new IOException("Failed to open USB device connection");
        port = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (port == null) throw new IOException("Failed to create UsbSerialDevice");
        if (!port.open()) throw new IOException("Failed to open serial port");
        // felHR85 uses setBaudRate, setDataBits, setStopBits, setParity
        port.setBaudRate(115200);
        port.setDataBits(UsbSerialInterface.DATA_BITS_8);
        port.setStopBits(UsbSerialInterface.STOP_BITS_1);
        port.setParity(UsbSerialInterface.PARITY_NONE);
        port.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        synchronized (AnytoneUart.class) {
            instance = this;
        }
        // start async reading
        try {
            port.read(this::onReceivedData);
        } catch (Exception ignored) {
        }
        // Attach to CommsThread singleton if it's already created (or create lazy if not yet needed)

    }

    // Provide no-arg accessor that throws if not yet constructed
    public static synchronized AnytoneUart getObj() {
        if (instance == null)
            throw new IllegalStateException("AnytoneUart not initialized; construct it first with new AnytoneUart(context, selector)");
        return instance;
    }

    // Optional reset if caller wants to fully drop the port
    public static synchronized void reset() {
        if (instance != null) {
            try {
                instance.close();
            } catch (Exception ignored) {
            }
            instance = null;
        }
    }

    private static String toHex(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("0x%02X", arr[i] & 0xFF));
        }
        return sb.toString();
    }

    public boolean isOpen() {
        return port != null;
    }

    public void onReceivedData(byte[] data) {
        if (data == null || data.length == 0) return;
        // Forward to communications thread reply queue
        CommsThread.getObj();
        CommsThread.enqueueIncoming(data);
        // Retain local buffer for any legacy direct reads (optional)
        synchronized (rxLock) {
            ensureCapacity(rxCount + data.length);
            System.arraycopy(data, 0, rxBuf, rxCount, data.length);
            rxCount += data.length;
            rxLock.notifyAll();
        }
    }

    private void ensureCapacity(int need) {
        if (need <= rxBuf.length) return;
        int newCap = Math.max(rxBuf.length * 2, need);
        byte[] n = new byte[newCap];
        System.arraycopy(rxBuf, 0, n, 0, rxCount);
        rxBuf = n;
    }

    // Legacy desktop-style constructor retained only to signal unsupported usage.

    private int takeFromBuffer(byte[] out, int off, int len) {
        int n = Math.min(len, rxCount);
        if (n <= 0) return 0;
        System.arraycopy(rxBuf, 0, out, off, n);
        int remain = rxCount - n;
        if (remain > 0) System.arraycopy(rxBuf, n, rxBuf, 0, remain);
        rxCount = remain;
        return n;
    }

    private UsbDevice pickDevice(String selector) {
        // Preserve order of insertion for index selection
        Map<String, UsbDevice> list = usbManager.getDeviceList();
        if (list.isEmpty()) return null;
        LinkedHashMap<String, UsbDevice> ordered = new LinkedHashMap<>(list); // iteration order stable
        if (selector == null || selector.isEmpty()) return ordered.values().iterator().next();
        if (selector.contains(":")) {
            String[] parts = selector.split(":");
            if (parts.length == 2) {
                try {
                    int vid = Integer.parseInt(parts[0], 16);
                    int pid = Integer.parseInt(parts[1], 16);
                    for (UsbDevice d : ordered.values()) {
                        if (d.getVendorId() == vid && d.getProductId() == pid) return d;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (selector.startsWith("index:")) {
            try {
                int idx = Integer.parseInt(selector.substring("index:".length()));
                int i = 0;
                for (UsbDevice d : ordered.values()) {
                    if (i == idx) return d;
                    i++;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        // fallback first
        return ordered.values().iterator().next();
    }

    void restartPort() throws IOException {
        try {
            if (port != null) port.close();
        } catch (Exception ignored) {
        }
        try {
            if (connection != null) connection.close();
        } catch (Exception ignored) {
        }
        this.device = pickDevice(this.selector != null ? this.selector : "");
        if (device == null) throw new IOException("No USB serial device on restart");
        if (!usbManager.hasPermission(device))
            throw new IOException("Missing USB permission on restart");
        connection = usbManager.openDevice(device);
        if (connection == null) throw new IOException("Failed to reopen USB connection");
        port = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (port == null) throw new IOException("Failed to recreate UsbSerialDevice");
        if (!port.open()) throw new IOException("Failed to open serial port (restart)");
        port.setBaudRate(115200);
        port.setDataBits(UsbSerialInterface.DATA_BITS_8);
        port.setStopBits(UsbSerialInterface.STOP_BITS_1);
        port.setParity(UsbSerialInterface.PARITY_NONE);
        port.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        // re-register async reading
        try {
            port.read(this::onReceivedData);
        } catch (Exception ignored) {
        }
    }

    void flushInput() {
        synchronized (rxLock) {
            int dropped = rxCount;
            rxCount = 0;
            if (dropped > 0) Log.d(TAG, "Flushed " + dropped + " buffered byte(s)");
        }
    }

    void writeBytes(byte[] b) throws IOException {
        Log.d(TAG, "TX  " + toHex(b));
        try {
            port.write(b);
        } catch (Exception e) {
            throw new IOException("Serial write failed", e);
        }
    }

    int blockingRead(byte[] buf, int timeoutMs) throws IOException { // visibility relaxed from private
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMs);
        synchronized (rxLock) {
            while (rxCount == 0) {
                long remain = deadline - System.currentTimeMillis();
                if (remain <= 0) return 0;
                try {
                    rxLock.wait(Math.min(100, remain));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            }
            return takeFromBuffer(buf, 0, buf.length);
        }
    }

    byte[] readExact(int len, int timeoutMs) throws IOException { // visibility relaxed from private
        if (len <= 0) return new byte[0];
        byte[] out = new byte[len];
        int off = 0;
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMs);
        synchronized (rxLock) {
            while (off < len) {
                if (rxCount == 0) {
                    long remain = deadline - System.currentTimeMillis();
                    if (remain <= 0) break;
                    try {
                        rxLock.wait(Math.min(100, remain));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                off += takeFromBuffer(out, off, len - off);
            }
        }
        if (off < len) throw new IOException("Timeout reading " + len + " bytes; got " + off);
        return out;
    }

    public boolean parseFrom(long addr, byte[] resp, int len) {
        if (resp == null || resp.length == 0) throw new IllegalArgumentException("resp vazio");
        if ((resp[0] & 0xFF) == 0x57) { // 'W'
            if (resp.length < FRAME_HEADER_LEN) throw new IllegalArgumentException("frame curto");
            this.framed = true;
            int a0 = resp[1] & 0xFF, a1 = resp[2] & 0xFF, a2 = resp[3] & 0xFF, a3 = resp[4] & 0xFF;
            this.address = (a0 << 24) | (a1 << 16) | (a2 << 8) | a3;
            if (this.address != (int) addr)
                throw new IllegalArgumentException(String.format("endereço mismatch: esperado 0x%08X, obtido 0x%08X", addr, this.address));
            if (len < 0 || len > 255)
                throw new IllegalArgumentException("tamanho inválido: " + len);
            this.size = resp[5] & 0xFF;
            if (this.size != len)
                throw new IllegalArgumentException(String.format("tamanho mismatch: esperado %d, obtido %d", len, this.size));
            int dataStart = 6;
            int minNoAckLen = dataStart + this.size + 1; // + checksum
            if (resp.length < minNoAckLen)
                throw new IllegalArgumentException("faltam dados/checksum");
            int dataEnd = dataStart + this.size;
            this.data = Arrays.copyOfRange(resp, dataStart, dataEnd);
            this.checksum = resp[dataEnd] & 0xFF;
            this.ackPresent = (resp.length >= minNoAckLen + 1) && ((resp[minNoAckLen] & 0xFF) == ACK);
            int sum = (a0 + a1 + a2 + a3 + (this.size & 0xFF)) & 0xFF;
            for (byte b : this.data) sum = (sum + (b & 0xFF)) & 0xFF;
            this.crc = sum;
            this.checksumOk = (this.crc == this.checksum);
            return this.checksumOk;
        }
        throw new IllegalArgumentException(String.format("formato inválido: primeiro byte 0x%02X não é 'W'", resp[0] & 0xFF));
    }

    @Override
    public void close() {
        try {
            if (port != null) port.close();
        } catch (Exception ignored) {
        }
        try {
            if (connection != null) connection.close();
        } catch (Exception ignored) {
        }
        if (instance == this) {
            instance = null;
        }
    }
}
