package org.janelia.it.jacs.compute.largevolume;

import java.io.File;
import java.io.FileInputStream;
import org.janelia.it.jacs.compute.largevolume.model.Tile;
import org.janelia.it.jacs.compute.largevolume.model.TileBase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

/**
 * Created by fosterl on 9/24/14.
 */
public class LargeVolumeYamlTest {
    private static final String INFILE_RESOURCE = "/largevolume/2014-06-24-Descriptor-stitch1/";
    private static final String OLD_TILING_PATH = "/tier2/mousebrainmicro/mousebrainmicro/data/2014-06-24/Tiling";
    private static final String NEW_TILING_PATH = "/tier2/mousebrainmicro/mousebrainmicro/data/2015-06-19/Tiling";
    private static final String OLD_FIRST_TILE_RELATIVE_PATH = "/2014-07-05/02/02030";
    private static final String NEW_FIRST_TILE_RELATIVE_PATH = "/2015-06-22/00/00000";
    private InputStream testStream;
    private InputStream modernYmlStream;

    @Before
    public void setUp() throws Exception {
        if ( null == ( testStream = TileWalkerTest.getTestFileStream() ) ) {
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
        if ( null == ( modernYmlStream = TileWalkerTest.getNewStyleTestFileStream() ) ) {
            String classpath = System.getProperty("java.class.path");
            String[] classpathMembers = classpath.split(":");
            StringBuilder bldr = new StringBuilder();
            for ( String member: classpathMembers ) {
                if ( member.contains("compute")) {
                    bldr.append(":").append(member);
                }
            }
            throw new Exception("Stream for " + TileWalkerTest.NEWSTYLE_TILEBASE_YML + " under " + TileWalkerTest.JOHAN_FULL_DIR + " not found.  Working directory is " + System.getProperty("user.dir") + ", and interesting classpath is " + bldr.toString());
        }
    }

    @Test
    public void readOldFile() throws Exception {
        examineTileBase(testStream, OLD_TILING_PATH, OLD_FIRST_TILE_RELATIVE_PATH);
    }

    @Test
    public void readModernFile() throws Exception {
        examineTileBase(modernYmlStream, NEW_TILING_PATH, NEW_FIRST_TILE_RELATIVE_PATH);
    }

    @Test
    public void checkYamlTolerance() throws Exception {
        InputStream nsTestStream = TileWalkerTest.getKnitTestFileStream();
        TileBase tileBase = new TileBaseReader().readTileBase(nsTestStream);
        Assert.assertNotNull("Null read", tileBase);
        String path = tileBase.getPath();
        //Assert.assertEquals("Unexpected path read.", path, TILING_PATH);
        Assert.assertNotNull("Null tile set read.", tileBase.getTiles());
        Tile tile = tileBase.getTiles().get(0);
        Assert.assertNotNull("Null tile read.", tile);
    }

    @After
    public void tearDown() throws Exception {

    }

    private void examineTileBase(InputStream inputStream, String tilingPath, String tileRelativePath) throws Exception {
        //        Yaml yaml = new Yaml();
        TileBase tileBase = new TileBaseReader().readTileBase( inputStream );
        Assert.assertNotNull("Null read", tileBase);
        String path = tileBase.getPath();
        Assert.assertEquals("Unexpected path read.", path, tilingPath);
        Assert.assertNotNull("Null tile set read.", tileBase.getTiles());
        Tile tile = tileBase.getTiles().get( 0 );
        Assert.assertNotNull("Null tile read.", tile );
        String subPath = tile.getPath();
        Assert.assertEquals("Tile sub path not as expected.", subPath, tileRelativePath);
    }

}


