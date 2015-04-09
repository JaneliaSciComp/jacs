package org.janelia.it.jacs.compute.largevolume;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import org.junit.Assert;

/**
 * Created by fosterl on 9/26/14.
 */
public class RawTiffFetcherTest {
    private File baseLocation;

    @Before
    public void setup() throws Exception {
        baseLocation = TileWalkerTest.getBaseLocationFile();
    }

    @Test
    public void fetch() throws Exception {
        RawFileFetcher fetcher = RawFileFetcher.getRawFileFetcher( baseLocation.getAbsolutePath() );
        int[][] coords = {
            new int[] { 16101, 18741, 6643 },
            new int[] { 14926, 15760, 6401 },
            new int[] { 14982, 17027, 6401 },
            new int[] { 16059, 18501, 4906 },
            new int[] { 26019, 19453, 4906 }
        };
        RawFileInfo[] files = new RawFileInfo[ coords.length ];
        for ( int i = 0; i < coords.length; i++ ) {
            files[ i ] = fetcher.getNearestFileInfo(coords[i]);
        }

        CoordinateToRawTransform transform = new CoordinateToRawTransform( baseLocation );
        for ( int i = 0; i < files.length; i++ ) {
            printOneFile(coords[i], files[i].getChannel0(), files[i].getChannel1(), transform.getMicroscopeCoordinate(coords[i]), i);
        }

        // This test value should be taken from a leaf-level node in octree.
        Assert.assertEquals("Scale-X incorrect", 300.279503, transform.getScale()[0], 0.01);
        Assert.assertEquals("Scale-Y incorrect", 300.241237, transform.getScale()[1], 0.01);
        Assert.assertEquals("Scale-Z incorrect", 1001.480000, transform.getScale()[2], 0.01);
    }
    
    private void printOneFile(int[] coord, File file0, File file1, int[] microscopeCoordinate, int i) {
        System.out.println("Raw File/0 " + i + " is " + file0);
        System.out.println("Raw File/1 " + i + " is " + file1);

        int[] scopeCoords = microscopeCoordinate;
        System.out.println(
                String.format(
                        "Translated coordinates from %,d %,d %,d to %,d %,d %,d.",
                        coord[ 0 ], coord[ 1 ], coord[ 2 ],
                        scopeCoords[ 0 ], scopeCoords[ 1 ], scopeCoords[ 2 ]
                )
        );
    }

}
