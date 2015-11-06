/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This enwraps all data related to the transform file, which should exist
 * in the folder given in the constructor.
 *
 * @author fosterl
 */
public class TransformFileObject {
    public static final String TRANSFORM_FILE = "transform.txt";
    private File topFolder;
    private final double zoomLevelCount;
    
    private int[] origin;
    private double[] scale;
    
    /**
     * Builds object to encapsulate the input file.
     * 
     * @param topFolder should contain a file of interest: transform.txt
     * @param zoomLevelCount as from tileFormat.getZoomLevelCount()
     */
    public TransformFileObject(File topFolder, double zoomLevelCount) {
        this.topFolder = topFolder;
        this.zoomLevelCount = zoomLevelCount;
        
        populate();
    }
    
    private boolean populate() {
        File transformFile = new File(getTopFolder(), "transform.txt");
        if (!transformFile.exists()) {
            return false;
        }
        try {
            Pattern pattern = Pattern.compile("([os][xyz]): (\\d+(\\.\\d+)?)");
            Scanner scanner = new Scanner(transformFile);
            double scaleScale = 1.0 / Math.pow(2, zoomLevelCount - 1);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    String key = m.group(1);
                    String value = m.group(2);
                    switch (key) {
                        case "ox":
                            origin[0] = Integer.parseInt(value);
                            break;
                        case "oy":
                            origin[1] = Integer.parseInt(value);
                            break;
                        case "oz":
                            origin[2] = Integer.parseInt(value);
                            break;
                        case "sx":
                            scale[0] = Double.parseDouble(value) * scaleScale;
                            break;
                        case "sy":
                            scale[1] = Double.parseDouble(value) * scaleScale;
                            break;
                        case "sz":
                            scale[2] = Double.parseDouble(value) * scaleScale;
                            break;
                        default:
                            break;
                    }
                }
            }
            // TODO 
            return true;
        } catch (FileNotFoundException ex) {
        }

        // Do a post conversion to microns.
        OriginScale adjustedOS = adjustOriScaleToMicrons(origin, scale);
        origin = adjustedOS.origin;
        scale = adjustedOS.scale;

        return false;
    }

    public static OriginScale adjustOriScaleToMicrons( int[] origin, double[] scale ) {
        // Raw scale must be converted to micrometers.
        for (int i = 0; i < scale.length; i++) {
            scale[ i] /= 1000; // nanometers to micrometers
        }
        // Origin must be divided by 1000, to convert to micrometers.
        for (int i = 0; i < origin.length; i++) {
            origin[ i] = (int) (origin[i] / (1000 * scale[i])); // nanometers to voxels
        }
        OriginScale rtnVal = new OriginScale();
        rtnVal.origin = origin;
        rtnVal.scale = scale;
        return rtnVal;
    }
    
    /**
     * @return the topFolder
     */
    public File getTopFolder() {
        return topFolder;
    }

    /**
     * @param topFolder the topFolder to set
     */
    public void setTopFolder(File topFolder) {
        this.topFolder = topFolder;
    }

    /**
     * @return the origin
     */
    public int[] getOrigin() {
        return origin;
    }

    /**
     * @return the scale
     */
    public double[] getScale() {
        return scale;
    }
    
    public static class OriginScale {
        public int[] origin;
        public double[] scale;
    }
}
