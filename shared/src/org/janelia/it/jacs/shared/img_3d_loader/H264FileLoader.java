/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.img_3d_loader;

import java.util.List;
import org.slf4j.Logger;
import org.janelia.it.jacs.shared.ffmpeg.FFMpegLoader;
import org.janelia.it.jacs.shared.ffmpeg.ImageStack;
import org.slf4j.LoggerFactory;

/**
 * A file loader to handle H.264 files for the workstation.
 * 
 * @author fosterl
 */
public class H264FileLoader extends AbstractVolumeFileLoader {

    private Logger logger = LoggerFactory.getLogger(H264FileLoader.class);
    
    @Override
    public void loadVolumeFile(String filename) throws Exception {
        setUnCachedFileName(filename);        
        FFMpegLoader movie = new FFMpegLoader(filename);
        try {
            ByteGatherAcceptor acceptor = populateAcceptor(movie);
            captureData(acceptor);
            //dumpMeta(acceptor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveFramesAsPPM(String filename) {
        FFMpegLoader movie = new FFMpegLoader(filename);
        try {
            movie.start();
            movie.grab();
            ImageStack image = movie.getImage();
            int frames = image.get_num_frames();
            PPMFileAcceptor acceptor = new PPMFileAcceptor();
            for (int i = 0; i < frames; i++ ) {
                acceptor.setFrameNum(i);
                movie.saveFrame(i, acceptor);
            }
            movie.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unused")
    private void dumpMeta(ByteGatherAcceptor acceptor) {
        int[] freqs = new int[256];
        if (acceptor.isPopulated()) {
            System.out.println("Total bytes read is " + acceptor.getTotalSize());
            List<byte[]> bytes = acceptor.getBytes();
            // DEBUG: check byte content.
            for (byte[] nextBytes : bytes) {
                for (byte nextByte: nextBytes) {
                    int temp = nextByte;
                    if (temp < 0) {
                        temp += 256;
                    }
                    freqs[temp] ++;
                }
                System.out.print(" " + nextBytes.length);
            }
            System.out.println();
            System.out.println("Total pages is " + bytes.size());
            System.out.println("Byte Frequencies");
            for (int i = 0; i < freqs.length; i++) {
                System.out.println("Frequence of letter " + i + " is " + freqs[i]);
            }
        }
    }

    /**
     * Save the frames into an acceptor, and hand back the populated acceptor.
     * 
     * @param movie generated around an input file.
     * @return the acceptor.
     * @throws Exception 
     */
    private ByteGatherAcceptor populateAcceptor(FFMpegLoader movie) throws Exception {
        movie.start();
        movie.grab();
        ImageStack image = movie.getImage();
        int frames = image.get_num_frames();
        
        ByteGatherAcceptor acceptor = new ByteGatherAcceptor();
        for (int i = 0; i < frames; i++ ) {
            movie.saveFrame(i, acceptor);
        }
        movie.release();
        
        //dumpMeta(acceptor);
        return acceptor;
    }
    
    private void captureData(ByteGatherAcceptor acceptor) throws Exception {
        setSx( acceptor.getWidth() );
        setSy( acceptor.getHeight() );
        setSz( acceptor.getNumPages() );
        setPixelBytes(acceptor.getPixelBytes());
        long totalSize = acceptor.getTotalSize();
        if (totalSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("The input file is too large to be represented here.  Size is " + totalSize);
        }
        byte[] texByteArr = new byte[ (int)acceptor.getTotalSize() ];
        int nextPos = 0;
        for (byte[] nextBytes: acceptor.getBytes() ) {
            System.arraycopy(nextBytes, 0, texByteArr, nextPos, nextBytes.length);
            nextPos += nextBytes.length;
        }
        setTextureByteArray(texByteArr);
    }
}
