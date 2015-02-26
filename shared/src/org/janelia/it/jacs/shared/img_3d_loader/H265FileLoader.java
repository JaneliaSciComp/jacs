/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.img_3d_loader;

import java.util.List;
import org.slf4j.Logger;
import org.janelia.it.jacs.shared.ffmpeg.H5JLoader;
import org.janelia.it.jacs.shared.ffmpeg.ImageStack;
import org.slf4j.LoggerFactory;

/**
 * A file loader to handle H.265 files for the workstation.
 * 
 * @author fosterl
 */
public class H265FileLoader extends AbstractVolumeFileLoader {

    private final Logger logger = LoggerFactory.getLogger(H265FileLoader.class);
    
    private final H26nFileLoadHelper helper = new H26nFileLoadHelper();
    
    @Override
    public void loadVolumeFile(String filename) throws Exception {
        setUnCachedFileName(filename);   
        H5JLoader reader = new H5JLoader(filename);
        try {
            ByteGatherAcceptor acceptor = gatherBytes(reader);
            helper.captureData(acceptor, this);
            //dumpMeta(acceptor);
        } catch (Exception e) {
            e.printStackTrace();
        }
        reader.close();
    }
    
    public void saveFramesAsPPM(String filename) {
        H5JLoader reader = new H5JLoader(filename);
        FFMPGByteAcceptor acceptor = new PPMFileAcceptor();
        accept(reader, acceptor);
    }

    /**
     * Save the frames into an acceptor, and hand back the populated acceptor.
     * 
     * @param reader generated around an input file.
     * @return the acceptor.
     * @throws Exception 
     */
    private ByteGatherAcceptor gatherBytes(H5JLoader reader) throws Exception {
        ByteGatherAcceptor acceptor = new ByteGatherAcceptor();
        accept(reader, acceptor);
        //helper.dumpMeta(acceptor);
        return acceptor;
    }
    
    private void accept(H5JLoader reader, FFMPGByteAcceptor acceptor) {
        List<String> channels = reader.channelNames();
        try {
            for (String channelId : channels) {                
                ImageStack image = reader.extract(channelId);
                acceptor.setPixelBytes(image.getBytesPerPixel());
                int frameCount = image.getNumFrames();
                for (int i = 0; i < frameCount; i++) {
                    logger.debug("Saving frame " + i + " of channel " + channelId);
                    acceptor.setFrameNum(i);                    
                    reader.saveFrame(i, acceptor);
                }
//                image.release();
            }            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
