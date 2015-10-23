package org.janelia.it.jacs.compute.largevolume;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.largevolume.auto_discovery.SampleDiscovery;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

/**
 * Created by fosterl on 10/23/15.
 */
public class LargeVolumeSampleDiscoveryTest {
    private Logger logger = Logger.getLogger(LargeVolumeSampleDiscoveryTest.class);
    @Test
    public void discoverSamples() throws Exception {
        SampleDiscovery sampleDiscovery = new SampleDiscovery(null, null);
        Set<String> discoveries = sampleDiscovery.discover();
        for (String path: discoveries) {
            logger.info("Discovered full path of " + path);
        }
        Assert.assertTrue("No paths found", discoveries.size() > 0);
    }
}
