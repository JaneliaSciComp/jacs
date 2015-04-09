/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.img_3d_loader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.bytedeco.javacpp.BytePointer;

/**
 *
 * @author fosterl
 */
public class PPMFileAcceptor implements FFMPGByteAcceptor {
    private int frameNum;
    private int pixelBytes;
    public PPMFileAcceptor() {
    }
    
    public void setFrameNum( int frameNum ) {
        this.frameNum = frameNum;
    }
    
    @Override
    public void accept(BytePointer data, int linesize, int width, int height) {
        final String FILENAME_FORMAT = "image%05d.ppm";
        // Open file
        try (OutputStream stream = new FileOutputStream(String.format(FILENAME_FORMAT,frameNum))) {

            // Write header
            stream.write(("P6\n" + width + " " + height + "\n255\n").getBytes());

            // Write pixel data
            byte[] bytes = new byte[width * pixelBytes];
            for (int y = 0; y < height; y++) {
                data.position(y * linesize).get(bytes);
                stream.write(bytes);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
    
    @Override
    public void accept(byte[] bytes, int linesize, int width, int height) {
        final String FILENAME_FORMAT = "image%05d.ppm";
        // Open file
        try (OutputStream stream = new FileOutputStream(String.format(FILENAME_FORMAT,frameNum))) {

            // Write header
            stream.write(("P6\n" + width + " " + height + "\n255\n").getBytes());

            // Write pixel data
            for (int y = 0; y < height; y++) {
                stream.write(bytes, y * linesize, linesize);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }
    
    @Override
    public void setPixelBytes(int pixelBytes) {
        this.pixelBytes = pixelBytes;
    }
    
}
