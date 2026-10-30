package veil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Per-peer FIFO for the LAN host. World ticks fail closed without blocking the
 * simulation; the peer's own socket worker may wait for room while streaming a
 * large initial world snapshot or an authoritative command response.
 */
final class LanOutbox implements AutoCloseable {
    private static final int MAX_PENDING = 256;
    private static final int MAX_LINE = 16384;
    private static final int RELIABLE_WAIT_SECONDS = 4;
    private final ArrayBlockingQueue<String> pending = new ArrayBlockingQueue<>(MAX_PENDING);
    private final Socket socket;
    private final Thread writer;
    private volatile boolean closed;

    LanOutbox(PrintWriter out, Socket socket) {
        this.socket = socket;
        writer = new Thread(() -> {
            try {
                while (!closed) {
                    String message = pending.take();
                    out.println(message);
                    if (out.checkError()) break;
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                close();
            }
        }, "veil-lan-output");
        writer.setDaemon(true);
        writer.start();
    }

    /** Nonblocking for host ticks: slow peers are disconnected, not allowed to stall the world. */
    void send(String message) { enqueue(message, false); }

    /** Backpressure for one peer's initial snapshot and direct command replies. */
    void sendReliable(String message) { enqueue(message, true); }

    private void enqueue(String message, boolean reliable) {
        if (closed) return;
        if (message == null || message.length() > MAX_LINE) { close(); return; }
        try {
            boolean queued = reliable
                ? pending.offer(message, RELIABLE_WAIT_SECONDS, TimeUnit.SECONDS)
                : pending.offer(message);
            if (!queued) close();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            close();
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        writer.interrupt();
        try { socket.close(); } catch (IOException ignored) { }
    }
}
