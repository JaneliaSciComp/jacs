package org.janelia.it.jacs.compute.largevolume.auto_discovery;

import java.util.HashSet;
import java.util.Set;

/**
 * Walk a known set of base directories, finding all valid sample folders, and add them to the database,
 * so that all Workstation (or other) clients can find them.
 *
 * Created by fosterl on 10/23/15.
 */
public class SampleDiscovery {
    // @See org.janelia.it.workstation.gui.large_volume_viewer.QuadViewUi
    //  This array copied from the GUI side. These prefixes are needed there as well,
    //  simply because we have heterogeneous clients.  They must resolve local-standard
    //  file paths, against the centrally-needed linux paths in this list. LLF
    private static String [] MOUSE_BRAIN_MICRO_PREFIXES = {
            "/groups/mousebrainmicro/mousebrainmicro/",
            "/nobackup/mousebrainmicro/",
            "/tier2/mousebrainmicro/mousebrainmicro/",
            "/tier2/mousebrainmicro-nb/"
    };

    // Credentials for running server operations.
    private String username;
    private String password;

    public SampleDiscovery(String username, String password) {

    }

    /**
     * Iterate over all base directories.
     * @throws Exception
     */
    public Set<String> discover() throws Exception {
        Set<String> rtnVal = new HashSet<>();
        for (String prefix: MOUSE_BRAIN_MICRO_PREFIXES) {
            SampleDiscoveryVisitor visitor = new SampleDiscoveryVisitor(prefix);
            visitor.exec();
            rtnVal.addAll(visitor.getValidatedFolders());
        }
        return rtnVal;
    }
}
