/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.access;

import java.util.Set;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.compute.api.TiledMicroscopeBeanRemote;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmSample;
import org.junit.Test;

/**
 * Testing various aspects of Tiled-Microscope entity/model creation, etc.
 *
 * @author fosterl
 */
public class TiledMicroscopeEJBTest {
    public static final String TEST_SAMPLE_NAME = "TestSample";
    public static final String TEST_SAMPLE_LOC = "/tier2/mousebrainmicro-nb/fromnobackup/2014-06-24-Descriptor-stitch1";
    public static final String TEST_IMPORT_LOC = "/home/fosterl/IMPORT_20151014/2014-06-24-Descriptor-Stitch1 nb General WS";

    //@Test
    // This test needs to have SystemConfigurationProperties populated from the developer's props, to target the developer's server.
    // No time at present.  LLF.
    public void neuronImport() throws Exception {
        // Create sample, workspace, and neuron.  Do an import of SWC.
        try {
            SystemConfigurationProperties.reload();
            TiledMicroscopeBeanRemote remoteTiledMicroscopeBean = EJBFactory.getRemoteTiledMicroscopeBean();
            EntityBeanRemote remoteEntityBean = EJBFactory.getRemoteEntityBean();
            TmSample sample = remoteTiledMicroscopeBean.createTiledMicroscopeSample(User.SYSTEM_USER_KEY, TEST_SAMPLE_NAME, TEST_SAMPLE_LOC);
            Long parentId = 0L;
            Set<Entity> parentEntities = remoteEntityBean.getEntitiesByName(User.SYSTEM_USER_KEY, EntityConstants.TYPE_WORKSPACE);
            if (parentEntities != null  &&  parentEntities.size() > 0) {
                Entity parentEntity = parentEntities.iterator().next();
                parentId = parentEntity.getId();
            }
            else {
                throw new Exception("Failed to obtain " + User.SYSTEM_USER_KEY + "'s workspace root.");
            }
            
            remoteTiledMicroscopeBean.createTiledMicroscopeWorkspace(parentId, sample.getId(), TEST_SAMPLE_NAME + " WS", User.SYSTEM_USER_KEY);            
            remoteTiledMicroscopeBean.importSWCFolder(TEST_IMPORT_LOC, User.SYSTEM_USER_KEY, null, sample.getId());
        } catch (Exception ex) {
            // Reporting is the whole point of this test.
            ex.printStackTrace();
            throw ex;
        }
    }
}
