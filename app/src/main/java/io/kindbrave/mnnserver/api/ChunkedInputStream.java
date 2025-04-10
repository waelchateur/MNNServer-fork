package io.kindbrave.mnnserver.api;

import java.io.IOException;
import java.io.PipedInputStream;

public class ChunkedInputStream extends PipedInputStream {

    private int chunk = 0;

    String[] chunks;

    public ChunkedInputStream(String[] chunks) {
        this.chunks = chunks;
    }

    @Override
    public synchronized int read(byte[] buffer, int off, int len) throws IOException {
        // Too implementation-linked, but...
        for (int i = 0; i < this.chunks[this.chunk].length(); ++i) {
            buffer[i] = (byte) this.chunks[this.chunk].charAt(i);
        }
        return this.chunks[this.chunk++].length();
    }
}