package org.janelia.it.jacs.compute.largevolume;

import org.janelia.it.jacs.compute.largevolume.model.Tile;
import org.janelia.it.jacs.compute.largevolume.model.TileBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

/**
 * Created by fosterl on 9/24/14.
 */
public class LargeVolumeYamlTest {
    private static final String INFILE_RESOURCE = "/largevolume/2014-06-24-Descriptor-stitch1/tilebase.cache.yml";
    public static final String TILING_PATH = "/tier2/mousebrainmicro/mousebrainmicro/data/2014-06-24/Tiling";
    public static final String FIRST_TILE_RELATIVE_PATH = "/2014-07-05/02/02030";
    //    private static final String INFILE_RESOURCE = "/largevolume/2014-06-24-Descriptor-stitch1/head.yml";
    private InputStream testStream;

    @Before
    public void setUp() throws Exception {
        if ( null == ( testStream = getTestFileStream() ) ) {
            String classpath = System.getProperty("java.class.path");
            String[] classpathMembers = classpath.split(":");
            StringBuilder bldr = new StringBuilder();
            for ( String member: classpathMembers ) {
                if ( member.contains("compute")) {
                    bldr.append(":").append(member);
                }
            }
            throw new Exception("Stream for " + INFILE_RESOURCE + " not found.  Working directory is " + System.getProperty("user.dir") + ", and interesting classpath is " + bldr.toString());
        }
    }

    @Test
    public void readFile() throws Exception {
        //        Yaml yaml = new Yaml();
        TileBase tileBase = new TileBaseReader().readTileBase( testStream );
        Assert.assertNotNull("Null read", tileBase);
        String path = tileBase.getPath();
        Assert.assertEquals("Unexpected path read.", path, TILING_PATH);
        Assert.assertNotNull("Null tile set read.", tileBase.getTiles());
        Tile tile = tileBase.getTiles().get( 0 );
        Assert.assertNotNull("Null tile read.", tile );
        String subPath = tile.getPath();
        Assert.assertEquals("Tile sub path not as expected.", subPath, FIRST_TILE_RELATIVE_PATH);
    }

    @After
    public void tearDown() throws Exception {

    }

    public static InputStream getTestFileStream() throws Exception {
        return LargeVolumeYamlTest.class.getResourceAsStream( INFILE_RESOURCE );
    }

}


