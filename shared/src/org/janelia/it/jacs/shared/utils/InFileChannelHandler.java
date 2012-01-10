
package org.janelia.it.jacs.shared.utils;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class InFileChannelHandler implements IsSerializable, Serializable {
    protected static final int DEFAULTBUFFERSIZE = 4096;

    protected FileChannel channel;
    protected ByteBuffer inBuffer;

    public InFileChannelHandler(FileChannel channel) {
        this(channel, DEFAULTBUFFERSIZE);
    }

    public InFileChannelHandler(FileChannel channel, int bufferSize) {
        this.channel = channel;
        if (bufferSize > 0) {
            inBuffer = ByteBuffer.allocateDirect(bufferSize);
            resetBuffers();
        }
    }

    public InFileChannelHandler() {
    }

    public long getReadPosition() throws IOException {
        long channelPosition = channel.position();
        if (inBuffer != null) {
            channelPosition -= inBuffer.remaining();
        }
        return channelPosition;
    }

    public void setPosition(long position) throws IOException {
        resetBuffers();
        channel.position(position);
    }

    public void close() throws IOException {
        channel.close();
    }

    public boolean eof() throws IOException {
        return getReadPosition() >= channel.size();
    }

    public byte readByte() throws IOException {
        if (remaining() < 1) {
            if (readBuffer() < 0) {
                throw new IllegalArgumentException("Cannot read a byte from the channel");
            }
        }
        return inBuffer.get();
    }

    public int readInt() throws IOException {
        if (remaining() < 4) {
            if (readBuffer() < 0) {
                throw new IllegalArgumentException("Cannot read an int from the channel");
            }
        }
        return inBuffer.getInt();
    }

    public long readLong() throws IOException {
        if (remaining() < 8) {
            if (readBuffer() < 0) {
                throw new IllegalArgumentException("Cannot read a long from the channel");
            }
        }
        return inBuffer.getLong();
    }

    public String readString() throws IOException {
        int sLen = readInt();
        if (sLen < 0) {
            return null;
        }
        byte[] sBuffer = new byte[sLen];
        if (remaining() < sLen) {
            if (readBuffer() < 0) {
                throw new IllegalArgumentException("Cannot read a string of length " + sLen + " from the channel");
            }
        }
        inBuffer.get(sBuffer);
        return new String(sBuffer);
    }

    protected int readBuffer() throws IOException {
        inBuffer.compact();
        int nbytes = channel.read(inBuffer);
        if (nbytes >= 0) {
            inBuffer.flip();
        }
        else {
            inBuffer.limit(0);
        }
        return nbytes;
    }

    public void resetBuffers() {
        inBuffer.clear();
        inBuffer.limit(0);
    }

    public boolean hasRemaining() {
        return inBuffer != null && inBuffer.hasRemaining();
    }

    public int remaining() {
        if (inBuffer == null) {
            return 0;
        }
        else {
            return inBuffer.remaining();
        }
    }

} // end InFileChannelHandler
