/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.passive_3d.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carries out a 3D filtering (for things like smoothing), against
 * some input byte array, which has N bytes per element.
 * 
 * NOTE: limited to byte array, which is constrained to an Integer.MAX_INT
 * divided by bytes-per-voxel, of filtered data.
 * 
 * @author fosterl
 */
public class MatrixFilter3D {
    private static final double AVG_VAL = 1.0/27.0; 
    public static double[] AVG_MATRIX_3_3_3 = new double[] {
        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,

        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,

        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,
        AVG_VAL, AVG_VAL, AVG_VAL,
    };
    
    private double[] matrix;
    private int matrixCubicDim;
    
    private static final Logger logger = LoggerFactory.getLogger( MatrixFilter3D.class );
    
    public MatrixFilter3D( double[] matrix ) {
        this.matrix = matrix;
        matrixCubicDim = (int)Math.pow( matrix.length, 1.0/3.0 );
        if ( matrixCubicDim * matrixCubicDim * matrixCubicDim != matrix.length ) {
            throw new IllegalArgumentException( "Matrix size not a square." );
        }
    }
    
    /**
     * Filter the input array using the supplied matrix.
     * 
     * @param inputBytes bytes of input data
     * @param bytesPerCell how many bytes make up the integer cell value (1..4)
     * @param sx length of x.
     * @param sy length of y.
     * @param sz length of z.
     * @return filtered version of original.
     */
    public byte[] filter( byte[] inputBytes, int bytesPerCell, int sx, int sy, int sz ) {
        byte[] outputBytes = new byte[ inputBytes.length ];
        FilteringParameter param = new FilteringParameter();
        param.setExtentX(matrixCubicDim);
        param.setExtentY(matrixCubicDim);
        param.setExtentZ(matrixCubicDim);
        param.setSx(sx);
        param.setSy(sy);
        param.setSz(sz);
        param.setVoxelBytes(bytesPerCell);
        param.setVolumeData(inputBytes);
        
        int lineSize = sx * bytesPerCell;
        int sheetSize = sy * lineSize;
        for ( int z = 0; z < sz; z++ ) {
            for ( int y = 0; y < sy; y++ ) {
                for ( int x = 0; x < sx; x++ ) {
                    long[] neighborhood = getNeighborhood( param, x, y, z );
                    long filtered = applyFilter( neighborhood );
                    byte[] value = getArrayEquiv( filtered, bytesPerCell );
                    for ( int voxByte = 0; voxByte < bytesPerCell; voxByte++ ) {
                        outputBytes[ z * sheetSize + y * lineSize + (x * bytesPerCell) + voxByte ] = value[ voxByte ];
                    }
                }
            }
        }
        return outputBytes;
    }
    
    private long applyFilter( long[] neighborhood ) {
        assert neighborhood.length == matrix.length : "Matrix and neighborhood length must match.";
        long rtnVal = 0;
        int i = 0;
        for ( int sheet = 0; sheet < matrixCubicDim; sheet++ ) {
            for ( int row = 0; row < matrixCubicDim; row++ ) {
                for ( int col = 0; col < matrixCubicDim; col++ ) {                    
                    rtnVal += matrix[ i ] * neighborhood[ i ];
                    i++;
                }
            }
        }
        return rtnVal;
    }

    /**
     * Finds the neighborhood surrounding the input point (x,y,z), as unsigned
     * integer array.  All edge cases (near end, near beginning) are handled
     * by using only partial neighborhoods which are truncated there.
     *
     * @param fparam metadata about the slice being calculated.
     * @param y input location under study.
     * @param x input location under study.
     * @param z input location under study.
     * @return computed value: all bytes of the voxel.
     */
    private long[] getNeighborhood(
            FilteringParameter fparam, int x, int y, int z
    ) {

        // Neighborhood starts at the x,y,z values of the loops.  There will be one
        // such neighborhood for each of these sets: x,y,z
        final int sz = fparam.getSz();
        final int sy = fparam.getSy();
        final int sx = fparam.getSx();
        final int extentX = fparam.getExtentX();
        final int extentY = fparam.getExtentY();
        final int extentZ = fparam.getExtentZ();
        
        long[] returnValue = new long[ extentX * extentY * extentZ ];
        int startX = neighborhoodStart(x, extentX);
        int startY = neighborhoodStart(y, extentY);
        int startZ = neighborhoodStart(z, extentZ);
        for ( int zNbh = startZ; zNbh < startZ + extentZ && zNbh < sz; zNbh ++ ) {
            if (zNbh < 0) {// Edge case: at beginning->partial neighborhood.
                continue;
            }
            long nbhZOffset = (sy * sx * zNbh) * fparam.getVoxelBytes();

            for ( int yNbh = startY; yNbh < startY + extentY && yNbh < sy; yNbh ++ ) {
                if (yNbh < 0) {// Edge case: at beginning->partial neighborhood.
                    continue;
                }
                long nbhYOffset = nbhZOffset + (sx * yNbh * fparam.getVoxelBytes() );

                for ( int xNbh = startX; xNbh < startX + extentX && xNbh < sx; xNbh++ ) {
                    if (xNbh < 0) {// Edge case: at beginning->partial neighborhood.
                        continue;
                    }
                    byte[] voxelVal = new byte[ fparam.getVoxelBytes() ];
                    long arrayCopyLoc = nbhYOffset + (xNbh * fparam.getVoxelBytes());
                    try {
                        byte[] volume = fparam.getVolume();
                        for ( int i = 0; i < (fparam.getVoxelBytes() ); i++ ) {
                            voxelVal[ i ] = volume[ (int)(i + arrayCopyLoc) ];
                        }
                    } catch ( Exception ex ) {
                        logger.error(
                                "Exception while trying to copy to {} with max of {}.",
                                arrayCopyLoc,
                                fparam.getVolume().length
                        );
                        logger.info( "Expected dimensions are " + sx + " x " + sy + " x " + sz );
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }

                    if ( isZero( voxelVal ) ) {
                        continue;  // Highest freq non-zero is kept.
                    }
                    long equivalentValue = getIntEquiv( voxelVal );
                    final int outputOffset = (zNbh-startZ)*(extentY*extentX) + (yNbh-startY) * extentX + (xNbh-startX);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Output location for " + xNbh + "," + yNbh + "," + zNbh + " is " + outputOffset);
                    }
                    if (outputOffset > returnValue.length) {
                        logger.error("Out of bounds.");
                    }
                    returnValue[ outputOffset ] = equivalentValue;
                }
            }
        }
        return returnValue;
    }
    
    /** 
     * Start of neighborhood for a given coordinate should be minus half
     * its extent. 
     */
    private int neighborhoodStart( int coord, int extent ) {
        return coord - (extent/2);
    }
    
    /**
     * Convert the voxel value into an integer.  Assumes 4 or fewer bytes per voxel.
     * Returning the "unsigned integer" equivalent of the array of bytes.
     * Must use long to avoid use of sign bits.
     *
     * @param voxelValue input bytes
     * @return integer conversion, based on LSB
     */
    private long getIntEquiv( byte[] voxelValue ) {
        long rtnVal = 0;
        int arrLen = voxelValue.length;
        for ( int i = 0; i < arrLen; i++ ) {
            //finalVal += startingArray[ j ] << (8 * (arrLen - j - 1));
            rtnVal += (voxelValue[ i ] << (8 * (arrLen - i - 1)));
        }
        return rtnVal;
    }
    
    private byte[] getArrayEquiv( long voxelValue, int arrLen ) {
        byte[] rtnVal = new byte[ arrLen ];
        for (int i = 0; i < arrLen; i++) {
            rtnVal[ i ] = 
            (byte)(
                    (voxelValue >> (8 * (arrLen - i - 1)))
                    & 0xFF
            );
        }
        return rtnVal;
    }
    
    private boolean isZero( byte[] bytes ) {
        for (byte aByte : bytes) {
            if (aByte != (byte) 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Bean of information used to drive the filtering.
     */
    public static class FilteringParameter {
        // Size of the entire volume, in all three dimensions.
        private int sx;
        private int sy;
        private int sz;
        
        // Size of a neighborhood, in all three dimensions.
        private int extentX;
        private int extentY;
        private int extentZ;
        
        // How many bytes make one voxel?
        private int voxelBytes;
        
        private byte[] volumeData;

        /**
         * @return the sx
         */
        public int getSx() {
            return sx;
        }

        /**
         * @param sx the sx to set
         */
        public void setSx(int sx) {
            this.sx = sx;
        }

        /**
         * @return the sy
         */
        public int getSy() {
            return sy;
        }

        /**
         * @param sy the sy to set
         */
        public void setSy(int sy) {
            this.sy = sy;
        }

        /**
         * @return the sz
         */
        public int getSz() {
            return sz;
        }

        /**
         * @param sz the sz to set
         */
        public void setSz(int sz) {
            this.sz = sz;
        }
        
        public byte[] getVolume() {
            return volumeData;
        }
        
        public void setVolumeData( byte[] volumeData ) {
            this.volumeData = volumeData;
        }

        /**
         * @return the extentX
         */
        public int getExtentX() {
            return extentX;
        }

        /**
         * @param extentX the extentX to set
         */
        public void setExtentX(int extentX) {
            this.extentX = extentX;
        }

        /**
         * @return the extentY
         */
        public int getExtentY() {
            return extentY;
        }

        /**
         * @param extentY the extentY to set
         */
        public void setExtentY(int extentY) {
            this.extentY = extentY;
        }

        /**
         * @return the extentZ
         */
        public int getExtentZ() {
            return extentZ;
        }

        /**
         * @param extentZ the extentZ to set
         */
        public void setExtentZ(int extentZ) {
            this.extentZ = extentZ;
        }

        /**
         * @return the voxelBytes
         */
        public int getVoxelBytes() {
            return voxelBytes;
        }

        /**
         * @param voxelBytes the voxelBytes to set
         */
        public void setVoxelBytes(int voxelBytes) {
            this.voxelBytes = voxelBytes;
        }
    }

}
