package org.janelia.it.jacs.compute.largevolume;

import Jama.Matrix;
import org.janelia.it.jacs.compute.largevolume.model.Tile;
import org.janelia.it.jacs.compute.largevolume.model.TileBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Studies tiles to shake out required information.
 * Created by fosterl on 9/24/14.
 */
public class TileWalker {
    private TileBase tileBase;
    private Map<List<Integer>, RawDataHandle> centroidToRawData;

    public TileWalker( TileBase tileBase ) {
        this.tileBase = tileBase;
    }

    public void interpret() {
        centroidToRawData = new HashMap<>();
        String basePath = tileBase.getPath();
        Integer[] patternArray = new Integer[0];
        for ( Tile tile: tileBase.getTiles() ) {
            Integer[] originInteger = tile.getAabb().getOri();
            Integer[] shapeInteger = tile.getAabb().getShape();

            List<Integer> centroid = calculateCentroid( originInteger, shapeInteger );
            RawDataHandle rawDataHandle = new RawDataHandle();
            rawDataHandle.setBasePath( basePath );
            rawDataHandle.setRelativePath( tile.getPath() );
            rawDataHandle.setCentroid( centroid.toArray( patternArray ) );
            rawDataHandle.setMinCorner( originInteger );
            rawDataHandle.setExtent( shapeInteger );
            centroidToRawData.put( centroid, rawDataHandle );
        }
    }

    public Map<List<Integer>, RawDataHandle> getCentroidToRawData() {
        return centroidToRawData;
    }

    private static final int STD_MATRIX_DIMENSION = 5;

    // Origin and far-corner make the opposite corners of the rectangular solid.
    public String getPathForPixel( Integer[] pixelLocation ) throws Exception {
        return null;
    }

    private Integer[] getNmCoords( Integer[] pixelLocation ) throws Exception {
        Integer[] originInteger = null;
        Matrix originMatrix = makeOneDMatrix( originInteger, STD_MATRIX_DIMENSION );
        Tile tile = null;
        Integer[] endPtInteger = tile.getShape().getDims();
        for ( int i = 0; i < 3; i++ ) {
            endPtInteger[ i ] += Math.round(originInteger[ i ]);
        }
        Double[] transformDouble = tile.getTransform();
        Matrix transformMatrix = make2DSquareMatrix(transformDouble, STD_MATRIX_DIMENSION);

        Matrix endingMatrix = makeOneDMatrix(endPtInteger, STD_MATRIX_DIMENSION);
        Matrix beginMatrix = transformMatrix.times(originMatrix);
        Matrix endMatrix = transformMatrix.times(endingMatrix);

        System.out.println("---------------------------------------------------");
        System.out.println("Begin:");
        beginMatrix.print( 5, 2 );
        System.out.println("End:");
        endMatrix.print( 5, 2 );
        System.out.println();

        return null;
    }

    /**
     * Makes a 2D Matrix object form a linearly-arranged 2D matrix.  Assumes output should be row-major order.
     *
     * @param squareInOneDMatrix input one-D array of Doubles.
     * @param dim how large are the (square) dimensions?
     * @return object suitable for matrix ops.
     */
    private Matrix make2DSquareMatrix(Double[] squareInOneDMatrix, int dim) {
        double[][] rtnVal = new double[dim][dim];
        int i = 0;
        for ( int row = 0; row < dim; row++ ) {
            for ( int col = 0; col < dim; col++ ) {
                rtnVal[ row ][ col ] = squareInOneDMatrix[ i ++ ];
            }
        }
        return new Matrix( rtnVal );
    }

    private Matrix makeOneDMatrix(Integer[] oneDMatrix, int dim) {
        int i;
        double[] rtnVal = new double[ dim ];
        for ( i = 0; i < oneDMatrix.length; i++ ) {
            rtnVal[ i ] = oneDMatrix[ i ];
        }

        // Pad with zeros to the end, then make that 1.
        for ( ; i < dim - 1; i++ ) {
            rtnVal[ i ] = 0.0;
        }
        rtnVal[ dim - 1 ] = 1.0;

        return new Matrix( rtnVal, dim );
    }

    private List<Integer> calculateCentroid( Integer[] originInteger, Integer[] shapeInteger ) {
        if ( shapeInteger.length != originInteger.length ) {
            throw new IllegalArgumentException("Incompatible matrix sizes: " + shapeInteger.length + " vs " + originInteger.length);
        }
        Integer[] farCornerInteger = new Integer[ originInteger.length ];
        for ( int i = 0; i < originInteger.length; i++ ) {
            farCornerInteger[ i ] = originInteger[ i ] + shapeInteger[ i ];
        }

        List<Integer> centroid = new ArrayList<>();
        for ( int i = 0; i < originInteger.length; i++ ) {
            centroid.add((farCornerInteger[ i ] + originInteger[ i ]) / 2);
        }

        return centroid;
    }
}
