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
        int[][] coords = {
            new int[] { 16101, 18741, 6643 },
            new int[] { 14926, 15760, 6401 },
            new int[] { 14982, 17027, 6401 },
            new int[] { 16059, 18501, 4906 },
            new int[] { 26019, 19453, 4906 }
        };
        File[] files = new File[ coords.length ];
        for ( int i = 0; i < coords.length; i++ ) {
            files[ i ] = fetcher.getMicroscopeFileDir(coords[i]);
        }

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
