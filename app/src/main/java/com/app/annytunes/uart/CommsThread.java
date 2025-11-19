package com.app.annytunes.uart;

import android.util.Log;

import com.app.annytunes.ui.MainActivity;
import com.app.annytunes.ui.ChannelTransferActivity;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unified communications worker: performs read+decode slabs and write buffers sequentially.
 * On start, performs the radio handshake (0x02 -> expect ID bytes). On finishing writes,
 * issues a commit command ('END').
 */
public class CommsThread extends Thread {


    // =====================================================================================
    // Types
    // =====================================================================================
    private enum Kind {READ_DECODE, WRITE, POISON, ENTER_PC_MODE, HANDSHAKE, EXIT_PC_MODE, COMMIT_WRITE, ERASE_BLOCK}

    private static final class Task {
        final Kind kind;
        final long addr;
        final byte[] data;
        final int recs;
        final int recSize;
        final CompletableFuture<Object> future;

        Task(Kind kind, long addr, byte[] data, int recs, int recSize, CompletableFuture<Object> future) {
            this.kind = kind;
            this.addr = addr;
            this.data = data;
            this.recs = recs;
            this.recSize = recSize;
            this.future = future;
        }
    }

    private static final class Result {
        final List<Channel> channels; // null -> EOS
        final Throwable err;

        Result(List<Channel> channels, Throwable err) {
            this.channels = channels;
            this.err = err;
        }
    }

    // =====================================================================================
    // Static / constants
    // =====================================================================================
    private static final String TAG = "CommsThread";
    private static CommsThread instance;

    // =====================================================================================
    // Fields
    // =====================================================================================
    private volatile AnytoneUart at;                      // attached UART
    private final BlockingQueue<Task> tasks = new LinkedBlockingQueue<>();
    private final BlockingQueue<Result> results = new LinkedBlockingQueue<>();
    private final AtomicReference<Throwable> firstError = new AtomicReference<>(null);
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final AtomicInteger outstanding = new AtomicInteger(0);
    private final Object drainLock = new Object();

    private volatile boolean accepting = true;
    private volatile boolean writesSubmitted = false;

    private int totalExpected;
    private final AtomicInteger totalSoFar = new AtomicInteger(0); // (progress not currently used)

    // Frame parsing state (moved from UART parse)
    private boolean framed;
    private int frameAddress;
    private int frameSize;
    private byte[] frameData;
    private int frameChecksum;
    private int frameCrc;
    private boolean frameChecksumOk;
    private boolean frameAckPresent;

    // Inbound reply queue (RTOS-like): chunks pushed by UART; readExact polls with timeout
    private final BlockingQueue<byte[]> inboundQueue = new LinkedBlockingQueue<>();
    private byte[] pendingChunk;
    private int pendingPos;


    // =====================================================================================
    // Construction / singleton
    // =====================================================================================

    public CommsThread() {
        super("anytone-comms");
        this.at = null;
        instance = this;
        setDaemon(true);
    }

    public static synchronized CommsThread getObj() {
        if (instance == null) {
            instance = new CommsThread();
            instance.start();
        }
        return instance;
    }

    public static synchronized void attachUart(AnytoneUart uart) {
        CommsThread ct = getObj();
        ct.at = uart;
    }

    // Inbound entry point from UART async callback
    public static void enqueueIncoming(byte[] data) {
        if (data == null || data.length == 0) return;
        CommsThread ct = getObj();
        // push into inbound queue for readExact to consume
        ct.inboundQueue.offer(data);
    }

    // =====================================================================================
    // Submission APIs
    // =====================================================================================
    public void submitWrite(long addr, byte[] data) throws InterruptedException {
        ensureAccepting();
        writesSubmitted = true;
        outstanding.incrementAndGet();
        tasks.put(new Task(Kind.WRITE, addr, data, 0, 0, null));
    }

    public void submitReadDecode(long addr, int recs, int recSize) throws InterruptedException {
        ensureAccepting();
        outstanding.incrementAndGet();
        totalExpected += recs;
        tasks.put(new Task(Kind.READ_DECODE, addr, null, recs, recSize, null));
    }

    public void submitPoison() throws InterruptedException {
        tasks.put(new Task(Kind.POISON, 0L, null, -1, -1, null));
    }

    public List<Channel> takeDecoded() throws IOException, InterruptedException {
        Result r = results.take();
        if (r.err != null) throw new IOException("CommsThread decode error", r.err);
        return r.channels; // may be null
    }

    // Synchronous protocol commands (queued)
    public void enterPcMode() throws IOException, InterruptedException {
        awaitSync(Kind.ENTER_PC_MODE);
    }

    public void handshake() throws IOException, InterruptedException {
        awaitSync(Kind.HANDSHAKE);
    }

    public void exitPcMode() throws IOException, InterruptedException {
        awaitSync(Kind.EXIT_PC_MODE);
    }


    public void commitWriteSync(long timeoutMs) throws IOException, InterruptedException {
        awaitSync(Kind.COMMIT_WRITE);
    }

    public boolean eraseBlock(long addr) throws IOException, InterruptedException {
        CompletableFuture<Object> fut = new CompletableFuture<>();
        tasks.put(new Task(Kind.ERASE_BLOCK, addr, null, 0, 0, fut));
        Object v = awaitFuture(fut);
        return v instanceof Boolean && (Boolean) v;
    }

    // =====================================================================================
    // Status / error helpers
    // =====================================================================================
    public boolean isQueueEmpty() {
        return outstanding.get() == 0;
    }

    public Throwable getFirstError() {
        return firstError.get();
    }

    // =====================================================================================
    // Finish lifecycle
    // =====================================================================================
    public void finishWritesAndJoin(long timeoutMs) throws IOException, InterruptedException {
        accepting = false;
        boolean drained = waitForDrain(timeoutMs);
        if (drained && writesSubmitted) {
            // queue commit as a task
            CompletableFuture<Object> fut = new CompletableFuture<>();
            tasks.put(new Task(Kind.COMMIT_WRITE, 0L, null, 0, 0, fut));
            try {
                awaitFuture(fut);
            } catch (IOException ioe) {
                firstError.compareAndSet(null, ioe);
            }
        } else if (writesSubmitted && !drained) {
            firstError.compareAndSet(null, new IOException("Timeout waiting for writes to drain"));
        }
        tasks.put(new Task(Kind.POISON, 0L, null, -1, -1, null));
        joinWithTimeout(timeoutMs);
        propagateFirstError();
    }

    // =====================================================================================
    // Thread execution
    // =====================================================================================
    @Override
    public void run() {


        try {
            while (true) {
                Task t = tasks.take();
                if (t.kind == Kind.POISON) {

                    // Always signal end-of-stream on results queue
                    try {
                        results.put(new Result(null, null));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                }

                inFlight.incrementAndGet();
                try {
                    switch (t.kind) {
                        case WRITE:
                            doWrite(t.addr, t.data);
                            completeIfFuture(t, Boolean.TRUE);
                            break;
                        case READ_DECODE:
                            doReadDecode(t.addr, t.recs, t.recSize);
                            completeIfFuture(t, null);
                            break;
                        case ENTER_PC_MODE:
                            doEnterPcMode();
                            completeIfFuture(t, Boolean.TRUE);
                            break;
                        case HANDSHAKE:
                            doHandshake();
                            completeIfFuture(t, Boolean.TRUE);
                            break;
                        case EXIT_PC_MODE:
                            doExitPcMode();
                            completeIfFuture(t, Boolean.TRUE);
                            break;
                        case COMMIT_WRITE:
                            doCommitWrite();
                            completeIfFuture(t, Boolean.TRUE);
                            break;
                        case ERASE_BLOCK:
                            completeIfFuture(t, doEraseBlock(t.addr));
                            break;
                        default:
                            break;
                    }
                } catch (Throwable ex) {
                    firstError.compareAndSet(null, ex);
                    completeExceptionIfFuture(t, ex);
                    break; // abort loop on first error
                } finally {
                    int nowInFlight = inFlight.decrementAndGet();
                    int left = outstanding.decrementAndGet();
                    if (left < 0) outstanding.set(0);
                    if (nowInFlight == 0 && left == 0) {
                        synchronized (drainLock) {
                            drainLock.notifyAll();
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (drainLock) {
                drainLock.notifyAll();
            }

        }
    }

    // =====================================================================================
    // Internal helpers
    // =====================================================================================
    private void ensureAccepting() {
        if (!accepting) throw new IllegalStateException("CommsThread closed for submissions");
    }

    private void completeIfFuture(Task t, Object value) {
        if (t.future != null) t.future.complete(value);
    }

    private void completeExceptionIfFuture(Task t, Throwable ex) {
        if (t.future != null) t.future.completeExceptionally(ex);
    }

    private void awaitSync(Kind kind) throws IOException, InterruptedException {
        CompletableFuture<Object> fut = new CompletableFuture<>();
        tasks.put(new Task(kind, 0L, null, 0, 0, fut));
        awaitFuture(fut);
    }

    private Object awaitFuture(CompletableFuture<Object> fut) throws IOException, InterruptedException {
        try {
            return fut.get();
        } catch (java.util.concurrent.ExecutionException ee) {
            Throwable c = ee.getCause();
            if (c instanceof IOException) throw (IOException) c;
            if (c instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw (InterruptedException) c;
            }
            throw new IOException(c);
        }
    }

    private boolean waitForDrain(long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        synchronized (drainLock) {
            while (!isQueueEmpty() && (timeoutMs <= 0 || System.currentTimeMillis() - start < timeoutMs)) {
                long remaining = (timeoutMs <= 0) ? 50L : Math.min(50L, timeoutMs - (System.currentTimeMillis() - start));
                if (remaining <= 0 && timeoutMs > 0) break;
                drainLock.wait(remaining);
            }
            return isQueueEmpty();
        }
    }

    private void joinWithTimeout(long timeoutMs) throws InterruptedException {
        if (timeoutMs > 0) join(timeoutMs);
        else join();
    }

    private void propagateFirstError() throws IOException {
        Throwable ex = getFirstError();
        if (ex != null) {
            if (ex instanceof IOException) throw (IOException) ex;
            throw new IOException("Comms thread failed", ex);
        }
    }

    private AnytoneUart ensureUart() {
        if (at == null)
            throw new IllegalStateException("AnytoneUart not yet attached to CommsThread");
        return at;
    }

    // =====================================================================================
    // Protocol implementations (moved from UART)
    // =====================================================================================
    private void doEnterPcMode() throws IOException {
        ensureUart().restartPort();
        ensureUart().writeBytes("PROGRAM".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        try {
            readExact(3, 4000);
            dispatchEnterPcMode(true, "OK");
        } catch (IOException ioe) {
            dispatchEnterPcMode(false, ioe.getMessage());
            throw ioe;
        }
    }

    private void doHandshake() throws IOException {
        ensureUart().flushInput();
        ensureUart().writeBytes(new byte[]{0x02});
        byte[] expect = new byte[]{
                (byte) 0x49, (byte) 0x44, (byte) 0x38, (byte) 0x37,
                (byte) 0x38, (byte) 0x55, (byte) 0x56, (byte) 0x00,
                (byte) 0x0E, (byte) 0x56, (byte) 0x31, (byte) 0x30,
                (byte) 0x31, (byte) 0x00, (byte) 0x00, (byte) 0x06
        };
        byte[] got = readExact(expect.length, 4000);
        if (!java.util.Arrays.equals(expect, got)) {
            throw new IOException("Handshake failed: unexpected banner");
        }
    }

    private void doExitPcMode() throws IOException {
        ensureUart().writeBytes("END".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private void doCommitWrite() throws IOException {
        ensureUart().writeBytes(new byte[]{0x45, 0x4E, 0x44}); // "END"
        waitForAck(1000);
    }

    private boolean doEraseBlock(long addr) throws IOException {
        int chunk = 64;
        ensureUart().flushInput();
        byte[] frame = new byte[AnytoneUart.FRAME_HEADER_LEN];
        frame[0] = 'E';
        frame[1] = (byte) ((addr >> 24) & 0xFF);
        frame[2] = (byte) ((addr >> 16) & 0xFF);
        frame[3] = (byte) ((addr >> 8) & 0xFF);
        frame[4] = (byte) (addr & 0xFF);
        frame[5] = (byte) (chunk & 0xFF);
        int sum = 0;
        for (int i = 1; i <= 5; i++) sum = (sum + (frame[i] & 0xFF)) & 0xFF;
        frame[6] = (byte) (sum & 0xFF);
        ensureUart().writeBytes(frame);
        return waitForAck(3000);
    }

    private boolean waitForAck(int timeoutMs) throws IOException {
        // Consume a single byte from inbound queue with timeout
        try {
            byte[] b = readExact(1, timeoutMs);
            return b.length == 1 && b[0] == AnytoneUart.ACK;
        } catch (IOException ioe) {
            return false;
        }
    }

    private byte[] readExact(int len, int timeoutMs) throws IOException {
        if (len <= 0) return new byte[0];
        byte[] out = new byte[len];
        int off = 0;
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMs);
        while (off < len) {
            // Drain pending chunk first
            if (pendingChunk != null && pendingPos < pendingChunk.length) {
                int n = Math.min(len - off, pendingChunk.length - pendingPos);
                System.arraycopy(pendingChunk, pendingPos, out, off, n);
                pendingPos += n;
                off += n;
                if (pendingPos >= pendingChunk.length) { pendingChunk = null; pendingPos = 0; }
                continue;
            }
            long remain = deadline - System.currentTimeMillis();
            if (remain <= 0) break;
            try {
                byte[] chunk = inboundQueue.poll(Math.min(100, remain), java.util.concurrent.TimeUnit.MILLISECONDS);
                if (chunk == null) continue;
                if (chunk.length == 0) continue;
                pendingChunk = chunk;
                pendingPos = 0;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (off < len) throw new IOException("Timeout reading " + len + " bytes; got " + off);
        return out;
    }

    private byte[] parseFrame(long addr, byte[] resp, int len) {
        if (resp == null || resp.length == 0) throw new IllegalArgumentException("empty resp");
        if ((resp[0] & 0xFF) != 0x57) {
            throw new IllegalArgumentException("invalid frame first byte 0x" + Integer.toHexString(resp[0] & 0xFF));
        }
        if (resp.length < AnytoneUart.FRAME_HEADER_LEN)
            throw new IllegalArgumentException("short frame");
        framed = true;
        int a0 = resp[1] & 0xFF;
        int a1 = resp[2] & 0xFF;
        int a2 = resp[3] & 0xFF;
        int a3 = resp[4] & 0xFF;
        frameAddress = (a0 << 24) | (a1 << 16) | (a2 << 8) | a3;
        if (frameAddress != (int) addr) throw new IllegalArgumentException("address mismatch");
        frameSize = resp[5] & 0xFF;
        if (frameSize != len) throw new IllegalArgumentException("size mismatch");
        int dataStart = 6;
        int minNoAck = dataStart + frameSize + 1; // + checksum
        if (resp.length < minNoAck) throw new IllegalArgumentException("missing data/checksum");
        int dataEnd = dataStart + frameSize;
        frameData = java.util.Arrays.copyOfRange(resp, dataStart, dataEnd);
        frameChecksum = resp[dataEnd] & 0xFF;
        frameAckPresent = (resp.length >= minNoAck + 1) && ((resp[minNoAck] & 0xFF) == AnytoneUart.ACK);
        int sum = (a0 + a1 + a2 + a3 + (frameSize & 0xFF)) & 0xFF;
        for (byte b : frameData) sum = (sum + (b & 0xFF)) & 0xFF;
        frameCrc = sum;
        frameChecksumOk = (frameCrc == frameChecksum);
        return frameData;
    }

    public byte[] readMem(long addr, int len) throws IOException {
        if (len < 0 || len > 255) throw new IllegalArgumentException("len inválido: " + len);
        ensureUart().flushInput();
        byte[] frame = new byte[AnytoneUart.FRAME_HEADER_LEN];
        frame[0] = 'R';
        frame[1] = (byte) ((addr >> 24) & 0xFF);
        frame[2] = (byte) ((addr >> 16) & 0xFF);
        frame[3] = (byte) ((addr >> 8) & 0xFF);
        frame[4] = (byte) (addr & 0xFF);
        frame[5] = (byte) (len & 0xFF);
        int sum = (frame[1] & 0xFF) + (frame[2] & 0xFF) + (frame[3] & 0xFF) + (frame[4] & 0xFF) + (frame[5] & 0xFF);
        frame[6] = (byte) (sum & 0xFF);
        ensureUart().writeBytes(frame);
        int expect = AnytoneUart.FRAME_HEADER_LEN + len + 1;
        byte[] resp = readExact(expect, 4000);
        if (resp.length < expect)
            throw new IOException("short read frame: got=" + resp.length + " exp>=" + expect);
        return parseFrame(addr, resp, len);
    }

    private void doWrite(long baseAddr, byte[] buf) throws IOException {
        if (buf == null) return;
        int off = 0;
        while (off < buf.length) {
            int n = Math.min(16, buf.length - off);
            byte[] part = (n == buf.length) ? buf : java.util.Arrays.copyOfRange(buf, off, off + n);
            long addr = baseAddr + off;
            if (ChannelIo.DEBUG) {
                Log.d(TAG, String.format("[comms] write frame addr=0x%08X len=%d", (int) addr, n));
            }
            boolean ack = writeFrame(addr, part);
            if (!ack)
                throw new IOException("no ACK for frame @0x" + String.format("%08X", (int) addr));
            off += n;
        }
    }

    public boolean writeFrame(long addr, byte[] buf) throws IOException {
        if (buf == null) buf = new byte[0];
        int dataLen = buf.length;
        if (dataLen > 0xFF)
            throw new IllegalArgumentException("buffer demasiado grande: " + dataLen);
        ensureUart().flushInput();
        byte[] frame = new byte[AnytoneUart.FRAME_HEADER_LEN + dataLen + 1];
        frame[0] = 'W';
        frame[1] = (byte) ((addr >> 24) & 0xFF);
        frame[2] = (byte) ((addr >> 16) & 0xFF);
        frame[3] = (byte) ((addr >> 8) & 0xFF);
        frame[4] = (byte) (addr & 0xFF);
        frame[5] = (byte) (dataLen & 0xFF);
        if (dataLen > 0) System.arraycopy(buf, 0, frame, 6, dataLen);
        int sum = 0;
        for (int i = 1; i < frame.length - 2; i++) {
            sum = (sum + (frame[i] & 0xFF)) & 0xFF;
        }
        frame[frame.length - 2] = (byte) (sum & 0xFF);
        frame[frame.length - 1] = AnytoneUart.ACK;
        ensureUart().writeBytes(frame);
        return waitForAck(5000);
    }

    private void doReadDecode(long addr, int recs, int recSize) throws IOException {
        int bytes = recs * recSize;
        byte[] slab = new byte[bytes];
        int off = 0;
        while (off < bytes) {
            int want = Math.min(0xFF, bytes - off);
            byte[] part = readMem(addr + off, want);
            if (part.length != want) throw new IOException("short read: exp=" + want + " got=" + part.length);
            System.arraycopy(part, 0, slab, off, want);
            off += want;
        }
        ChannelIo channelIo = ChannelIo.getObj();
        for (int r = 0; r < recs; r++) {
            int roff = r * recSize;
            Channel ch = channelIo.decodeChannel(slab, roff, recSize);
            // Try to notify Activity directly via getObj(); fallback to queue if unavailable
            try {
                ChannelTransferActivity act = ChannelTransferActivity.getObj();
                int soFar = totalSoFar.incrementAndGet();
                try { act.onChannelsDecoded(java.util.Collections.singletonList(ch), soFar, totalExpected); } catch (Throwable ignored) {}
            } catch (Throwable notReady) {
                try { results.put(new Result(java.util.Collections.singletonList(ch), null)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    public void commitAndKeepAlive() throws IOException, InterruptedException {
        // queue commit write task but do not poison
        CompletableFuture<Object> fut = new CompletableFuture<>();
        tasks.put(new Task(Kind.COMMIT_WRITE, 0L, null, 0, 0, fut));
        awaitFuture(fut);
    }

    private void dispatchEnterPcMode(boolean ok, String msg){
        try { ChannelTransferActivity.getObj().onEnterPcMode(ok, msg); } catch (Throwable ignored) {}
        try { MainActivity.getObj().onEnterPcMode(ok, msg); } catch (Throwable ignored) {}
    }


}
