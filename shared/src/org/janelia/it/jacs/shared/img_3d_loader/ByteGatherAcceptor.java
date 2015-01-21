/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.img_3d_loader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bytedeco.javacpp.BytePointer;

/**
 * This acceptor will keep all bytes in memory.
 * @author fosterl
 */
public class ByteGatherAcceptor implements FFMPGByteAcceptor {

    private static final int N_THREADS = 32;
    private List<byte[]> pages = new ArrayList<>();
    private long totalSize = 0;
    private int width;
    private int height;
    private int pixelBytes;
            
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
        final int elementWidth = width * getPixelBytes();
        setWidth(width);
        setHeight(height);
        // Write pixel data
        final int pagesize = elementWidth * height;        
        final byte[] bytes = new byte[pagesize];
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
        for (int y = 0; y < height; y++) {
            final int fY = y;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int lineInPagePos = fY * linesize;
                    data.position(lineInPagePos).get(bytes, lineInPagePos, linesize);
                }
            };
            executorService.submit(runnable);
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch ( InterruptedException ie ) {
            throw new RuntimeException(ie);
        }
        
        totalSize += bytes.length;
        pages.add( bytes );
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

    /**
     * @param pixelBytes the pixelBytes to set
     */
    public void setPixelBytes(int pixelBytes) {
        this.pixelBytes = pixelBytes;
    }

}
