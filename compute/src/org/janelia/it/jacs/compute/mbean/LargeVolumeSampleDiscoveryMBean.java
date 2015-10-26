package org.janelia.it.jacs.compute.mbean;

/**
 * This can auto-discover large volume (mouse light) samples and add them to the database.
 * Created by fosterl on 10/23/15.
 */
public interface LargeVolumeSampleDiscoveryMBean {
    void discoverSamples();
}
