package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MultiColorFlipOutFileDiscoveryTask;

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

    public void execute(IProcessData processData) {
        try {
            this.task = (MultiColorFlipOutFileDiscoveryTask)ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            Set<Map.Entry<String, Object>> entrySet = processData.entrySet();
            List<String> directoryPathList = new ArrayList<String>();
            for (Map.Entry<String, Object> entry : entrySet) {
                String paramName = entry.getKey();
                if (paramName.startsWith(DIRECTORY_PARAM_PREFIX)) {
                    directoryPathList.add((String)entry.getValue());
                }
            }
            String taskInputDirectoryList=task.getParameter(MultiColorFlipOutFileDiscoveryTask.PARAM_inputDirectoryList);
            if (taskInputDirectoryList!=null) {
                String[] directoryArray=taskInputDirectoryList.split(",");
                for (String d : directoryArray) {
                    directoryPathList.add(d.trim());
                }
            }
        }
        catch (Exception e) {
        }
    }
}

