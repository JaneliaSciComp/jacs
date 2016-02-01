package org.janelia.it.jacs.compute.largevolume.auto_discovery;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import static org.janelia.it.jacs.shared.sample_discovery.SampleDiscoveryConstants.*;

/**
 * Walk a known set of base directories, finding all valid sample folders, and add them to the database,
 * so that all Workstation (or other) clients can find them.
 *
 * Created by fosterl on 10/23/15.
 */
public class SampleDiscovery {
    private EntityBeanRemote entityBean;
    public SampleDiscovery(EntityBeanRemote entityBean) {
        this.entityBean = entityBean;
    }

    /**
     * Iterate over all base directories.
     * @throws Exception
     */
    public Set<File> discover() throws Exception {
        String folderName = ENTITY_FOLDER_NAME;
        Collection<Entity> folders = entityBean.getEntitiesByName(REQUIRED_OWNER, folderName);

        // Typical paths.
        //            "/groups/mousebrainmicro/mousebrainmicro/",
        //            "/nobackup/mousebrainmicro/",
        //            "/tier2/mousebrainmicro/mousebrainmicro/",
        //            "/tier2/mousebrainmicro-nb/"
        String[] mouseBrainMicroPrefixes = null;
        if (folders.size() >= 1) {
            Entity folderEntity = folders.iterator().next();
            for (Entity child: folderEntity.getChildren()) {
                // Flesh out the child entity.
                child = entityBean.getEntityById(REQUIRED_OWNER, child.getId());
                if (child == null || child.getEntityTypeName() == null) {
                    continue;
                }
                if (child.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)
                    && child.getName().equals(SETTINGS_ENTITY_NAME)) {
                    // Now, to find the Name/Value pair with our values.
                    for (EntityData ed : child.getEntityData()) {
                        if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
                            String nameValue = ed.getValue();
                            String[] nameValueArr = nameValue.split("=");
                            if (nameValueArr.length == 2 && nameValueArr[0].trim().equals(PATHS_ATTRIBUTE)) {
                                // Now have the paths info.
                                mouseBrainMicroPrefixes = nameValueArr[1].split("\n");
                                break;
                            }
                        }
                    }
                    
                }
            }
        }
        return discover(mouseBrainMicroPrefixes);        
    }

    public Set<File> discover(String[] mouseBrainMicroPrefixes) throws IOException {
        Set<File> rtnVal = new HashSet<>();
        for (String prefix: mouseBrainMicroPrefixes) {
            SampleDiscoveryVisitor visitor = new SampleDiscoveryVisitor(prefix);
            visitor.exec();
            rtnVal.addAll(visitor.getValidatedFolders());
        }
        return rtnVal;
    }
}
