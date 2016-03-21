package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;

import org.janelia.it.jacs.compute.service.align.AlignmentInputFile;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Cleans up the temporary regenerated files. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CleanUpRegeneratedFilesService extends AbstractDomainService {

    private static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
    private static final String centralDir = SystemConfigurationProperties.getString(CENTRAL_DIR_PROP);
    
    public void execute() throws Exception {
        AlignmentInputFile alignmentInputFile = (AlignmentInputFile)data.getRequiredItem("ALIGNMENT_INPUT");
        String filepath = alignmentInputFile.getFilepath();
        
        File file = new File(filepath);
        File sampleDir = file.getParentFile().getParentFile().getAbsoluteFile();
        String path = sampleDir.getAbsolutePath();

        if (!path.startsWith(centralDir)) {
            logger.warn("Cannot delete 'temp' dir because name it is not in the central filestore: "+path);
            return;
        }
        
        Node node = null;
        try {
            long fileNodeId = Long.parseLong(sampleDir.getName());
            node = computeBean.getNodeById(fileNodeId);
        }
        catch (NumberFormatException e) {
            logger.warn("Cannot delete 'temp' dir because name is not an id: "+path);
            return;
        }

        long numEntities = entityBean.getCountUserEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_FILE_PATH, path+"%");
        if (numEntities==0) {
            contextLogger.info("Deleting regenerated temporary files at: "+path+" (nodeId="+node.getObjectId()+")");
            String username = EntityUtils.getNameFromSubjectKey(ownerKey);
            computeBean.trashNode(username, node.getObjectId(), true);
        }
        else {
            logger.warn("Path should be temporary, but it's referenced in the database: "+path);
        }
    }
}
