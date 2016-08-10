package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.base.Splitter;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;

import com.google.common.collect.ImmutableList;

/**
 * Discovers the SAGE samples associated with the given LSM and makes sage loader tasks to
 * process the corresponding samples.
 */
public class LSMSampleInitService extends AbstractDomainService {

    private String datasetName;

    public void execute() throws Exception {
        datasetName = processData.getString("DATASET_NAME");
        List<String> lsmNames = ImmutableList.copyOf(
                Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split((String) processData.getMandatoryItem("LSM_NAMES")));
        logger.info("Sage loader processing for " + ownerKey + "'s " + datasetName);

        DataSet dataSet = getDataSet();
        if (lsmNames.isEmpty()) {
            String message = "No LSMs were found. Not spawning any Sage loading tasks. Aborting pipeline.";
            fail(message);
        }
        else {
            // All LSMs are supposed belong to the same Data Set so Index them in one shot.
            List<Task> sageLoadingTasks = new ArrayList<>();
            // Forcing lab to flylight for now.
            sageLoadingTasks.add(createSageLoaderTask(task.getOwner(), dataSet.getSageConfigPath(), dataSet.getSageGrammarPath(), "flylight", null, lsmNames));
            processData.putItem("SAGE_TASK", sageLoadingTasks);
        }
    }

    private void fail(String message) throws Exception {
        logger.error(message);
        task.addMessage(message);
        computeBean.addEventToTask(task.getObjectId(), new Event(message, new Date(), Event.ERROR_EVENT));
        throw new Exception(message);
    }

    private DataSet getDataSet() throws Exception {
        DomainDAL domainDAL = DomainDAL.getInstance();
        DataSet dataSet = domainDAL.getDataSetByIdentifier(ownerKey, datasetName);
        if (dataSet == null) {
            List<DataSet> dataSets = domainDAL.getUserDomainObjectsByName(ownerKey, DataSet.class, datasetName);
            if (dataSets == null) {
                throw new Exception("Failed to find dataset by name: " + datasetName);
            }
            else if (!(dataSets.size() == 1)) {
                for (DataSet ds: dataSets) {
                    logger.info("Name=" + ds.getName()+", Owner=" + ds.getOwnerName() +  ", ID=" + ds.getId() + ", Identifier=" + ds.getIdentifier());
                }
                throw new Exception(dataSets.size() + " datasets found for name " + datasetName);
            }
            else {
                dataSet = dataSets.get(0);
            }
        }
        return dataSet;
    }

    private SageLoaderTask createSageLoaderTask(String owner, String configPath, String grammarPath, String lab, String line, List<String> slideImageNames) throws DaoException {
        SageLoaderTask sageLoaderTask = new SageLoaderTask(owner,
                new ArrayList<Event>(),
                slideImageNames,
                line,
                configPath,
                grammarPath,
                lab,
                "true",
                null);
        sageLoaderTask.setParentTaskId(task.getObjectId());
        computeBean.saveOrUpdateTask(sageLoaderTask);
        logger.info("Created SageLoaderTask " + sageLoaderTask.getObjectId());
        return sageLoaderTask;
    }

}
