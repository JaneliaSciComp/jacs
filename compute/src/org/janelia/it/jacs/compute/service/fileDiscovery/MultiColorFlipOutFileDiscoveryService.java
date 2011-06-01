package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MultiColorFlipOutFileDiscoveryTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class MultiColorFlipOutFileDiscoveryService implements IService {

    private MultiColorFlipOutFileDiscoveryTask task;
    private String sessionName;
    private static String DIRECTORY_PARAM_PREFIX = "DIRECTORY_";
    private org.apache.log4j.Logger logger;
    List<String> directoryPathList = new ArrayList<String>();
    AnnotationBeanRemote annotationBean;

    public void execute(IProcessData processData) {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (MultiColorFlipOutFileDiscoveryTask) ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            annotationBean = EJBFactory.getRemoteAnnotationBean();
            Set<Map.Entry<String, Object>> entrySet = processData.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String paramName = entry.getKey();
                if (paramName.startsWith(DIRECTORY_PARAM_PREFIX)) {
                    directoryPathList.add((String) entry.getValue());
                }
            }
            String taskInputDirectoryList = task.getParameter(MultiColorFlipOutFileDiscoveryTask.PARAM_inputDirectoryList);
            if (taskInputDirectoryList != null) {
                String[] directoryArray = taskInputDirectoryList.split(",");
                for (String d : directoryArray) {
                    String trimmedPath=d.trim();
                    if (trimmedPath.length()>0) {
                        directoryPathList.add(trimmedPath);
                    }
                }
            }
            for (String directoryPath : directoryPathList) {
                logger.info(" MultiColorFlipOutFileDiscoveryService including directory = "+directoryPath);
            }
            processDirectories();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    protected void processDirectories() {
        for (String directoryPath : directoryPathList) {
            File dir = new File(directoryPath);
            if (!dir.exists()) {
                logger.error("Directory "+dir.getAbsolutePath()+" does not exist - skipping");
            }
            else if (!dir.isDirectory()) {
                logger.error(("File " + dir.getAbsolutePath()+ " is not a directory - skipping"));
            } else {
                processDirectory(dir);
            }
        }
    }

    protected void processDirectory(File dir) {
        File[] dirContents = dir.listFiles();
        for (File file : dirContents) {
            if (file.isDirectory()) {
                processDirectory(file);
            } else {
                logger.info("Found file = " + file.getAbsolutePath());
                if (file.getName().toUpperCase().endsWith(".LSM")) {
                    considerNewLsmEntity(file);
                }
            }
        }
    }

    protected void considerNewLsmEntity(File file) {
        logger.info("Considering LSM file = " + file.getAbsolutePath());
        List<Entity> possibleLsmFiles = annotationBean.getEntitiesWithFilePath(file.getAbsolutePath());

    }

}

