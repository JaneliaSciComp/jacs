package org.janelia.it.jacs.compute.largevolume;

import org.janelia.it.jacs.compute.largevolume.model.TileBase;
import org.junit.Assert;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

/**
 * Created by fosterl on 9/24/14.
 */
public class TileWalkerTest {
    private static final String BASE_LOCATION = System.getProperty("user.dir") + "/compute/test/resources/largevolume/2014-06-24-Descriptor-stitch1/";

    private InputStream inputStream;

    public static final File getBaseLocationFile() { return new File( BASE_LOCATION); }

    @Before
    public void setUp() throws Exception {
        inputStream = LargeVolumeYamlTest.getTestFileStream();
    }

    @Test
    public void walk() throws Exception {
        // Now, to find and parse the transform.txt file.
        //  Doing a directory drill-down.
        CoordinateToRawTransform transformParameters = new CoordinateToRawTransform( new File( BASE_LOCATION )  );
        System.out.println(
                String.format(
                        "Transform origin: %,d %,d %,d.  Transform scale: %,f %,f %,f.",
                        transformParameters.getOrigin()[0], transformParameters.getOrigin()[1], transformParameters.getOrigin()[2],
                        transformParameters.getScale()[0], transformParameters.getScale()[1], transformParameters.getScale()[2]
                )
        );
        for ( int i = 0; i < 3; i++ ) {
            Assert.assertNotEquals( "0 found for origin[" + i + "]", transformParameters.getOrigin()[i], 0 );
            Assert.assertNotEquals( "0 found for scale[" + i + "]", transformParameters.getScale()[i], 0 );
        }

        TileBase tileBase = new TileBaseReader().readTileBase( inputStream );
        TileWalker walker = new TileWalker( tileBase );
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
                "Global Max Centroid: [%,d %,d %,d]",
                maxCoords[0], maxCoords[1], maxCoords[2]
        ));
        System.out.println(String.format(
                "Global Min Centroid: [%,d %,d %,d]",
                minCoords[0], minCoords[1], minCoords[2]
        ));

        System.out.println(String.format(
                "Global Extents: [%,d %,d %,d]",
                maxCoords[0] - minCoords[0],
                maxCoords[1] - minCoords[1],
                maxCoords[2] - minCoords[2]
        ));

        System.out.println(String.format("Total centroid records: %,d.", map.size()));

    }
}
