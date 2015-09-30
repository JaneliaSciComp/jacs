
package org.janelia.it.jacs.compute.service.recruitment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.recruitment.GenomeProjectBlastFrvTask;
import org.janelia.it.jacs.model.tasks.recruitment.GenomeProjectRecruitmentSamplingTask;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;
import org.janelia.it.jacs.shared.tasks.GenbankFileInfo;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 16, 2010
 * Time: 10:07:27 AM
 * NOTE:  This service assumes the /sample_name on the defline and in the statistics file is the same as
 * the blast db name for that library.
 */
public class FrvRecruitFromStatisticsService implements IService {

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        FileWriter writer=null;
        try {
            Logger _logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            GenomeProjectRecruitmentSamplingTask task = (GenomeProjectRecruitmentSamplingTask) ProcessDataHelper.getTask(processData);
            EJBFactory.getRemoteComputeBean().saveEvent(task.getObjectId(), "Full Recruitment Running", "Full Recruitment Running", new Date());
            List<String> tmpDbList = Task.listOfStringsFromCsvString(task.getParameter(GenomeProjectRecruitmentSamplingTask.BLASTABLE_DATABASE_NODES));
            HashMap<String, String> blastNameToIdMap = new HashMap<String, String>();
            for (String tmpDb : tmpDbList) {
                String tmpDbName = EJBFactory.getLocalComputeBean().getNodeById(Long.valueOf(tmpDb)).getName();
                blastNameToIdMap.put(tmpDbName, tmpDb);
            }
            RecruitmentFileNode resultNode = (RecruitmentFileNode) EJBFactory.getLocalComputeBean().getNodeById(processData.getLong(ProcessDataConstants.RECRUITMENT_FILE_NODE_ID));
            File statsFile = new File(resultNode.getFilePathByTag(RecruitmentFileNode.TAG_STATS));
            File recruitingFile = new File(resultNode.getFilePathByTag(RecruitmentFileNode.TAG_RECRUITMENT_TASKS));

            // If no good list provided, exit
            if (null==blastNameToIdMap || blastNameToIdMap.keySet().size()<=0) {
                System.out.println("No list of blast databases provided for update.  Exiting...");
                throw new ServiceException("No blast databases are listed for recruiting against.("+task.getObjectId()+")");
            }
            // Update the sample.info file
            //System.out.println("Updating the sample.info file");
            // Commented out because the database doesn't have new sample/project/library data
            // todo This situation should be fixed
            // generateSampleInfoFile();

            // Now run the pipeline
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            HashMap<String, GenbankFileInfo> genbankFiles = RecruitmentDataHelper.getGenbankFileMap();
            HashMap<String, String> recruitingSetofGbksAndTaskMap = new HashMap<String, String>();
            Subject tmpUser = computeBean.getSubjectByNameOrKey(task.getOwner());
            Scanner scanner = new Scanner(statsFile);
            ArrayList<String> rowList = new ArrayList<String>();
            int moleculeNameIndex=0,expectedCoverageIndex=6,totalHitsIndex=7,sampleNameIndex=8;
            while (scanner.hasNextLine()){
                HashSet<String> targetDbIdSet = new HashSet<String>();
                String tmpLine = scanner.nextLine().trim();
                rowList.add(tmpLine);
                // This triggers the looping for all rows of a given molecule - aggregateSamples should ALWAYS be last row
                boolean moleculeComplete = tmpLine.indexOf("aggregateSamples")>=0;
                if (moleculeComplete){
                    String tmpGbkFileName="";
                    for (String row : rowList) {
                        String[] values = row.split("\t");
                        double comparisonValue = 0.5;
                        double coverageValue = Double.parseDouble(values[expectedCoverageIndex]);
                        boolean lastRow = values[sampleNameIndex].equals("aggregateSamples");
                        if (lastRow){
                            // Set the molecule name on the last row
                            tmpGbkFileName = values[moleculeNameIndex];
                            comparisonValue = 0.5;
                        }
                        if (coverageValue>=comparisonValue && Long.valueOf(values[totalHitsIndex])>=10){
                            // If allSamples passes clear the set and take all dbs
                            if (lastRow) {
                                targetDbIdSet.clear();
                                targetDbIdSet.addAll(tmpDbList);
                            }
                            // Build a set of individual dbs which pass the test
                            else {
                                targetDbIdSet.add(blastNameToIdMap.get(values[sampleNameIndex]));
                            }
                        }
                    }
                    // Clear the row data
                    rowList.clear();
                    // Figure out what exactly to blast
                    String summary = "";
                    if (targetDbIdSet.size()>0) {
                        String blastDBCommaList = Task.csvStringFromCollection(new ArrayList<String>(targetDbIdSet));
                        summary+=", Recruiting "+tmpGbkFileName+" against "+blastDBCommaList+"...";
                        // For each Genbank file, run blast, import recruitment file node, and recruitment result file node
                        GenbankFileInfo genbankFileInfo = genbankFiles.get(tmpGbkFileName);
                        try {
                            String genbankFileName = genbankFileInfo.getGenbankFile().getName();
//                            String frvFilterTaskId = computeBean.getRecruitmentFilterDataTaskForUserByGenbankId(genbankFileName,
//                                    tmpUser.getUserLogin());
                            // If this is a new GenomeProject, build the filterDataTask
                            Task runningTask;
//                            if (null==frvFilterTaskId){
                                System.out.println("Running GenomeProjectBlastFrvTask for: " + genbankFileInfo.getGenbankFile().getName());
                                // For the Genbank file, run blast, import recruitment file node, and recruitment result file node
                                GenomeProjectBlastFrvTask tmpTask = new GenomeProjectBlastFrvTask(
                                        genbankFileInfo.getGenomeProjectNodeId().toString(),
                                        genbankFileInfo.getGenbankFile().getName(),
                                        blastDBCommaList,
                                        null,
                                        tmpUser.getName(),
                                        new ArrayList<Event>(),
                                        new HashSet<TaskParameter>());
                                tmpTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
                                tmpTask.setParentTaskId(task.getObjectId());
                                tmpTask = (GenomeProjectBlastFrvTask) computeBean.saveOrUpdateTask(tmpTask);
                                // Submit the job
                                computeBean.submitJob("GenomeProjectBlastFRVGrid", tmpTask.getObjectId());
                                runningTask = tmpTask;
//                            }
//                            else {
//                                System.out.println("Running BlastFRVUpdate for: " + genbankFileInfo.getGenbankFile().getName());
//                                RecruitmentViewerFilterDataTask filterTask = (RecruitmentViewerFilterDataTask) computeBean.getTaskById(Long.valueOf(frvFilterTaskId));
//                                System.out.println("Found " + genbankFileName + " recruited and filtered for user " + tmpUser.getUserLogin() + ". Blast-Recruiting new data...");
//                                GenomeProjectBlastFrvUpdateTask tmpTask = new GenomeProjectBlastFrvUpdateTask(filterTask.getObjectId().toString(),
//                                        blastDBCommaList,
//                                        genbankFileInfo.getGenomeProjectNodeId().toString(),
//                                        genbankFileName, null, tmpUser.getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>());
//                                tmpTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
//                                tmpTask.setParentTaskId(task.getObjectId());
//                                tmpTask = (GenomeProjectBlastFrvUpdateTask) computeBean.saveOrUpdateTask(tmpTask);
//                                // Submit the job
//                                computeBean.submitJob("GenomeProjectBlastFRVUpdateGrid", tmpTask.getObjectId());
//                                runningTask = tmpTask;
//                            }
                            recruitingSetofGbksAndTaskMap.put(tmpGbkFileName, runningTask.getObjectId().toString());
                            // Don't want to overwhelm JDBC or the Grid processing upon start-up
                            Thread.sleep(250);
                        }
                        catch (RemoteException e) {
                            _logger.error("Could not generate data for " + genbankFileInfo.getGenbankFile().getName(), e);
                        }
                    }
                    else {
                        summary+=", NOT Recruiting "+tmpGbkFileName+"...";
                    }
                    targetDbIdSet.clear();
                    _logger.debug(summary);
                }
            }
            writer = new FileWriter(recruitingFile);
            for (String recruitmentEntry : recruitingSetofGbksAndTaskMap.keySet()) {
                //_logger.debug("GBK: "+recruitmentEntry+", Task: "+recruitingSetofGbksAndTaskMap.get(recruitmentEntry));
                writer.append(recruitmentEntry).append("\t").append(recruitingSetofGbksAndTaskMap.get(recruitmentEntry)).append("\n");
            }
        }
        catch (Exception e) {
            throw new ServiceException("Error running the FrvRecruitFromStatisticsService");
        }
        finally {
            if (null!=writer){
                try {
                    writer.close();
                }
                catch (IOException e) {
                    System.err.println("Problem closing the "+RecruitmentFileNode.TAG_RECRUITMENT_TASKS+" file.");
                    e.printStackTrace();
                }
            }
        }
    }
}
