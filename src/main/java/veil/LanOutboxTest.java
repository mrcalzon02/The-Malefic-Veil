package veil;

import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Deterministic slow-output regression for first-join snapshot backpressure. */
public final class LanOutboxTest {
    private LanOutboxTest() { }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static final class SlowWriter extends Writer {
        final StringBuilder received = new StringBuilder();
        @Override public synchronized void write(char[] chars, int off, int len) {
            received.append(chars, off, len);
            for (int i = off; i < off + len; i++) if (chars[i] == '\n') {
                try { Thread.sleep(2); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
        }
        @Override public void flush() { }
        @Override public void close() { }
        synchronized String snapshot() { return received.toString(); }
    }
    public static void main(String[] args) throws Exception {
        SlowWriter slow = new SlowWriter();
        try (Socket socket = new Socket();
             LanOutbox out = new LanOutbox(new PrintWriter(slow, true), socket)) {
            // More than the 256-message bound, with a deliberately slower writer.
            // The sender must wait for space instead of losing the WELCOME or
            // late-join WEATHER frame behind the initial world inventory.
            for (int i = 0; i < 640; i++) out.sendReliable("SNAPSHOT|" + i);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (slow.snapshot().split("\\R").length < 640 && System.nanoTime() < deadline)
                Thread.sleep(10);
            String[] lines = slow.snapshot().split("\\R");
            check(lines.length == 640, "Initial snapshot must arrive without disconnect or drop");
            for (int i = 0; i < 640; i++)
                check(lines[i].equals("SNAPSHOT|" + i), "FIFO snapshot ordering at " + i);
            check(!socket.isClosed(), "Slow but draining peer must stay connected");
        }
        CountDownLatch release = new CountDownLatch(1);
        Writer blocked = new Writer() {
            @Override public void write(char[] chars, int off, int len) {
                try { release.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        try (Socket socket = new Socket();
             LanOutbox out = new LanOutbox(new PrintWriter(blocked, true), socket)) {
            for (int i = 0; i < 640; i++) out.send("TICK|" + i);
            check(socket.isClosed(), "Non-draining tick output must fail closed");
        } finally { release.countDown(); }
        System.out.println("PASS LanOutboxTest: 640 ordered snapshot frames, bounded tick overflow disconnect");
    }
}
