package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;

/**
 * File discovery service for supporting files.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CellCountingResultsDiscoveryService extends AbstractDomainService {

    protected String resultEntityName;
    protected String resultEntityType;
    protected List<File> allFiles = new ArrayList<File>();
    
    @Override
    protected void execute() throws Exception {
        // TODO: PORT FROM NG   
        throw new UnsupportedOperationException();
    }    
    
//    @Override
//    public void execute(IProcessData processData) throws ServiceException {
//        this.resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME");
//        if (resultEntityName==null) {
//            throw new ServiceException("Input parameter RESULT_ENTITY_NAME may not be null");
//        }
//
//        this.resultEntityType = (String)processData.getItem("RESULT_ENTITY_TYPE");
//        if (resultEntityType==null) {
//            throw new ServiceException("Input parameter RESULT_ENTITY_TYPE may not be null");
//        }
//
//        super.execute(processData);
//    }
//
//    @Override
//    protected Entity verifyOrCreateChildFolderFromDir(Entity parentFolder, File dir, Integer index) throws Exception {
//
//        logger.info("Discovering cell counting result files in " + dir.getAbsolutePath());
//        logger.info("Creating entity named '" + resultEntityName + "' with type '" + resultEntityType + "'");
//
//        Entity resultEntity = helper.createFileEntity(dir.getAbsolutePath(), resultEntityName, resultEntityType);
//        resultEntity.setValueByAttributeName(EntityConstants.ATTRIBUTE_CELL_COUNT, getCellCountValue(dir));
//        resultEntity = entityBean.saveOrUpdateEntity(resultEntity);
//        helper.addToParent(parentFolder, resultEntity, parentFolder.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_RESULT);
//
//        processData.putItem("RESULT_ENTITY", resultEntity);
//        processData.putItem("RESULT_ENTITY_ID", resultEntity.getId().toString());
//
//        return resultEntity;
//    }
//
//    @Override
//    protected void processFolderForData(Entity folder) throws Exception {
//
//        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
//        logger.info("Processing "+folder.getName()+" results in "+dir.getAbsolutePath());
//
//        if (!dir.canRead()) {
//            logger.info("Cannot read from folder "+dir.getAbsolutePath());
//            return;
//        }
//
//        allFiles.addAll(helper.addFilesInDirToFolder(folder, dir, true));
//    }
//
//    public String getCellCountValue(File dir) {
//        String cellCountValue = "UNK";
//        File[] dirFiles = dir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.endsWith("_CellCounterReport.txt");
//            }
//        });
//        if (null!=dirFiles && dirFiles.length==1) {
//            Scanner scanner=null;
//            try {
//                scanner = new Scanner(dirFiles[0]);
//                int count=0;
//                while (scanner.hasNextLine()) {
//                    count++;
//                    scanner.nextLine();
//                }
//                cellCountValue = Integer.toString(count);
//            }
//            catch (Exception ex) {
//                logger.error("Unable to determine the cell count result size.  Continuing...");
//            }
//            finally {
//                if (null!=scanner) {
//                    scanner.close();
//                }
//            }
//        }
//        return cellCountValue;
//    }
}
