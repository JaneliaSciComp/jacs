
package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerRecruitmentTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.tasks.recruitment.UserBlastFrvTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.server.access.hibernate.NodeDAOImpl;
import org.janelia.it.jacs.server.access.hibernate.TaskDAOImpl;
import org.janelia.it.jacs.shared.processors.recruitment.AnnotationTableData;
import org.janelia.it.jacs.shared.processors.recruitment.ProjectData;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;
import org.janelia.it.jacs.shared.processors.recruitment.SampleData;
import org.janelia.it.jacs.shared.tasks.RecruitableJobInfo;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;
import org.janelia.it.jacs.web.gwt.common.client.model.tasks.LegendItem;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Apr 30, 2007
 * Time: 4:42:41 PM
 */
public class RecruitmentAPI {
    static Logger logger = Logger.getLogger(RecruitmentAPI.class.getName());
    TaskDAOImpl taskDao;
    NodeDAOImpl dataDao;
    ComputeBeanRemote computeBean;
    public static final String SAMPLE_FILE_NAME = SystemConfigurationProperties.getString("RecruitmentViewer.SampleFile.Name");
    public static final String BASE_FILE_PATH = SystemConfigurationProperties.getString("Reports.Dir");
    public static final int DAYS_TILL_EXPIRATION = SystemConfigurationProperties.getInt("RecruitmentViewer.DaysTillExpiration");

    public RecruitmentAPI() {
    }

    public void setTaskDao(TaskDAOImpl taskDao) {
        this.taskDao = taskDao;
    }

    public void setDataDao(NodeDAOImpl dataDao) {
        this.dataDao = dataDao;
    }

    public void setComputeBean(ComputeBeanRemote computeBean) {
        this.computeBean = computeBean;
    }

    public LegendItem[] getLegend(String nodeId) {
        ArrayList<LegendItem> legendItems = new ArrayList<LegendItem>();
        try {
            FileNode targetNode = (FileNode) dataDao.getNodeById(Long.parseLong(nodeId));
            if (targetNode != null) {
                // Read the legend file
                String tmpFile = FileUtil.getFileContentsAsString(targetNode.getDirectoryPath() + File.separator + RecruitmentResultFileNode.LEGEND_FILENAME);
                String[] items = tmpFile.split("\n");

                // Convert the String[] into ArrayList<LegendItem>
                legendItems = new ArrayList<LegendItem>(items.length);
                for (String item : items) {
                    String[] pieces = item.split("\t");
                    if (pieces == null || pieces.length < 4)
                        logger.warn("got incomplete data on a legend item");
                    else {
                        float[] tmpHSB = Color.RGBtoHSB(Integer.parseInt(pieces[2]), Integer.parseInt(pieces[3]),
                                Integer.parseInt(pieces[4]), null);
                        //if (logger.isDebugEnabled()) logger.debug("Sample is " + pieces[0]);
                        legendItems.add(new LegendItem(pieces[0], pieces[1], pieces[2], pieces[3], pieces[4], tmpHSB[0]));
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Error in RecruitmentAPI attempting to get the legend for node " + nodeId + ": " + e.getMessage());
        }

        return legendItems.toArray(new LegendItem[legendItems.size()]);
    }

    public String getAnnotationInfoForSelection(String nodeId, long ntPosition, String annotationFilter) {
        try {
            RecruitmentFileNode node = (RecruitmentFileNode) dataDao.getNodeById(new Long(nodeId));
            if (null == node) {
                logger.error("There is no RecruitmentFileNode returned for nodeId=" + nodeId);
                return null;
            }
            Task tmpTask = node.getTask();
            // Has to have a gpFile node
            GenomeProjectFileNode gpNode = (GenomeProjectFileNode) dataDao.getNodeById(Long.valueOf(tmpTask.getParameter(RecruitmentViewerTask.GENOME_PROJECT_NODE_ID)));
            String giSourcePath = gpNode.getDirectoryPath() + File.separator + tmpTask.getParameter(RecruitmentViewerRecruitmentTask.GENBANK_FILE_NAME);
//            String giSourcePath = "S:\\filestore\\tsafford\\recruitment\\data\\gi78711884sequence.gb";//node.getFilePathByTag(RecruitmentFileNode.TAG_NCBI_SOURCE);
            if ("".equals(giSourcePath)) {
                return null;
            }
            GenbankFile genbank = new GenbankFile(giSourcePath);
            genbank.populateAnnotations();
            List<String> returnList = genbank.getGenesAtLocation(ntPosition, annotationFilter);
            StringBuffer buf = new StringBuffer();
            buf.append("\n");
            for (String s : returnList) {
                buf.append(s).append("\n");
            }
            if (0 == returnList.size()) {
                return "\nNo annotations found.";
            }
            //if (returnList.size()>=suggestedIndex) {
            return buf.toString();
            //}
        }
        catch (Exception e) {
            logger.error("Error in getAnnotationInfoForSelection\n" + e.getMessage());
        }
        return null;
    }

    public HashMap<ProjectData, ArrayList<SampleData>> getRVSampleData() {
        List sampleList;
        HashMap<String, ProjectData> projectToProjectDataMap = new HashMap<String, ProjectData>();
        HashMap<ProjectData, ArrayList<SampleData>> projectToSampleMap = new HashMap<ProjectData, ArrayList<SampleData>>();
        RecruitmentDataHelper helper;
        try {
            helper = new RecruitmentDataHelper(BASE_FILE_PATH + File.separator + SAMPLE_FILE_NAME);
            sampleList = helper.importSamplesInformation();
            // build the map
            for (Object o : sampleList) {
                SampleData tmpSampleData = (SampleData) o;
                if (!projectToProjectDataMap.keySet().contains(tmpSampleData.getProjectAccession())) {
                    ProjectData tmpProjectData = new ProjectData(tmpSampleData.getProjectAccession(),
                            tmpSampleData.getProjectName());
                    projectToProjectDataMap.put(tmpSampleData.getProjectAccession(), tmpProjectData);
                    projectToSampleMap.put(tmpProjectData, new ArrayList<SampleData>());
                }
                projectToSampleMap.get(projectToProjectDataMap.get(tmpSampleData.getProjectAccession())).add(tmpSampleData);
            }
            // sort the lists
            for (ArrayList<SampleData> o : projectToSampleMap.values()) {
                Collections.sort(o);
            }
        }
        catch (Exception e) {
            logger.error("Error in getRVSampleData\n" + e.getMessage());
        }
        return projectToSampleMap;
    }


    /**
     * Runs the recruitment job and returns the task id in question
     *
     * @param job             - recruitment job detail
     * @param currentUsername - person requesting the recruitment @return return the id of the recruitment task currently running
     * @return returns task id as string
     * @throws Throwable unable to successfully run the recruitment job
     */
    public String runRecruitmentJob(RecruitableJobInfo job, String currentUsername) throws Throwable {
        // if the job is not owned by this user, recruit anew
        String processName;
        RecruitmentViewerFilterDataTask filterDataTask = null;
        try {
            // Case 1: Current user is not the owner of the Recruitment Task (New)
            if (!job.getUsername().equals(currentUsername)) {
                RecruitmentFileNode tmpNode = (RecruitmentFileNode) dataDao.getNodeById(Long.parseLong(job.getRecruitableNodeId()));
                HashSet<RecruitmentFileNode> inputNodes = new HashSet<RecruitmentFileNode>();
                inputNodes.add(tmpNode);
                RecruitmentViewerRecruitmentTask tmpTask = (RecruitmentViewerRecruitmentTask) tmpNode.getTask();
                filterDataTask = new RecruitmentViewerFilterDataTask(inputNodes,
                        currentUsername, null,
                        new HashSet<TaskParameter>(), job.getSubjectName(),
                        job.getQueryName(), job.getNumHits(),
                        job.getPercentIdentityMin(), job.getPercentIdentityMax(), job.getRefAxisBeginCoord(),
                        Double.parseDouble(tmpTask.getParameter(RecruitmentViewerRecruitmentTask.GENOME_SIZE)),
                        job.getSamplesRecruitedAsList(),
                        job.getMateInfo(),
                        job.getAnnotationFilterString(),
                        job.getMateSpanPoint(),
                        job.getColorizationType());
                // Copy all the old parameters into the new task
                for (TaskParameter param : tmpTask.getTaskParameterSet()) {
                    filterDataTask.setParameter(param.getName(), param.getValue());
                }

                // Set expirationDate
                GregorianCalendar expirationDate = new GregorianCalendar();
                expirationDate.add(GregorianCalendar.DAY_OF_MONTH, 1);
                filterDataTask.setExpirationDate(expirationDate.getTime());
                processName = "FrvNovelNonGrid";
                // save the task and then execute
                filterDataTask = (RecruitmentViewerFilterDataTask) taskDao.saveOrUpdateTask(filterDataTask);
                computeBean.submitJob(processName, filterDataTask.getObjectId());
                return filterDataTask.getObjectId().toString();
            }
            // Case 2: Current user owns the Recruitment Task (Targeted Update)
            else {
                filterDataTask = (RecruitmentViewerFilterDataTask) taskDao.getTaskById(Long.parseLong(job.getJobId()));
                boolean dataChanged = false;
                processName = "FrvDataOnlyNonGrid";
                if (null != filterDataTask.getParameter(RecruitmentViewerFilterDataTask.SAMPLE_LIST) &&
                        !filterDataTask.getParameter(RecruitmentViewerFilterDataTask.SAMPLE_LIST).equals(job.getSamplesRecruited())) {
                    logger.info("Sample list changed");
                    dataChanged = true;
                    filterDataTask.setParameter(RecruitmentViewerFilterDataTask.SAMPLE_LIST, job.getSamplesRecruited());
                }
                if (null != filterDataTask.getParameter(RecruitmentViewerFilterDataTask.MATE_BITS) &&
                        !filterDataTask.getParameter(RecruitmentViewerFilterDataTask.MATE_BITS).equals(job.getMateInfo())) {
                    logger.info("Mate bits changed");
                    dataChanged = true;
                    filterDataTask.setParameter(RecruitmentViewerFilterDataTask.MATE_BITS, job.getMateInfo());
                }
                if (null != filterDataTask.getParameter(RecruitmentViewerFilterDataTask.MATE_SPAN_POINT) &&
                        !filterDataTask.getParameter(RecruitmentViewerFilterDataTask.MATE_SPAN_POINT).equals(job.getMateSpanPoint())) {
                    logger.info("Mate Span Point changed");
                    dataChanged = true;
                    filterDataTask.setParameter(RecruitmentViewerFilterDataTask.MATE_SPAN_POINT, job.getMateSpanPoint());
                }
                if (null != filterDataTask.getParameter(RecruitmentViewerFilterDataTask.COLORIZATION_TYPE) &&
                        !filterDataTask.getParameter(RecruitmentViewerFilterDataTask.COLORIZATION_TYPE).equals(job.getColorizationType())) {
                    logger.info("Colorization Type changed");
                    dataChanged = true;
                    filterDataTask.setParameter(RecruitmentViewerFilterDataTask.COLORIZATION_TYPE, job.getColorizationType());
                }
                if (!job.getAnnotationFilterString().equalsIgnoreCase(filterDataTask.getParameter(RecruitmentViewerFilterDataTask.ANNOTATION_FILTER_STRING))) {
                    logger.info("Annotation filter changed");
                    filterDataTask.setParameter(RecruitmentViewerFilterDataTask.ANNOTATION_FILTER_STRING, job.getAnnotationFilterString());
                    if (dataChanged) {
                        processName = "FrvDataAndAnnotationsNonGrid";
                    }
                    else {
                        processName = "FrvAnnotationsOnlyNonGrid";
                        dataChanged = true;
                    }
                }
                if (dataChanged) {
                    // save the update task and then execute
                    filterDataTask.addEvent(new org.janelia.it.jacs.model.tasks.Event("Resubmitting the task for processing", new Date(), org.janelia.it.jacs.model.tasks.Event.RESUBMIT_EVENT));
                    filterDataTask = (RecruitmentViewerFilterDataTask) taskDao.saveOrUpdateTask(filterDataTask);
                    computeBean.submitJob(processName, filterDataTask.getObjectId());
                    return filterDataTask.getObjectId().toString();
                }
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
            logger.error("Error processing submitRecruitmentJob");
            try {
                // Make an effort to log the error...
                if (null != filterDataTask) {
                    filterDataTask.addEvent(new Event("Error trying to process recruitment task", new Date(), Event.ERROR_EVENT));
                    taskDao.saveOrUpdateTask(filterDataTask);
                }
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
            throw e;
        }
        logger.debug("Recruitment job submitted without any changes.");
        return Long.valueOf(job.getJobId()).toString();
    }

    public List<AnnotationTableData> getAnnotationInfoForRange(String nodeId, long ntStartPosition, long ntStopPosition, String annotationFilter) {
        try {
            RecruitmentFileNode node = (RecruitmentFileNode) dataDao.getNodeById(new Long(nodeId));
            if (null == node) {
                logger.error("There is no RecruitmentFileNode returned for nodeId=" + nodeId);
                return null;
            }
            Task tmpTask = node.getTask();
            // Has to have a gpFile node
            GenomeProjectFileNode gpNode = (GenomeProjectFileNode) dataDao.getNodeById(Long.valueOf(tmpTask.getParameter(RecruitmentViewerTask.GENOME_PROJECT_NODE_ID)));
            String giSourcePath = gpNode.getDirectoryPath() + File.separator + tmpTask.getParameter(RecruitmentViewerRecruitmentTask.GENBANK_FILE_NAME);
            if ("".equals(giSourcePath)) {
                return null;
            }
            GenbankFile genbank = new GenbankFile(giSourcePath);
            genbank.populateAnnotations();
            List<String> entries = genbank.getGenesInRange(ntStartPosition, ntStopPosition, annotationFilter);
            ArrayList<AnnotationTableData> returnList = new ArrayList<AnnotationTableData>();
            for (Object entry : entries) {
                String entryString = (String) entry;
                long tmpBegin = genbank.getAnnotationCoordinate(entryString, true);
                long tmpEnd = genbank.getAnnotationCoordinate(entryString, false);
                returnList.add(new AnnotationTableData(genbank.getProteinIdForEntry(entryString), genbank.getDBXRefIdForEntry(entryString),
                        genbank.getProductForEntry(entryString), Long.toString(tmpBegin),
                        Long.toString(tmpEnd),
                        (genbank.entryIsOnForwardStrand(entryString) ? "+" : "-"), Long.toString(Math.abs(tmpEnd - tmpBegin))));
            }
            return returnList;
        }
        catch (Exception e) {
            logger.error("Error in getAnnotationInfoForRange\n" + e.getMessage());
        }
        return null;
    }

    public void runUserBlastRecruitment(String queryNodeId, String sessionUser) throws Throwable {
        Node inputNode = dataDao.getNodeById(Long.valueOf(queryNodeId));
        HashSet<Node> inputNodes = new HashSet<Node>();
        inputNodes.add(inputNode);
        UserBlastFrvTask userTask = new UserBlastFrvTask(inputNodes, sessionUser, new ArrayList(), new HashSet());
        userTask.setJobName("FRV for " + inputNode.getName());
        // todo This needs to be exposed to the user or provided.
        userTask.setParameter(Task.PARAM_project, "08020");
        taskDao.saveOrUpdateTask(userTask);
        try {
            computeBean.submitJob("UserBlastFRVGrid", userTask.getObjectId());
        }
        catch (Throwable e) {
            logger.error("Error in runUserBlastRecruitment\n" + e.getMessage());
            userTask.addEvent(new Event("Error submitting to the User FRV pipeline", new Date(), Event.ERROR_EVENT));
            taskDao.saveOrUpdateTask(userTask);
            throw e;
        }
    }
}
