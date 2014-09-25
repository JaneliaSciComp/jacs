package org.janelia.it.jacs.compute.largevolume;

import org.janelia.it.jacs.compute.largevolume.model.TileBase;
import org.junit.Assert;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

/**
 * Created by fosterl on 9/24/14.
 */
public class TileWalkerTest {
    private InputStream inputStream;
    @Before
    public void setUp() throws Exception {
        inputStream = LargeVolumeYamlTest.getTestFileStream();
    }

    @Test
    public void walk() throws Exception {
        TileBase tileBase = new TileBaseReader().readTileBase( inputStream );
        TileWalker walker = new TileWalker( tileBase );
        walker.interpret();
        Map<List<Integer>, RawDataHandle> map = walker.getCentroidToRawData();
        Assert.assertNotNull( "Centroid Map is Null", map );
        int stopCount = 0;
        // Dumping some data, to see that it looks right.
        for ( List<Integer> centroid: map.keySet() ) {
            ++ stopCount;
            if ( stopCount > 10 ) {
                continue;
            }

            RawDataHandle handle = map.get( centroid );
            System.out.println(String.format(
                    "Centroid: [%,d %,d %,d]  Path: %s/%s",
                    handle.getCentroid()[0], handle.getCentroid()[1], handle.getCentroid()[2],
                    handle.getBasePath(),
                    handle.getRelativePath()
                    )
            );

            Assert.assertNotNull( handle.getBasePath() );
            Assert.assertNotNull( handle.getRelativePath() );
            Assert.assertNotNull( handle.getCentroid() );

        }

        Integer[] minCoords = new Integer[] {
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        };

        Integer[] maxCoords = new Integer[] {
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
        };

        for ( List<Integer> centroid: map.keySet() ) {
            RawDataHandle handle = map.get(centroid);
            for ( int i = 0; i < 3; i++ ) {
                if ( handle.getMinCorner()[i] < minCoords[ i ] ) {
                    minCoords[ i ] = centroid.get( i );
                }
            }

            for ( int i = 0; i < 3; i++ ) {
                Integer endPt = handle.getMinCorner()[ i ] + handle.getExtent()[ i ];
                if ( endPt > maxCoords[ i ] ) {
                    maxCoords[ i ] = endPt;
                }
            }

        }

        System.out.println(String.format(
                "Global Max: [%,d %,d %,d]",
                maxCoords[0], maxCoords[1], maxCoords[2]
        ));
        System.out.println(String.format(
                "Global Min: [%,d %,d %,d]",
                minCoords[0], minCoords[1], minCoords[2]
        ));

        System.out.println(String.format(
                "Global Extents: [%,d %,d %,d]",
                maxCoords[0] - minCoords[0],
                maxCoords[1] - minCoords[1],
                maxCoords[2] - minCoords[2]
        ));

        System.out.println(String.format("Total records: %,d.", map.size()));
    }
}
