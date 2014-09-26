package org.janelia.it.jacs.compute.largevolume;

import java.io.*;

/**
 * Created by fosterl on 9/26/14.
 */
public class TransformParameters {
    private static final String TRANSFORM_FILE = "transform.txt";
    private static final int MAX_DIGIT_HIERARCHY_LEVEL = 6;

    private int[] origin = new int[3];
    private double[] scale = new double[3];
    private int relativeDrillDepth = 0;

    public TransformParameters( File baseLocation ) {
        init( baseLocation );
    }

    /**
     * @return the origin
     */
    public int[] getOrigin() {
        return origin;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigin(int[] origin) {
        this.origin = origin;
    }

    /**
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
        File bottomLevelDigitDir = digitDrilldown( baseLocation );
        if ( bottomLevelDigitDir == null ) {
            throw new IllegalArgumentException( "Unable to find a suitable " + TRANSFORM_FILE + " file for transform parameters." );
        }
        File[] transformTexts = bottomLevelDigitDir.listFiles(new TransformTextFilter());
        int setterMask = 0;  // ox=1, sx=2; oy=4, sy=8; oz=16, sz=32
        try (BufferedReader br = new BufferedReader( new FileReader( transformTexts[ 0 ] ) )) {

            String inline = null;
            while ( null != ( inline = br.readLine() ) ) {
                if ( inline.startsWith( "#" ) ) {
                    continue;
                }
                else {
                    int maskValue = 1;
                    String[] tagValue = inline.trim().split(":");
                    if ( tagValue.length == 2 ) {
                        String tag = tagValue[0].trim().toLowerCase();
                        String value = tagValue[1].trim();

                        char coord = tag.charAt( 1 );
                        int index = 0;
                        switch (coord) {
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
                                throw new IllegalArgumentException("Unexpected value while reading scale/origin parameters.");
                        }
                        // Either origin or scale.
                        char type = tag.charAt( 0 );
                        if ( type == 'o' ) {
                            getOrigin()[ index ] = Integer.parseInt( value );
                        }
                        else {
                            getScale()[ index ] = Double.parseDouble( value );
                            maskValue <<= 3;
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
                throw new IllegalArgumentException( "Did not find all required origin and scale parameters from  " + transformTexts[ 0 ] );
            }

        } catch ( IOException ioe ) {
            throw new RuntimeException( ioe );
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


