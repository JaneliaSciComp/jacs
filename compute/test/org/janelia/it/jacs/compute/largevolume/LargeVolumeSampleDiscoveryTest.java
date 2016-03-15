package org.janelia.it.jacs.compute.largevolume;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.largevolume.auto_discovery.SampleDiscovery;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.util.Hashtable;
import java.util.Set;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;

/**
 * Created by fosterl on 10/23/15.
 */
public class LargeVolumeSampleDiscoveryTest {
    private Logger logger = Logger.getLogger(LargeVolumeSampleDiscoveryTest.class);
    @Test
    public void discoverSamples() throws Exception {
        SampleDiscovery sampleDiscovery = new SampleDiscovery(null);
        
        String[] paths = new String[] {
                    "/groups/mousebrainmicro/mousebrainmicro/",
                    "/nobackup/mousebrainmicro/",
                    "/nobackup2/mouselight/",
                    "/tier2/mousebrainmicro/mousebrainmicro/",
                    "/tier2/mousebrainmicro-nb/"
        };
        Set<File> discoveries = sampleDiscovery.discover(paths);
        for (File file : discoveries) {
            Path path = Paths.get(file.getAbsolutePath());
            FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
            logger.info("Discovered full file of " + file + " ownership is " + ownerAttributeView.getOwner());
        }
        Assert.assertTrue("No paths found", discoveries.size() > 0);
    }
}
