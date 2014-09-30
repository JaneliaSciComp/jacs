package org.janelia.it.jacs.compute.largevolume;

import org.janelia.it.jacs.compute.largevolume.model.TileBase;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Created by fosterl on 9/26/14.
 */
public class RawTiffFetcherTest {
    private File baseLocation;
    private TileBase tileBase;

    @Before
    public void setup() throws Exception {
        tileBase = new TileBaseReader().readTileBase( LargeVolumeYamlTest.getTestFileStream() );
        baseLocation = TileWalkerTest.getBaseLocationFile();
    }

    @Test
    public void fetch() throws Exception {
        RawTiffFetcher fetcher = new RawTiffFetcher( tileBase, baseLocation );
        File[] files = new File[ 5 ];
        int[][] coords = {
            new int[] { 0, 0, 0 },
            new int[] { 0, 200000, 100000 },
            new int[] { 300000, 3000000, 300000 },
            new int[] { 500000, 5000000, 500000 },
            new int[] { 700000, 7000000, 600000 }
        };
        files[ 0 ] = fetcher.getMicroscopeFileDir(coords[0]);
        files[ 1 ] = fetcher.getMicroscopeFileDir(coords[1]);
        files[ 2 ] = fetcher.getMicroscopeFileDir(coords[2]);
        files[ 3 ] = fetcher.getMicroscopeFileDir(coords[3]);
        files[ 4 ] = fetcher.getMicroscopeFileDir(coords[4]);

        CoordinateToRawTransform transform = new CoordinateToRawTransform( baseLocation );
        for ( int i = 0; i < files.length; i++ ) {
            System.out.println("Directory " + i + " is " + files[ i ] );
            File[] tiffFiles = fetcher.getMicroscopeFiles( files[i] );
            System.out.println("File-0 for " + i + " is " + tiffFiles[ 0 ] );
            System.out.println("File-1 for " + i + " is " + tiffFiles[ 1 ] );

            int[] scopeCoords = transform.getMicroscopeCoordinate( coords[ i ] );
            System.out.println(
                    String.format(
                            "Translated coordinates from %,d %,d %,d to %,d %,d %,d.",
                            coords[ i ][ 0 ], coords[ i ][ 1 ], coords[ i ][ 2 ],
                            scopeCoords[ 0 ], scopeCoords[ 1 ], scopeCoords[ 2 ]
                    )
            );
        }
    }

}
