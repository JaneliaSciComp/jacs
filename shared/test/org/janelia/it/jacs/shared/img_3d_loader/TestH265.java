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
public class TestH265 {
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
                filename = "/Volumes/jacs/jacsShare/H265SamplesForReview/ffmpeg_hdf/test_output.h5j";
            }
            H265FileLoader loader = new H265FileLoader();
//            loader.saveFramesAsPPM(filename);
            loader.loadVolumeFile(filename);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }
}
