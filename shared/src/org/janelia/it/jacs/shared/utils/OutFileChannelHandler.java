
package org.janelia.it.jacs.shared.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class OutFileChannelHandler {
    private static final int DEFAULTBUFFERSIZE = 4096;

    private FileChannel channel;
    private ByteBuffer outBuffer;

    public OutFileChannelHandler(FileChannel channel) {
        this(channel, DEFAULTBUFFERSIZE);
    }

    public OutFileChannelHandler(FileChannel channel, int bufferSize) {
        this.channel = channel;
        outBuffer = ByteBuffer.allocateDirect(bufferSize);
        resetBuffers();
    }

    public long getWritePosition() throws IOException {
        long channelPosition = channel.position();
        if (outBuffer != null) {
            channelPosition += outBuffer.position();
        }
        return channelPosition;
    }

    public void setPosition(long position) throws IOException {
        if (outBuffer.hasRemaining()) {
            flush();
        }
        resetBuffers();
        channel.position(position);
    }

    public void close() throws IOException {
        flush();
        channel.close();
    }

    public int flush() throws IOException {
        outBuffer.flip();
        int nWritten = channel.write(outBuffer);
        outBuffer.compact();
        return nWritten;
    }

    public void writeByte(byte b) throws IOException {
        if (remainingCapacity() < 1) {
            flush();
        }
        outBuffer.put(b);
    }

    public void writeBytes(byte[] b) throws IOException {
        int bOffset = 0;
        for (; bOffset < b.length;) {
            if (remainingCapacity() == 0) {
                flush();
            }
            int nBytes;
            int remaining = remainingCapacity();
            if (b.length - bOffset > remaining) {
                nBytes = remaining;
            }
            else {
                nBytes = b.length - bOffset;
            }
            outBuffer.put(b, bOffset, nBytes);
            bOffset += nBytes;
        }
    }

    public void writeInt(int i) throws IOException {
        if (remainingCapacity() < 4) {
            flush();
        }
        outBuffer.putInt(i);
    }

    public void writeLong(long l) throws IOException {
        if (remainingCapacity() < 8) {
            flush();
        }
        outBuffer.putLong(l);
    }

    public void writeString(String s) throws IOException {
        int sLen = -1;
        if (s == null) {
            writeInt(sLen);
        }
        else {
            byte[] sBuffer = s.getBytes();
            sLen = sBuffer.length;
            // write the length of the string
            writeInt(sLen);
            // write the string bytes
            writeBytes(sBuffer);
        }
    }

    public void resetBuffers() {
        outBuffer.clear();
    }

    public int remainingCapacity() {
        return outBuffer.remaining();
    }

} // end OutFileChannelHandler
