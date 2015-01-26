/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.jacs.shared.img_3d_loader;

/**
 * Call this test to try opening an H.264 file.
 * @author fosterl
 */
public class TestH264 {
    public static void main(String[] args) {
        String filename = null;
        /*
        if (args.length < 1) {
            throw new IllegalArgumentException(
                "Usage: java " + TestH264.class.getName() + " <infile>"
            );
        }
        filename = args[0];
        */
        try {
            if (filename == null) {
                // Note: see also C2, C3, C4 tiles.
                filename = "/Volumes/jacs/jacsShare/H264SamplesForReview/C1-tile-2033803516857811042.v3dpbd.v3draw.avi.mp4";
            }
            H264FileLoader loader = new H264FileLoader();
            loader.saveFramesAsPPM(filename);
            loader.loadVolumeFile(filename);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }
}
