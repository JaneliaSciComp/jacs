package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import java.io.*;
import org.apache.log4j.Logger;

/**
 * Given a base location for "rendered" tiffs, this class can transform an incoming Large Volume Viewer
 * coordinate into a microscope-scaled coordinate.
 * Created by fosterl on 9/26/14.
 */
public class CoordinateToRawTransform implements Serializable {
    private enum TransformParseType { origin, scale, numlevels, all }

    public static final String TRANSFORM_FILE = "transform.txt";
    private static final int MAX_DIGIT_HIERARCHY_LEVEL = 6;

    private int[] origin = new int[3];
    private double[] scale = new double[3];
    private int numLevels = -1;
    private int relativeDrillDepth = 0;
    private static Logger log = Logger.getLogger(CoordinateToRawTransform.class);

    public CoordinateToRawTransform(File baseLocation) {
        init( baseLocation );
    }

    /**
     * Return the coordinates relative to the microscope stage, that correspond to the screen/Large Volume Viewer
     * coordinate given.
     *
     * @param lvvScreenCoordinate presented to user while working with LVV.
     * @return nanometer-scale location in 3D.
     */
    public int[] getMicroscopeCoordinate( int[] lvvScreenCoordinate ) {
        int[] rtnval = new int[3];
        for ( int i = 0; i < lvvScreenCoordinate.length; i++ ) {
            rtnval[ i ] = origin[ i ] + (int)(lvvScreenCoordinate[ i ] * scale[ i ]);
        }
        return rtnval;
    }

    /**
     * Returns origin in nanometers.
     * 
     * @return the origin
     */
    public int[] getOrigin() {
        return origin;
    }

    /**
     * Returns scale in nanometers.
     * @return the scale
     */
    public double[] getScale() {
        return scale;
    }

    /**
     * Recursively drill down from the starting, parent file, looking for directories named for single
     * digits (part of an octree), to find the/a bottom-most level.  Must avoid "local maxima" of depth,
     * because certain directories may in fact NOT have the finest-grained images associated with them.
     *
     * @param parentFile currently-explored directory.
     * @return chain-reaction from bottom-most directory.
     */
    private File digitDrilldown( File parentFile ) {
        relativeDrillDepth ++;
        DigitDirFileFilter filter = new DigitDirFileFilter();
        File[] digitSubDirs = parentFile.listFiles(filter);
        if ( digitSubDirs == null ) {
            if ( relativeDrillDepth >= MAX_DIGIT_HIERARCHY_LEVEL ) {
                return parentFile;
            }
            else {
                return null;
            }
        }
        for ( File digitSubDir: digitSubDirs ) {
            if ( digitSubDir.listFiles( filter ).length == 0 ) {
                // Directories can have no digit-directory children, but have siblings that go deeper.
                if ( relativeDrillDepth < MAX_DIGIT_HIERARCHY_LEVEL ) {
                    continue;
                }
                return digitSubDir;
            }
            else {
                return digitDrilldown( digitSubDir );
            }
        }
        return null;
    }

    /**
     * Initializer reads the transform.txt file.
     *
     * Example input file format:
     * ox: 74564008
     * oy: 44229473
     * oz: 21551260
     * sx: 300.279503
     * sy: 300.241237
     * sz: 1001.480000
     */
    private void init( File baseLocation ) {
        // Parse a top-level directory for the origin.
        File[] transformTexts = baseLocation.listFiles(new TransformTextFilter());
        parseTransform(transformTexts[0], TransformParseType.all);
        if ( numLevels > 0 ) {
            double divisor = Math.pow(2.0, numLevels - 1);
            for (int i = 0; i < getScale().length; i++) {
                getScale()[i] /= divisor;
            }
            if (log.isDebugEnabled()) {
                log.debug("Dividing scale by numlevels " + numLevels);
            }
        }
        else {
            File bottomLevelDigitDir = digitDrilldown(baseLocation);
            if (bottomLevelDigitDir == null) {
                throw new IllegalArgumentException("Unable to find a suitable " + TRANSFORM_FILE + " file for transform parameters.");
            }
            // Parse a bottom-level directory for the scale.
            transformTexts = bottomLevelDigitDir.listFiles(new TransformTextFilter());
            parseTransform(transformTexts[0], TransformParseType.scale);
        }

    }

    private void parseTransform(File transformText, TransformParseType parseType) {
        int setterMask = 0;
        try (BufferedReader br = new BufferedReader( new FileReader(transformText) )) {

            String inline = null;
            while ( null != ( inline = br.readLine() ) ) {
                if ( inline.startsWith( "#" ) ) {
                    continue;
                }
                else {
                    int maskValue = 1;
                    String[] tagValue = inline.trim().split(":");
                    if ( tagValue.length == 2 ) {
                        String value = tagValue[1].trim();

                        String tag = tagValue[0].trim().toLowerCase();
                        char secondPos = tag.charAt( 1 );
                        int index = 0;
                        switch (secondPos) {
                            case 'x':
                                maskValue = 1;
                                index = 0;
                                break;
                            case 'y':
                                maskValue = 2;
                                index = 1;
                                break;
                            case 'z':
                                maskValue = 4;
                                index = 2;
                                break;
                            default:
                                break;
                        }
                        // Either origin, scale, or number-of-levels.
                        char type = tag.charAt( 0 );
                        if ( type == 'o'  &&  (parseType == TransformParseType.origin  ||  parseType == TransformParseType.all) ) {
                            getOrigin()[ index ] = Integer.parseInt( value );
                        }
                        else if ( type == 's' ) {
                            if ( parseType == TransformParseType.scale  ||  parseType == TransformParseType.all ) {
                                getScale()[ index ] = Double.parseDouble( value );
                            }
                            maskValue <<= 3;
                        }
                        else if ( tag.equalsIgnoreCase("nl")  &&  (parseType == TransformParseType.numlevels  ||  parseType == TransformParseType.all) ) {
                            // This is an optional field. Not included in
                            // the mask-of-set-values.
                            numLevels = Integer.parseInt( value );
                        }
                        setterMask |= maskValue;
                    }
                    else {
                        continue;
                    }
                }
            }

            // Now, will have set everything.
            if ( setterMask != 0b111111 ) {
                throw new IllegalArgumentException( "Did not find all required origin and scale parameters from  " + transformText);
            }
        } catch ( IOException ioe ) {
            throw new RuntimeException( ioe );
        } catch ( Exception any ) {
            any.printStackTrace();
            throw any;
        }
        
    }

    private static class DigitDirFileFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory()  &&  file.getName().length() == 1  &&  Character.isDigit( file.getName().charAt( 0 ) );
        }
    }

    private static class TransformTextFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isFile() && file.getName().equals( TRANSFORM_FILE );
        }
    }

}


