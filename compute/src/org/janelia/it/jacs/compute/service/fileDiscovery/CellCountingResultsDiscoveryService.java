package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * File discovery service for supporting files.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CellCountingResultsDiscoveryService extends FileDiscoveryService {

    protected String resultEntityName;
    protected String resultEntityType;
    protected List<File> allFiles = new ArrayList<File>();

    @Override
    public void execute(IProcessData processData) throws ServiceException {

        this.resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME");
        if (resultEntityName==null) {
            throw new ServiceException("Input parameter RESULT_ENTITY_NAME may not be null");
        }

        this.resultEntityType = (String)processData.getItem("RESULT_ENTITY_TYPE");
        if (resultEntityType==null) {
            throw new ServiceException("Input parameter RESULT_ENTITY_TYPE may not be null");
        }

        super.execute(processData);
    }

    @Override
    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir, Integer index) throws Exception {

        logger.info("Discovering cell counting result files in "+dir.getAbsolutePath());
        logger.info("Creating entity named '"+resultEntityName+"' with type '"+resultEntityType+"'");

        Entity resultEntity = helper.createFileEntity(dir.getAbsolutePath(), resultEntityName, resultEntityType);
        helper.addToParent(parentFolder, resultEntity, parentFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_RESULT);

        processData.putItem("RESULT_ENTITY", resultEntity);
        processData.putItem("RESULT_ENTITY_ID", resultEntity.getId().toString());

        return resultEntity;
    }

    @Override
    protected void processFolderForData(Entity folder) throws Exception {

        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing "+folder.getName()+" results in "+dir.getAbsolutePath());

        if (!dir.canRead()) {
            logger.info("Cannot read from folder "+dir.getAbsolutePath());
            return;
        }

        allFiles.addAll(helper.addFilesInDirToFolder(folder, dir, true));
    }
}
