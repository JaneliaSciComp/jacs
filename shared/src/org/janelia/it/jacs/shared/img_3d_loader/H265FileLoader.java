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
            ByteGatherAcceptor acceptor = populateAcceptor(reader);
            helper.captureData(acceptor, this);
            //dumpMeta(acceptor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Save the frames into an acceptor, and hand back the populated acceptor.
     * 
     * @param reader generated around an input file.
     * @return the acceptor.
     * @throws Exception 
     */
    private ByteGatherAcceptor populateAcceptor(H5JLoader reader) throws Exception {
        List<String> channels = reader.channelNames();
        ByteGatherAcceptor acceptor = new ByteGatherAcceptor();
        acceptor.setPixelBytes(1);
        for (String channelId: channels) {
            try {
                ImageStack image = reader.extract(channelId);
                System.out.println("Version 10");
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException ie) {
//                    ie.printStackTrace();
//                }
                System.out.println(channelId + " has " + image.get_num_frames() + " frames, and " + image.get_bytes_per_pixel() + " bytes per pixel.");
                acceptor.setPixelBytes(image.get_bytes_per_pixel());
                int frameCount = image.get_num_frames();
                for (int i = 0; i < frameCount; i++) {
                    System.out.println("Saving frame " + i);
                    reader.saveFrame(i, acceptor);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //helper.dumpMeta(acceptor);
        return acceptor;
    }
    
}
