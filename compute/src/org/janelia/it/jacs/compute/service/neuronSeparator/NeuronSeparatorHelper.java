package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/18/11
 * Time: 10:19 AM
 */
public class NeuronSeparatorHelper {

    public static String getFileListString(NeuronSeparatorPipelineTask task) throws ServiceException {
        String[] lsmPaths = getLSMFilePaths(task);
        return lsmPaths[0] + " , " + lsmPaths[1];
    }

    public static String[] getLSMFilePaths(NeuronSeparatorPipelineTask task) throws ServiceException {
        AnnotationBeanRemote annotationBean = EJBFactory.getRemoteAnnotationBean();
        String lsmEntityList = task.getParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmEntityIdList);

        if (lsmEntityList==null || lsmEntityList.trim().length()==0) {
            throw new ServiceException("PARAM_inputLsmEntityIdList must be populated");
        }

        Entity lsm1;
        Entity lsm2;
        String[] lsmList = lsmEntityList.split(",");
        if (lsmList.length!=2) {
            throw new ServiceException("Expected two files in lsmEntityList="+lsmEntityList);
        }
        lsm1 = annotationBean.getEntityById(lsmList[0].trim());
        lsm2 = annotationBean.getEntityById(lsmList[1].trim());
        if (lsm1 == null || lsm2 == null) {
            throw new ServiceException("Must provide two LSM stack entities.");
        }
        String[] returnList = new String[2];
        returnList[0] = lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        returnList[1] = lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        return returnList;
    }
}
