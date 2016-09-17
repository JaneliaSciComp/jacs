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

import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.model.util.MatrixUtilities;
/**
 * This should ultimately allow the user to invoke an auto-creation of samples for LVV.
 * Created by fosterl on 10/23/15.
 * @See TiledMicroscopeManager - which currently does not do much.  Should this method be in that instead?
 */
@Singleton
@Startup
public class LargeVolumeSampleDiscovery extends AbstractComponentMBean implements LargeVolumeSampleDiscoveryMBean {
    
    public static final String SHARED_PERMISSION = "group:mouselight";
    // Setting ownership of all created samples, to Jayaram, to avoid breakage
    // due to disagreements between filesystem and Workstation usernames.
    public static final String OWNERSHIP_USER = "chandrashekarj";
    private static final Logger logger = Logger.getLogger(LargeVolumeSampleDiscovery.class);
    
    private Notifier notifier;
    
    public LargeVolumeSampleDiscovery() {
        super("jacs");
    }
    
    public LargeVolumeSampleDiscovery(Notifier notifier) {
        super("jacs");
        this.notifier = notifier;
    }

    @Override
    public void discoverSamples() {
        int addedSampleCount = 0;
        try {
            // Need to find out whether samples already exist.
            AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();

            TiledMicroscopeBeanRemote timBean = EJBFactory.getRemoteTiledMicroscopeBean();
            EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
            SampleDiscovery discovery = new SampleDiscovery(entityBean);
            Set<File> sampleDirectories = discovery.discover();
            
            // Iterate over all samples, adding them to db.
            for (File sample: sampleDirectories) {
                Path path = Paths.get(sample.getAbsolutePath());
                FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
                String fileLocation = sample.getAbsolutePath();
                List<Entity> entities = annotationBean.getEntitiesWithFilePath(fileLocation);
                boolean original = true;
                for ( Entity entity: entities ) {
                    if (entity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                        if (entity.getOwnerKey().equals(SHARED_PERMISSION)) {
                            logger.info("Sample " + fileLocation + " already known.  Ignoring.");
                            original = false;
                        }
                    }
                }

                if ( original ) {
                    CoordinateToRawTransform transform = null;
                    try {
                        transform = timBean.getTransform(fileLocation);
                    } catch (Exception ex) {
                        logger.error("Failed to interpret the tilebase file for " + fileLocation + ".  Excluding it from Sample generation.");
                        
                        continue;

                    }
                    
                    final String userName = ownerAttributeView.getOwner().getName();
                    logger.info("Found sample belonging to " + userName + ".  Adding " + sample.getName());
                    TmSample tmSample = timBean.createTiledMicroscopeSample(
                            SHARED_PERMISSION, sample.getName(), fileLocation
                    );
                    Long entityId = tmSample.getId();  // Happens to be the entity id of the wrapped entity.

                    //String ownerSubject = "user:" + OWNERSHIP_USER;
                    String ownerSubject = SHARED_PERMISSION;
                    //entityBean.grantPermissions( ownerSubject, entityId, SHARED_PERMISSION, "r", true);
                    
                    // Now, apply conversion matrices to the sample.
                    int[] origin = new int[3];
                    System.arraycopy(transform.getOrigin(), 0, origin, 0, 3);
                    double[] voxelMicrometers = new double[3];
                    System.arraycopy(transform.getScale(), 0, voxelMicrometers, 0, 3);
                    for (int i = 0; i < 3; i++) {
                        origin[i] /= voxelMicrometers[i];
                        voxelMicrometers[i] /= 1000;
                    }
                    String voxToMicronString = MatrixUtilities.createSerializableVoxToMicron(voxelMicrometers, origin);
                    String micronToVoxString = MatrixUtilities.createSerializableMicronToVox(voxelMicrometers, origin);

                    entityBean.setOrUpdateValue(ownerSubject, entityId, EntityConstants.ATTRIBUTE_MICRON_TO_VOXEL_MATRIX, micronToVoxString);
                    entityBean.setOrUpdateValue(ownerSubject, entityId, EntityConstants.ATTRIBUTE_VOXEL_TO_MICRON_MATRIX, voxToMicronString);
                    
                    addedSampleCount ++;

                }
            }
            if (notifier != null) {
                notifier.status(true, String.format("Added %d samples.", addedSampleCount));
            }
        } catch (Exception ex) {
            if (notifier != null) {
                notifier.status(false, "Exception: " + ex.getMessage());
            }
            ex.printStackTrace();
        }
    }

    public static interface Notifier {
        void status(boolean success, String reason);
    }
}

