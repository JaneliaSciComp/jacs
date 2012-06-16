package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.entity.MaskAnnotationResultNode;


/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 1/31/12
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */



/*

    This service creates a reference guide for understanding the labeling and 3D structure of a specific mask set
    as run by the mask annotation pattern service.

    The inputs to this service are:

    1) the 3D RGB file specifying the mask.
    2) the name index file which maps the names to the colors in the RGB mask

    The outputs are:

    1) The creation of the top-level "Mask Guide" folder, if it doesn't already exist
    2) The creation of a sub-folder for this particular mask
    3) A 2D MIP and 3D stack for the whole rgb file
    4) A 2D MIP and 3D stack for each compartment, in which the non-compartment volume is "ghosted"

 */


public class MaskGuideService  extends SubmitDrmaaJobService {

    private static final Logger logger = Logger.getLogger(MaskGuideService.class);

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "maskGuideConfiguration.";

    public static final String PARAM_maskFolderName = "Mask Folder Name";
    public static final String PARAM_refresh = "Refresh";

    public static final String NAME_INDEX_FILENAME = "maskNameIndex.txt";
    public static final String RGB_FILENAME = "maskRGB.v3dpbd";

    protected String maskAnnotationTopResourceDir=SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");

    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected User user;
    protected Date createDate;
    protected Task task;
    protected String sessionName;
    protected String visibility;
    protected IProcessData processData;
    protected Boolean refresh;
    protected String maskFolderName;
    protected File maskAnnotationResourceDir;
    protected File guideDir;
    protected File rgbFile;
    protected File maskNameIndexFile;

    protected void init(IProcessData processData) throws Exception {
        try {
            logger.info("MaskGuideService execute() start");
            this.processData = processData;
            task = ProcessDataHelper.getTask(processData);
            logger.info("MaskGuideService running under TaskId=" + task.getObjectId());
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            visibility = User.SYSTEM_USER_LOGIN.equalsIgnoreCase(task.getOwner()) ? Node.VISIBILITY_PUBLIC : Node.VISIBILITY_PRIVATE;
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            refresh = task.getParameter(PARAM_refresh).trim().toLowerCase().equals("true");
            maskFolderName = task.getParameter(PARAM_maskFolderName);

            if (maskFolderName == null) {
                throw new Exception("Mask Folder name must be defined in task");
            } else {
                maskAnnotationResourceDir = new File(maskAnnotationTopResourceDir, maskFolderName);
                logger.info("Using maskAnnotationFolder=" + maskAnnotationResourceDir.getAbsolutePath());
            }

            guideDir = new File(maskAnnotationResourceDir, "guide");
            if (!guideDir.exists()) {
                if (!guideDir.mkdir()) {
                    throw new Exception("Could not create guide dir=" + guideDir.getAbsolutePath());
                }
            }

            maskNameIndexFile = new File(maskAnnotationResourceDir, NAME_INDEX_FILENAME);
            if (!maskNameIndexFile.exists()) {
                throw new Exception("Could not locate expected name index file=" + maskNameIndexFile.getAbsolutePath());
            }
            rgbFile = new File(maskAnnotationResourceDir, RGB_FILENAME);
            if (!rgbFile.exists()) {
                throw new Exception("Could not locate expected RGB file=" + rgbFile.getAbsolutePath());
            }

            MaskAnnotationResultNode maskGuideResultNode = new MaskAnnotationResultNode(task.getOwner(), task, "MaskAnnotationResultNode",
                    "MaskAnnotationResultNode for task " + task.getObjectId(), visibility, sessionName);
            EJBFactory.getLocalComputeBean().saveOrUpdateNode(maskGuideResultNode);
            resultFileNode = maskGuideResultNode;
            super.init(processData);

            logger.info("MaskGuideService execute() end");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    protected String getGridServicePrefixName() {
        return "maskGuide";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        writeInstanceFiles();
        setJobIncrementStop(1);
        createShellScript(writer);
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) {
            throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath());
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix());
        script.append("\n");
        script.append(Vaa3DHelper.getMaskGuideCommand(maskNameIndexFile.getAbsolutePath(), rgbFile.getAbsolutePath(), guideDir.getAbsolutePath()));
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }

    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
        SerializableJobTemplate jt = super.prepareJobTemplate(drmaa);
        jt.setNativeSpecification("-pe batch 4");
        return jt;
    }

    @Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    public void postProcess() throws MissingDataException {
        logger.info("Creating Mask Guide entities");

    }

}
