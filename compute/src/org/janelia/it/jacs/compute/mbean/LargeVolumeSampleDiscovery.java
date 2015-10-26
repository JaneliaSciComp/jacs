package org.janelia.it.jacs.compute.mbean;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.TiledMicroscopeBeanRemote;
import org.janelia.it.jacs.compute.largevolume.auto_discovery.SampleDiscovery;

import java.util.Set;
import java.io.File;

/**
 * This should ultimately allow the user to invoke an auto-creation of samples for LVV.
 * Created by fosterl on 10/23/15.
 * @See TiledMicroscopeManager - which currently does not do much.  Should this method be in that instead?
 */
public class LargeVolumeSampleDiscovery implements LargeVolumeSampleDiscoveryMBean {
    @Override
    public void discoverSamples() {
        try {
            TiledMicroscopeBeanRemote timBean = EJBFactory.getRemoteTiledMicroscopeBean();
            SampleDiscovery discovery = new SampleDiscovery();
            Set<File> sampleDirectories = discovery.discover();
            // NOTE: questions should be answered before going much further on this.
            //  What user for that first field?
            //  What if a sample on same directory already exists?
            //
            for (File sample: sampleDirectories) {
                timBean.createTiledMicroscopeSample( null, sample.getName(), sample.getAbsolutePath() );
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

