package org.janelia.it.jacs.compute.largevolume;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.largevolume.model.TileBase;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;

import Jama.Matrix;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fosterl on 9/26/14.
 */
public class RawFileFetcher {

    public static final String TIFF_0_SUFFIX = "-ngc.0.tif";
    public static final String TIFF_1_SUFFIX = "-ngc.1.tif";

    private static final double DISTANCE_DOWN_SCALER = 1000000.0;
    private Map<List<Integer>, RawDataHandle> centroidToRawDataHandle;
    private CoordinateToRawTransform transform;
    private Logger logger = Logger.getLogger(RawFileFetcher.class);
    private final static Map<String,RawFileFetcher> fetcherMap = new HashMap<>();
    
    public static RawFileFetcher getRawFileFetcher( String basePath ) throws Exception {
        RawFileFetcher fetcher = fetcherMap.get( basePath );
        if ( fetcher == null ) {
            fetcher = new RawFileFetcher( basePath );
            fetcherMap.put( basePath, fetcher );
        }
        return fetcher;
    }

    private RawFileFetcher(String basePath) throws Exception {
        File renderedBaseDirectory = new File( basePath );
        TileBase tileBase = getTileBase(renderedBaseDirectory);
        centroidToRawDataHandle = new TileWalker( tileBase ).getCentroidToRawData();
        transform = new CoordinateToRawTransform( renderedBaseDirectory );
    }

    //private static Map<String,double[][]> baseToInverse = new HashMap<>();
    
    /**
     * Returned object has files representing the point as two channels of Tiff data, plus
     * metadata required to use them.
     *
     * @param lvvCoords from large volume viewer.
     * @return info sufficient to read raw data.
     * @throws Exception from called methods.
     */
    public RawFileInfo getNearestFileInfo(int[] lvvCoords) throws Exception {
        int[] microScopeCoords = transform.getMicroscopeCoordinate( lvvCoords );
        List<Integer> closestCentroid = getClosestCentroid(microScopeCoords);
        RawDataHandle handle = centroidToRawDataHandle.get( closestCentroid );
        File rawFileDir = new File( handle.getBasePath() + handle.getRelativePath() );
        if ( ! rawFileDir.exists()  ||  ! rawFileDir.isDirectory() ) {
            logger.error( "Failed to open microscope files directory " + rawFileDir );
        }

        RawFileInfo rawFileInfo = new RawFileInfo();
        rawFileInfo.setCentroid( closestCentroid );
        rawFileInfo.setChannel0( new File( rawFileDir, rawFileDir.getName() + TIFF_0_SUFFIX) );
        rawFileInfo.setChannel1( new File( rawFileDir, rawFileDir.getName() + TIFF_1_SUFFIX) );

        rawFileInfo.setTransformMatrix( this.getSquaredMatrix(handle.getTransformMatrix()) );
        /*
         Caching of this is broken: not sufficient delineation of keys.
        double[][] invertedTransform = baseToInverse.get( handle.getBasePath() );
        if ( invertedTransform == null ) {
            invertedTransform = getInvertedTransform( rawFileInfo.getTransformMatrix() );
            baseToInverse.put( handle.getBasePath(), invertedTransform );
        }
        */
        double[][] invertedTransform = getInvertedTransform( rawFileInfo.getTransformMatrix() );
        rawFileInfo.setInvertedTransform( invertedTransform );
        
        rawFileInfo.setMinCorner( convertToPrimArray( handle.getMinCorner() ) );
        rawFileInfo.setExtent( convertToPrimArray( handle.getExtent() ) );
        List<Integer> queryMicroscopeCoords = new ArrayList<>();
        for ( int coord: microScopeCoords ) {
            queryMicroscopeCoords.add( coord );
        }
        rawFileInfo.setQueryMicroscopeCoords( queryMicroscopeCoords );
        rawFileInfo.setQueryViewCoords( lvvCoords );
        rawFileInfo.setScale( transform.getScale() );
        if ( rawFileInfo.getChannel0() == null  ||  !rawFileInfo.getChannel0().exists() ) {
            logger.error("Failed to find channel 0 tiff file in " + rawFileDir + ".");
        }
        if ( rawFileInfo.getChannel1() == null  ||  !rawFileInfo.getChannel1().exists() ) {
            logger.error("Failed to find channel 1 tiff file in " + rawFileDir + ".");
        }

        return rawFileInfo;
    }

    /**
     * Given microscope/stage coordinates, return the closest centroid from the centroid set from the tilebase.
     *
     * @param microScopeCoords some point expressed in the overall stage space.
     * @return centroid for some raw tiff, in which that point falls.
     */
    public List<Integer> getClosestCentroid(int[] microScopeCoords) {
        List<Integer> closestCentroid = null;
        double squareOfClosestDistance = (double)Float.MAX_VALUE;
        for ( List<Integer> centroid: centroidToRawDataHandle.keySet() ) {
            if ( closestCentroid == null ) {
                closestCentroid = centroid;
            }
            else {
                double squareCentroidDistance = getCentroidDistanceMetric(centroid, microScopeCoords);
                if ( squareOfClosestDistance > squareCentroidDistance ) {
                    closestCentroid = centroid;
                    squareOfClosestDistance = squareCentroidDistance;
                }
            }
        }
        return closestCentroid;
    }

    /**
     * Note: do not need actual distance, only to know what is the smallest 'distance'.  Therefore,
     * not bothering to take square root, which would incur more overhead.  However, do need to scale all the
     * values by some large number to prevent double overflow.  Distances are significant enough to cause that
     * if squared.
     *
     * @param centroid how close _to
     * @param coords how close _is
     * @return square of distance
     */
    public double getCentroidDistanceMetric(List<Integer> centroid, int[] coords) {
        double xDist = (centroid.get(0) - coords[0]) / DISTANCE_DOWN_SCALER;
        double yDist = (centroid.get(1) - coords[1]) / DISTANCE_DOWN_SCALER;
        double zDist = (centroid.get(2) - coords[2]) / DISTANCE_DOWN_SCALER;
        return  (xDist * xDist) +
                (yDist * yDist) +
                (zDist * zDist);
    }
    
    private double[][] getSquaredMatrix(Double[] linearMatrix) {
        int sqMtrxDim = 4;
        double[][] primitiveMatrix = new double[sqMtrxDim][sqMtrxDim];
        int origColCount = 5;
        // Weird matrix on input: translation column is last column, rather than
        // one after x,y,z.
        for ( int row = 0; row < 3; row++ ) {
            for ( int col = 0; col < 3; col++ ) {
                primitiveMatrix[ row ][ col ] = 
                        linearMatrix[ row * origColCount + col ];
            }
        }
        for ( int row = 0; row < 4; row++ ) {
            primitiveMatrix[ row ][ 3 ] =
                    linearMatrix[ row * origColCount + (origColCount - 1) ];
        }
        primitiveMatrix[sqMtrxDim - 1][sqMtrxDim - 1] = 1.0; // To satisfy invertible requirement.
        return primitiveMatrix;
    }
    
    private TileBase getTileBase(File renderedBaseDirectory) throws Exception {
        File yaml = new File(renderedBaseDirectory, TileBaseReader.STD_TILE_BASE_FILE_NAME);
        if (!yaml.exists() || !yaml.isFile()) {
            String errorString = "Failed to open yaml file " + yaml;
            throw new Exception(errorString);
        }
        TileBase tileBase = new TileBaseReader().readTileBase(new FileInputStream(yaml));
        return tileBase;
    }
    
    private double[][] getInvertedTransform(double[][] primitiveMatrix) {
        Matrix matrix = new Matrix(primitiveMatrix);
        return matrix.inverse().getArray();
    }
    
    private int[] convertToPrimArray( Integer[] array ) {
        int[] rtnVal = new int[ array.length ];
        for ( int i = 0; i < array.length; i++ ) {
            rtnVal[ i ] = array[ i ];
        }
        return rtnVal;
    }
}
