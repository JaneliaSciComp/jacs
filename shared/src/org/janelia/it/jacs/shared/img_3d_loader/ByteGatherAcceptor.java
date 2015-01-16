/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.img_3d_loader;

import java.util.ArrayList;
import java.util.List;
import org.bytedeco.javacpp.BytePointer;

/**
 * This acceptor will keep all bytes in memory.
 * @author fosterl
 */
public class ByteGatherAcceptor implements FFMPGByteAcceptor {

    private List<byte[]> pages = new ArrayList<>();
    private int pagePos = 0;
    private long totalSize = 0;
            
    /**
     * Accept one "page" of data, of width x height given, and with linesize
     * bytes on each line of the page.
     * 
     * @param data pointer to grab the data.
     * @param linesize 
     * @param width
     * @param height 
     */
    @Override
    public void accept(BytePointer data, int linesize, int width, int height) {
        pagePos = 0;  //Nota Bene: had forgotten to do this, and the error
                      // that manifested itself resembled a JNI error.
        // Write pixel data
        final int pagesize = width * 3 * height;
        byte[] bytes = new byte[pagesize];
        for (int y = 0; y < height; y++) {
            data.position(y * linesize).get(bytes, pagePos, linesize);
            pagePos += linesize;
        }
        totalSize += bytes.length;
        pages.add( bytes );
    }
    
    public boolean isPopulated() {
        return pagePos > 0;
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
    
    public long getTotalSize() {
        return totalSize;
    }

}
