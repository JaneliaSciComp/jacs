package org.janelia.it.jacs.compute.mbean;

import javax.management.MXBean;

/**
 * This can auto-discover large volume (mouse light) samples and add them to the database.
 * Created by fosterl on 10/23/15.
 */
@MXBean
public interface LargeVolumeSampleDiscoveryMBean {
    void discoverSamples();
}
