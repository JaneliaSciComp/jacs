/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.img_3d_loader;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.bytedeco.javacpp.BytePointer;

/**
 * This acceptor will keep all bytes in memory.
 * @author fosterl
 */
public class ByteGatherAcceptor implements FFMPGByteAcceptor {

//    private byte[] bytes;
    private List<byte[]> pages = new ArrayList<>();
    private long totalSize = 0;
    private int width;
    private int height;
    private int pixelBytes;
    private int frameNum;
    private Logger logger = Logger.getLogger(ByteGatherAcceptor.class);
            
    public ByteGatherAcceptor() {
        setPixelBytes(3);
    }
    
    /**
     * Accept one "page" of data, of width x height given, and with linesize
     * bytes on each line of the page.
     * 
     * @param data pointer to grab the data.
     * @param linesize how long is a line (with multiplier justification).
     * @param width how wide is a line in elements.
     * @param height number of lines in the page.
     */
    @Override
    public void accept(final BytePointer data, final int linesize, final int width, int height) {
        setWidth(width);
        setHeight(height);
        // Write pixel data
        byte[] page = new byte[linesize * height];
        pagewisePageCapture(width, height, linesize, data, page);
        
//        byte[] bytes = data.getStringBytes();
        totalSize += page.length;
        pages.add( page );
    }

    @Override
    public void setFrameNum(int frameNum) {
        this.frameNum = frameNum;
    }
    
    /**
     * @param pixelBytes the pixelBytes to set
     */
    @Override
    public void setPixelBytes(int pixelBytes) {
        this.pixelBytes = pixelBytes;
    }

    public boolean isPopulated() {
        return pages.size() > 0;
    }
    
    /**
     * Fetch all pages of data.
     * @return the accumulated collection.
     */
    public List<byte[]> getBytes() {
        if (! isPopulated()) {
            throw new IllegalStateException("Must first accept some bytes.");
        }
        return pages;
    }
    
    public int getNumPages() {
        return getBytes().size();
    }
    
    public long getTotalSize() {
        return totalSize;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        if (this.width > 0 && this.width != width) {
            String message = String.format("Width %d differs from accepted width of %d", width, this.width);
            throw new IllegalStateException(message);
        }
        this.width = width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        if (this.height > 0 && this.height != height) {
            String message = String.format("Height %d differs from accepted height of %d", height, this.height);
            throw new IllegalStateException(message);
        }
        this.height = height;
    }

    /**
     * @return the pixelBytes
     */
    public int getPixelBytes() {
        return pixelBytes;
    }

    private void linewisePageCapture(final int width, int height, final int linesize, final BytePointer data, byte[] page) {
        byte[] bytes = new byte[width * pixelBytes];
        for (int y = 0; y < height; y++) {
            final int lineInPagePos = y * linesize;
            BytePointer ptr = data.position(lineInPagePos);
            ptr.get(bytes);
            System.arraycopy(bytes, 0, page, y * linesize, bytes.length);
        }
    }

    private void pagewisePageCapture(final int width, int height, final int linesize, final BytePointer data, byte[] page) {
        BytePointer ptr = data.position(0);
        //ptr.get(page);
    }
    
}
