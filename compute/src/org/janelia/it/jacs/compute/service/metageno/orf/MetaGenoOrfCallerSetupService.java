
package org.janelia.it.jacs.compute.service.metageno.orf;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.metageno.MetageneService;
import org.janelia.it.jacs.compute.service.metageno.RrnaScanService;
import org.janelia.it.jacs.compute.service.metageno.TrnaScanService;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.*;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.metageno.MetaGenoOrfCallerResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 23, 2009
 * Time: 3:55:23 PM
 */

public class MetaGenoOrfCallerSetupService implements IService {

    String setupType = "UNKNOWN";
    String sessionName;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.setupType = (String) processData.getMandatoryItem("SETUP_TYPE");
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            if (setupType.equals("START")) {
                logger.info(this.getClass().getName() + " START execute() start");
                MetaGenoOrfCallerTask parentTask = (MetaGenoOrfCallerTask) ProcessDataHelper.getTask(processData);
                String useClearRange = parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_useClearRange);
                logger.info("useClearRange=" + useClearRange);
                // Create new node
                MetaGenoOrfCallerResultNode resultFileNode = new MetaGenoOrfCallerResultNode(parentTask.getOwner(), parentTask,
                        "MetaGenoOrfCallerResultNode", "MetaGenoOrfCallerResultNode for task " + parentTask.getObjectId(),
                        Node.VISIBILITY_PRIVATE, sessionName);
                resultFileNode = (MetaGenoOrfCallerResultNode) computeBean.saveOrUpdateNode(resultFileNode);
                FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
                FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
                processData.putItem("META_GENO_ORF_CALLER_RESULT_NODE", resultFileNode);
//                List experimentalList=Collections.synchronizedList(new ArrayList());
//                processData.putItem("EXPERIMENTAL_LIST", experimentalList);
                logger.info(this.getClass().getName() + " START execute() finish");
            }
            else if (setupType.equals("PIPELINE")) {
                logger.info(this.getClass().getName() + " PIPELINE execute() start");
                MetaGenoOrfCallerTask parentTask = (MetaGenoOrfCallerTask) ProcessDataHelper.getTask(processData);
                String useClearRange = parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_useClearRange);
                logger.info("useClearRange=" + useClearRange);
                // Create child tasks
                TrnaScanTask tRNAScanTask = TrnaScanService.createDefaultTask();
                setupChildTask(parentTask, tRNAScanTask);
                RrnaScanTask rRNAScanTask = RrnaScanService.createDefaultTask();
                setupChildTask(parentTask, rRNAScanTask);
                SimpleOrfCallerTask simpleOrfCallerTask = SimpleOrfCallerService.createDefaultTask();
                setupChildTask(parentTask, simpleOrfCallerTask);
                populateSimpleOrfCallerParameters(simpleOrfCallerTask, parentTask);
                MetageneTask metageneTask = MetageneService.createDefaultTask();
                setupChildTask(parentTask, metageneTask);
                // We must use EJB rather than Dao here to force commit of task

                tRNAScanTask = (TrnaScanTask) computeBean.saveOrUpdateTask(tRNAScanTask);
                rRNAScanTask = (RrnaScanTask) computeBean.saveOrUpdateTask(rRNAScanTask);
                simpleOrfCallerTask = (SimpleOrfCallerTask) computeBean.saveOrUpdateTask(simpleOrfCallerTask);
                metageneTask = (MetageneTask) computeBean.saveOrUpdateTask(metageneTask);
                // Add tasks to process data object
                processData.putItem(TrnaScanService.TRNA_SCAN_TASK, tRNAScanTask);
                processData.putItem(RrnaScanService.RRNA_SCAN_TASK, rRNAScanTask);
                processData.putItem(SimpleOrfCallerService.SIMPLE_ORF_CALLER_TASK, simpleOrfCallerTask);
                processData.putItem(MetageneService.METAGENE_TASK, metageneTask);
                Thread.sleep(100);
                logger.info(this.getClass().getName() + " PIPELINE execute() finish");
            }
            else {
                throw new Exception("Do not recognize setupType=" + setupType);
            }
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private void setupChildTask(Task parentTask, Task childTask) {
        childTask.setOwner(parentTask.getOwner());
        childTask.setParameter("project", parentTask.getParameter("project"));
        childTask.setParentTaskId(parentTask.getObjectId());
    }

    private void populateSimpleOrfCallerParameters(SimpleOrfCallerTask simpleOrfCallerTask, MetaGenoOrfCallerTask parentTask) {
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_translationTable))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_translation_table, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_translationTable));
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_beginningAsStart))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_beginning_as_start, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_beginningAsStart));
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_endAsStop))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_end_as_stop, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_endAsStop));
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_assumeStops))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_assume_stops, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_assumeStops));
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_fullOrfs))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_full_orfs, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_fullOrfs));
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_minOrfSize))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_min_orf_size, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_minOrfSize));
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_maxOrfSize))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_max_orf_size, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_maxOrfSize));
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_minUnmaskedSize))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_min_unmasked_size, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_minUnmaskedSize));
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_frames))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_frames, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_frames));
        if (parameterIsSet(parentTask, MetaGenoOrfCallerTask.PARAM_forceMethionine))
            simpleOrfCallerTask.setParameter(SimpleOrfCallerTask.PARAM_force_methionine, parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_forceMethionine));
    }

    private boolean parameterIsSet(Task task, String parameter) {
        return task.getParameter(parameter) != null && task.getParameter(parameter).trim().length() > 0;
    }


}
