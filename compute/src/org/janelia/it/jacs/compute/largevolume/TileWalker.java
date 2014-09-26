package org.janelia.it.jacs.compute.largevolume;

import org.janelia.it.jacs.compute.largevolume.model.Tile;
import org.janelia.it.jacs.compute.largevolume.model.TileBase;

import java.io.*;
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
