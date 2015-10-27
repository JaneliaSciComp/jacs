package org.janelia.it.jacs.compute.mbean;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.compute.api.TiledMicroscopeBeanRemote;
import org.janelia.it.jacs.compute.largevolume.auto_discovery.SampleDiscovery;
import org.janelia.it.jacs.model.entity.EntityActorPermission;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmSample;

import java.util.Set;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import org.apache.log4j.Logger;
/**
 * This should ultimately allow the user to invoke an auto-creation of samples for LVV.
 * Created by fosterl on 10/23/15.
 * @See TiledMicroscopeManager - which currently does not do much.  Should this method be in that instead?
 */
public class LargeVolumeSampleDiscovery implements LargeVolumeSampleDiscoveryMBean {
    public static final String SHARED_PERMISSION = "group:mouselight_common_user";
    // Setting ownership of all created samples, to Jayaram, to avoid breakage
    // due to disagreements between filesystem and Workstation usernames.
    public static final String OWNERSHIP_USER = "chandrashekarj";
    private static final Logger logger = Logger.getLogger(LargeVolumeSampleDiscovery.class);

    @Override
    public void discoverSamples() {
        try {
            // Need to find out whether samples already exist.
            AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();

            TiledMicroscopeBeanRemote timBean = EJBFactory.getRemoteTiledMicroscopeBean();
            EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
            SampleDiscovery discovery = new SampleDiscovery();
            Set<File> sampleDirectories = discovery.discover();
            // NOTE: questions should be answered before going much further on this.
            //  What user for that first field?
            //  What if a sample on same directory already exists?
            //
            for (File sample: sampleDirectories) {
                Path path = Paths.get(sample.getAbsolutePath());
                FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
                String fileLocation = sample.getAbsolutePath();
                List<Entity> entities = annotationBean.getEntitiesWithFilePath(fileLocation);
                boolean original = true;
                for ( Entity entity: entities ) {
                    if (entity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                        Set<EntityActorPermission> permissions = entity.getEntityActorPermissions();
                        for (EntityActorPermission permission: permissions) {
                            if ( permission.getSubjectKey().equals(SHARED_PERMISSION)) {
                                original = false;
                                break;
                            }
                        }
                    }
                }

                if ( original ) {
                    final String userName = ownerAttributeView.getOwner().getName();
                    logger.info("Found sample belonging to " + userName + ".  Adding " + sample.getName());
                    TmSample tmSample = timBean.createTiledMicroscopeSample(
                            OWNERSHIP_USER, sample.getName(), fileLocation
                    );
                    Long entityId = tmSample.getId();  // Happens to be the entity id of the wrapped entity.

                    String ownerSubject = "user:" + OWNERSHIP_USER;
                    entityBean.grantPermissions( ownerSubject, entityId, SHARED_PERMISSION, "r", true);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

