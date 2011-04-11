/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.control.report;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.Blastable;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.server.access.NodeDAO;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.security.JacsSecurityUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.rmi.RemoteException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jan 30, 2007
 * Time: 3:53:32 PM
 */
public class TaskReportController extends BaseCommandController {
    Logger logger = Logger.getLogger(TaskReportController.class);
    private static final int ONE_HOUR = 1000 * 60 * 60;
    private static final int ONE_DAY = ONE_HOUR * 24;
    private static final int ONE_WEEK = ONE_DAY * 7;
    private static final long BEGINNING_OF_VICSWEB_TIME = 1136073600000L; // from 1/1/2006

    public static final String LAST_HOUR = "last hour";
    public static final String LAST_24_HOURS = "last 24 hours";
    public static final String YESTERDAY = "yesterday";
    public static final String LAST_WEEK = "last week";
    public static final String TODAY = "today";
    public static final String SINGLE_MONTH = "calendar month";

    public static final String[] ALL_PERIODS = new String[]{LAST_HOUR, LAST_24_HOURS, LAST_WEEK, TODAY, YESTERDAY, SINGLE_MONTH};

    public static final String SELECTED_PERIOD = "period";
    public static final String SELECTED_MONTH = "selectedMonth";
    public static final String SELECTED_YEAR = "selectedYear";
    public static final String INCLUDE_SYSTEM_TASK = "includeSystem";

    private TaskDAO _taskDAO;
    private NodeDAO _nodeDAO;
    ComputeBeanRemote _computeBean;

    public void setComputeBean(ComputeBeanRemote computeBean) {
        _computeBean = computeBean;
    }

    public void setTaskDAO(TaskDAO taskDAO) {
        _taskDAO = taskDAO;
    }

    public void setNodeDAO(NodeDAO nodeDAO) {
        _nodeDAO = nodeDAO;
    }

    private void cleanHibernateCache(Task task) {
        Class c = task.getClass();
        _nodeDAO.getSessionFactory().evictCollection(c.getName() + ".inputNodes", task.getObjectId());
        _nodeDAO.getSessionFactory().evictCollection(c.getName() + ".outputNodes", task.getObjectId());
        _nodeDAO.getSessionFactory().evictCollection(c.getName() + ".events", task.getObjectId());
        _nodeDAO.getSessionFactory().evictCollection(c.getName() + ".taskParameterSet", task.getObjectId());
        _nodeDAO.getSessionFactory().evictCollection(c.getName() + ".messages", task.getObjectId());
        _nodeDAO.getSessionFactory().evict(c, task.getObjectId());
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse) throws Exception {
        User user = JacsSecurityUtils.getSessionUser(httpServletRequest);
        String selectedPeriod = RequestUtils.getStringParameter(httpServletRequest, SELECTED_PERIOD, LAST_HOUR);
        Boolean includeSystem = RequestUtils.getBooleanParameter(httpServletRequest, INCLUDE_SYSTEM_TASK, false);
        Date reportStartTime = new Date();
        Date endDate = new Date(); // always now.
        // calcutate start date
        Date startDate = new Date();
        if (LAST_24_HOURS.equals(selectedPeriod)) {
            startDate.setTime(endDate.getTime() - ONE_DAY);
        }
        else if (TODAY.equals(selectedPeriod)) {
            startDate = truncateTime(endDate);
        }
        else if (YESTERDAY.equals(selectedPeriod)) {
            endDate = truncateTime(endDate);
            startDate.setTime(endDate.getTime() - ONE_DAY);
        }
        else if (LAST_WEEK.equals(selectedPeriod)) {
            startDate.setTime(truncateTime(endDate).getTime() - ONE_WEEK); // a little more then a week
        }
        else if (SINGLE_MONTH.equals(selectedPeriod)) {
            int monthNo = RequestUtils.getRequiredIntParameter(httpServletRequest, SELECTED_MONTH);
            int year = RequestUtils.getRequiredIntParameter(httpServletRequest, SELECTED_YEAR);
            GregorianCalendar cal = new GregorianCalendar(year, monthNo, 1);
            startDate = cal.getTime();
            cal.add(Calendar.MONTH, 1);
            endDate = cal.getTime();
        }
        else // default to one hour
        {
            selectedPeriod = LAST_HOUR;
            startDate.setTime(endDate.getTime() - ONE_HOUR);
        }
        String errorMessage = null;
        List<TaskReportInfo> taskInfoList = new ArrayList<TaskReportInfo>();
        Long startId = TimebasedIdentifierGenerator.getUidApproximationOfDate(startDate);
        Long endId = TimebasedIdentifierGenerator.getUidApproximationOfDate(endDate);
        // Execute query to get list of tasks between startId and endId, descending.
        // Add each entry to taskList.
        TaskReportStatistics stats = new TaskReportStatistics();
        List<Task> taskList = _taskDAO.findOrderedTasksByIdRange(startId, endId);
        logger.info("Processing report for '" + selectedPeriod + "' timeframe. Generating details for " + taskList.size() + " tasks");
        for (Task task : taskList) {
            logger.debug("Creating TaskreportInfo for Task " + task.getObjectId());
            try {
                TaskReportInfo taskReportInfo = new TaskReportInfo(task);
                taskInfoList.add(taskReportInfo);
                if (Event.COMPLETED_EVENT.equals(taskReportInfo.getLastStatus().getEventType())) {
                    stats.addReportItem(taskReportInfo);
                }
                cleanHibernateCache(task);

            }
            catch (Throwable t) {
                errorMessage = "There was an error accessing task " + task.getObjectId();
                logger.info(errorMessage);
                logger.error(t);
            }
            logger.debug("TaskReportInfo for Task " + task.getObjectId() + " is created");
        }

        Date reportEndTime = new Date();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(startDate);

        ModelAndView mv = new ModelAndView("TaskReport");
        mv.addObject("curMonth", cal.get(Calendar.MONTH));
        mv.addObject("includeSystem", Boolean.valueOf(includeSystem));
        mv.addObject("now", startDate);
        mv.addObject("allPeriods", ALL_PERIODS);
        mv.addObject(SELECTED_PERIOD, selectedPeriod);
        mv.addObject("errorMessage", errorMessage);
        mv.addObject("taskList", taskInfoList);
        mv.addObject("reportDuration", new Long((reportEndTime.getTime() - reportStartTime.getTime()) / 1000));
        mv.addObject("stats", stats);

        logger.debug("Sending model to view");
        return mv;
    }

    private Date truncateTime(Date dt) {
        // must use calendar now
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(dt);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public class TaskReportStatistics {
        int taskCount = 0;
        long taskDurationTotal = 0L; // miliseconds

        void addReportItem(TaskReportInfo tri) {
            long duration = tri.getLastStatus().getTimestamp().getTime() - tri.getStartTime().getTime();
            addReportItem(duration);
        }

        void addReportItem(long taskDuration) {
            taskCount++;
            taskDurationTotal += taskDuration;
        }

        public int getTaskCount() {
            return taskCount;
        }

        public long getTaskDurationTotal() {
            return taskDurationTotal;
        }

        public String getAvgTaskDuration() {
            if (taskCount > 0)
                return millisecsToString(taskDurationTotal / taskCount);
            else
                return "0";
        }

    }

    /**
     * Report Data Structure
     */
    public class TaskReportInfo {
        long taskID;
        long parentTaskID = -1;
        String taskOwner;
        String lastEventType;
        String taskName;
        String taskParams;
        UserDataNodeVO queryNode;
        List<String> subjectNodes;
        Event lastStatus;
        Date startTime;
        String taskDuration; // in mm.ss  format
        Map<String, String> parameters = new HashMap<String, String>();
        Long numHits;
        String cpuTime = "0";
        long inputSize;
        long outputSize;


        public TaskReportInfo(Task task) throws DaoException, ParameterException, RemoteException {
            this.taskID = task.getObjectId().longValue();
            this.taskOwner = task.getOwner();
            this.lastEventType = task.getLastEvent().getEventType();
            this.taskName = task.getTaskName();
            if (task.getParentTaskId() != null) {
                logger.info("Parent task id of task id " + taskID + " is " + task.getParentTaskId());
                this.parentTaskID = task.getParentTaskId().longValue();
            }

            long tempCpuTime = _computeBean.getCumulativeCpuTime(this.taskID);
            logger.debug("Cumulative CPU time for " + this.taskID + " is " + tempCpuTime);
            this.cpuTime = millisecsToHHMMSSString(tempCpuTime);
            logger.debug("Cumulative CPU time in HHMMSS" + this.cpuTime);

            Event firstEvent = task.getFirstEvent();
            Event lastEvent = task.getLastNonDeletedEvent();

            this.setLastStatus(lastEvent);

            Set paramKeys = task.getParameterKeySet();

            StringBuffer paramsOut = new StringBuffer();
            for (String paramKey : (Set<String>) task.getParameterKeySet()) {
                if (StringUtils.hasText(paramKey)) {
                    ParameterVO value = task.getParameterVO(paramKey);
                    if (value != null) {
                        this.parameters.put(paramKey, value.getStringValue());
                        paramsOut.append(paramKey).append("=").append(value.getStringValue()).append(";");
                    }
                }
            }
            this.setTaskParams(paramsOut.toString());
            logger.debug("Task parameters:" + paramsOut.toString());


            Object objNode = task.getParameterVO(BlastTask.PARAM_query);
            if (objNode != null) {
                ParameterVO queryNodeVO = (ParameterVO) objNode;
                logger.debug("Query Node: " + queryNodeVO.getStringValue());
                if (StringUtils.hasText(queryNodeVO.getStringValue())) {
                    try {
                        Node queryNode = _nodeDAO.getNodeById(Long.parseLong(queryNodeVO.getStringValue()));
                        UserDataNodeVO userNode = new UserDataNodeVO(queryNode.getObjectId() + "",
                                null, null, queryNode.getDataType(), ((Blastable) queryNode).getSequenceType(),
                                null, null, null,
                                TimebasedIdentifierGenerator.getTimestamp(queryNode.getObjectId()), null, null);
                        userNode.setSequenceLength(queryNode.getLength());
                        userNode.setSequenceCount(((Blastable) queryNode).getSequenceCount());
                        setQueryNode(userNode);

                        _nodeDAO.getSessionFactory().evict(queryNode.getClass(), queryNode.getObjectId()); // clean up memory
                    }
                    catch (NumberFormatException nfe) {
                        logger.info("Unable to retrieve node by non-numeric ID ('" + queryNodeVO.getStringValue() + "') for task " + task.getObjectId() + ". Must be a synonym parameter, not a Node ID");
                    }
                    catch (HibernateException e) {
                        logger.info("Eviction error");
                    }
                }
            }
            else {
                logger.debug("Query Node: none");
            }

            Set outputNodes = task.getOutputNodes();
            logger.debug("Output nodes: " + ((outputNodes != null) ? outputNodes.size() : "none"));
            for (Node resultNode : task.getOutputNodes()) {
                if (resultNode.getLength() != null) {
                    this.outputSize += resultNode.getLength();
                }
            }

            if (lastEvent.getEventType().equals(Event.RUNNING_EVENT) || lastEvent.getEventType().equals(Event.POSTPROCESS_EVENT))
                this.setTaskDuration((new Date()).getTime() - firstEvent.getTimestamp().getTime());
            else
                this.setTaskDuration(lastEvent.getTimestamp().getTime() - firstEvent.getTimestamp().getTime());

            this.setStartTime(firstEvent.getTimestamp());

        }

        public String getTaskParams() {
            return taskParams;
        }

        public void setTaskParams(String taskParams) {
            this.taskParams = taskParams;
        }

        public Date getStartTime() {
            return startTime;
        }

        public void setStartTime(Date startTime) {
            this.startTime = startTime;
        }

        public UserDataNodeVO getQueryNode() {
            return queryNode;
        }

        public void setQueryNode(UserDataNodeVO queryNode) {
            this.queryNode = queryNode;
        }

        public List<String> getSubjectNodes() {
            return subjectNodes;
        }

        private void setSubjectNodes(List<Node> subjectNodes) {
            this.subjectNodes = new LinkedList<String>();
            for (Node n : subjectNodes) {
                this.subjectNodes.add(n.getName() + "(" + n.getLength() + ")");
                _nodeDAO.getSessionFactory().evict(n.getClass(), n.getObjectId());
            }
        }

        public Event getLastStatus() {
            return lastStatus;
        }

        public void setLastStatus(Event lastStatus) {
            this.lastStatus = lastStatus;
        }

        public String getTaskDuration() {
            return taskDuration;
        }

        public void setTaskDuration(String taskDuration) {
            this.taskDuration = taskDuration;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }

        public void setTaskDuration(long taskDurationMiliSecs) {
            this.taskDuration = millisecsToHHMMSSString(taskDurationMiliSecs / 1000);
        }

        public long getTaskID() {
            return taskID;
        }

        public String getTaskOwner() {
            return taskOwner;
        }

        public String getLastEventType() {
            return lastEventType;
        }

        public String getTaskName() {
            return taskName;
        }

        public long getParentTaskID() {
            return parentTaskID;
        }

        public void setParentTaskID(long parentTaskID) {
            this.parentTaskID = parentTaskID;
        }

        public String getCpuTime() {
            return cpuTime;
        }

        public void setCpuTime(long cpuTime) {
            this.cpuTime = millisecsToHHMMSSString(cpuTime);
        }

        public void setInputSize(long inputSize) {
            this.inputSize = inputSize;
        }

        public long getInputSize() {
            return inputSize;
        }

        public void setOnputSize(long outputSize) {
            this.outputSize = outputSize;
        }

        public long getOutputSize() {
            return outputSize;
        }
    }

    public static String millisecsToString(long miliSecs) {
        long secs = miliSecs / 1000;
        long mins = secs / 60;
        secs = secs - mins * 60;
        return String.format("%1$d.%2$02d", mins, secs);
    }

    public static String millisecsToHHMMSSString(long secs) {
        return String.format("%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, (secs % 60));

    }

}