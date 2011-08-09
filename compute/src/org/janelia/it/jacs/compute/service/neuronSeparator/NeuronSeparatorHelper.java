package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;

import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;

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

    public static String getScriptToCreateLsmMetadataFile(NeuronSeparatorResultNode parentNode, String lsmPath) throws ServiceException {

        File lsmFile = new File(lsmPath);
        if (!lsmFile.exists()) {
            throw new ServiceException("Could not find LSM file "+lsmFile.getAbsolutePath());
        }
        File lsmDataFile=new File(parentNode.getDirectoryPath()+"/"+createLsmMetadataFilename(lsmFile)+".metadata");
        String cmdLine = "cd " + parentNode.getDirectoryPath() + ";perl " +
                SystemConfigurationProperties.getString("Executables.ModuleBase") + "singleNeuronTools/lsm_metadata_dump.pl " +
                addQuotes(lsmPath) + " " + addQuotes(lsmDataFile.getAbsolutePath());

        return cmdLine;
    }

    public static String addQuotes(String s) {
    	return "\""+s+"\"";
    }

    public static String addQuotesToCsvString(String csvString) {
        String[] clist=csvString.split(",");
        StringBuffer sb=new StringBuffer();
        for (int i=0;i<clist.length;i++) {
            sb.append("\"");
            sb.append(clist[i].trim());
            sb.append("\"");
            if (i<clist.length-1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
    
    public static String createLsmMetadataFilename(File lsmFile) {
        return lsmFile.getName().replaceAll("\\s+","_");
    }
}
