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

package org.janelia.it.jacs.compute.drmaa;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.api.JobControlBeanRemote;
import org.janelia.it.jacs.compute.service.common.grid.submit.GridProcessResult;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.RemoteJobStatusLogger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: May 4, 2009
 * Time: 6:47:59 PM
 */
public class DrmaaSubmitter {
    private static Logger logger;

    // program parameter options
    public static final String OPT_TEMPLATE_FILE = "template";
    public static final String OPT_TASK_ID = "task_id";
    public static final String OPT_SUBMISSION_KEY = "key";
    public static final String OPT_JOB_ARRAY_PARAMS = "bulk";
    public static final String OPT_LOOP_SLEEP_PERIOD = "loop_sleep";
    public static final String OPT_RETURN_BY = "return";
    public static final String OPT_RETURN_VIA_QUEUE_VAL = "queue";
    public static final String OPT_RETURN_VIA_SYSTEM_VAL = "system";
    public static final String DELIMETER = "=";

    protected static final int MAX_JOBS_IN_ARRAY = SystemConfigurationProperties.getInt("Grid.MaxNumberOfJobs");

    private SubmitterParams params;
    private DrmaaHelper drmaa;
    private String sgeQueueName = "default";

    public DrmaaSubmitter(SubmitterParams p) throws DrmaaException {
        params = p;
        // initialize session
        drmaa = new DrmaaHelper(logger);
        drmaa.getVersion(); // to test it!
    }

    public Set<String> submitJob() throws Exception {
        Set<String> jobs = new HashSet<String>();
        SerializableJobTemplate jt = drmaa.loadTemplate(params.jtFileName);
        // figure out and store queue name
        // for SGE queue is defined by option -l
        String nativeSpec = jt.getNativeSpecification();
        if (nativeSpec != null) {
            Pattern p = Pattern.compile("\\-l\\s+(\\w+)");
            Matcher m = p.matcher(nativeSpec);
            if (m.matches())
                sgeQueueName = m.group(1);
        }
        if (params.start == -1) {
            String jobID = drmaa.runJob(jt);
            jobs.add(jobID);
        }
        else // bulk job
        {
            /* DRMAA has a limitation of 75000 jobs in a single array
               Submittion in a loop is a work around for this limitation
              */
            for (int startIdx = params.start, endIdx = 0; startIdx <= params.end; startIdx += MAX_JOBS_IN_ARRAY) {
                endIdx += MAX_JOBS_IN_ARRAY;
                if (endIdx > params.end) endIdx = params.end;

                jobs.addAll(drmaa.runBulkJobs(jt, startIdx, endIdx, 1));
            }

//            jobs = drmaa.runBulkJobs(jt, params.start, params.end, params.incr);
        }
        // remove template
        drmaa.deleteJobTemplate(jt);
        return jobs;
    }

    public static void main(String[] args) {
        // parse parameters
        SubmitterParams params = null;
        try {
            params = new SubmitterParams(args);
        }
        catch (Throwable t) {
            System.err.println(t.getMessage());
            System.exit(1);
        }
        // log file name will be template file base name with log extention
        File fileHandle = new File(params.jtFileName);
        String logFileName = fileHandle.getParentFile().getAbsolutePath() + File.separator + "DrmaaSubmitter.log";

        configureLog4j(logFileName);
        logger = Logger.getLogger(DrmaaSubmitter.class);

        // load Spring application context which has beans for EJB and JMS access
        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath*:drmaaSubmitterApp.xml");
        JobControlBeanRemote jobControlBean = (JobControlBeanRemote) ctx.getBean("remoteJobControlService");
        try {
            DrmaaSubmitter ds = new DrmaaSubmitter(params);
            Set<String> jobs = ds.submitJob();
            String mainJobID = jobs.iterator().next();
            if (jobs.size() > 1) {
                mainJobID = mainJobID.substring(0, mainJobID.lastIndexOf('.')) + ".N";
            }
            // now wait for jobs to be completed
            // Instantiate Status Logger
            JobStatusLogger jsl;
            if (params.notificationMethod.equalsIgnoreCase(OPT_RETURN_VIA_QUEUE_VAL))
                jsl = new RemoteJobStatusLogger(params.taskId, jobControlBean, logger);
            else
                jsl = new TextJobStatusLogger(System.out);

            // store statuses
            jsl.bulkAdd(jobs, ds.sgeQueueName, GridJobStatus.JobState.QUEUED);

            logger.info("******** " + jobs.size() + " jobs (" + mainJobID + ") submitted to grid **********");
            boolean gridActionSuccessful = ds.drmaa.waitForJobs(jobs, "Computing results: ", jsl, params.loopSleep);
            GridProcessResult gpr = new GridProcessResult(params.taskId, gridActionSuccessful);
            gpr.setGridSubmissionKey(params.submissionKey);
            if (!gridActionSuccessful) {
                //
                logger.error("\nThere was an error with the grid execution: " + ds.drmaa.getError() + " \n");
                gpr.setError(ds.drmaa.getError());
            }

            String output = "Job completed. Results: \n" + gpr.toString();
            // return results
            if (params.notificationMethod.equalsIgnoreCase(OPT_RETURN_VIA_QUEUE_VAL)) {
                JmsTemplate jmsTemplate = (JmsTemplate) ctx.getBean("queueTemplate");
                jmsTemplate.send(new StatusMessageCreator(gpr));
                logger.info("Reply message posted to " + jmsTemplate.getDefaultDestinationName());
            }
            else {
                logger.info("Reply sent to STDOUT");
                System.out.println(output);
            }
            logger.info(output);

            System.exit(gridActionSuccessful ? 0 : 200);
        }
        catch (Throwable t) {
            logger.error("DrmaaSubmit Error: " + t.getMessage(), t);
            System.err.println(t.getClass().getName() + ": " + t.getMessage());
            System.exit(1);
        }
        finally {
            LogManager.shutdown();
        }
    }

    private static void configureLog4j(String logFileName) {
        Properties log4jProps = new Properties();
        log4jProps.setProperty("log4j.rootLogger", "ERROR, A1");
        log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.FileAppender");
        log4jProps.setProperty("log4j.appender.A1.File", logFileName);
        log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d [%t] %-5p %c - %m%n");
        log4jProps.setProperty("log4j.logger.org.janelia.it.jacs", "WARN");
        log4jProps.setProperty("log4j.logger.org.janelia.it.jacs.compute.drmaa", "INFO");
        log4jProps.setProperty("log4j.logger.org.jboss", "ERROR");
        log4jProps.setProperty("log4j.logger.org.hibernate", "ERROR");
        PropertyConfigurator.configure(log4jProps);
    }
}

class StatusMessageCreator implements MessageCreator {
    private Serializable message;

    public StatusMessageCreator(Serializable m) {
        message = m;
    }

    public Message createMessage(Session session) throws JMSException {
        return session.createObjectMessage(message);
    }
}

class SubmitterParams {
    // template file location
    String jtFileName;

    // bulk job parameters
    int start = -1;
    int end = -1;
    int incr = -1;

    // seconds to sleep in the waiting loop
    // -1 forces default incremental sleep
    int loopSleep = -1;

    // task id for status updates
    long taskId = -1;

    // notification Method for reporting back
    String notificationMethod = DrmaaSubmitter.OPT_RETURN_VIA_SYSTEM_VAL;
    String queueName;
    String submissionKey = "NONE";


//    private String usage() {
//        return ("usage: template=<file name for serilized JobTemplate> [bulk=<start,end,increment>] - values for job array " +
//                " [return=<system|queue>] [loop_sleep=<num of sec>\n"
//                + "default values: return=system bulk=-1,-1,-1 loop_sleep=-1");
//    }
//
    public SubmitterParams(String[] args) {
        for (String arg : args) {
            String[] nameValuePair = arg.split(DrmaaSubmitter.DELIMETER);
            if (DrmaaSubmitter.OPT_TEMPLATE_FILE.equalsIgnoreCase(nameValuePair[0])) {
                jtFileName = nameValuePair[1];
                File f = new File(jtFileName);
                if (!f.isFile())
                    throw new IllegalArgumentException(jtFileName + " does not exist or not a regular file");
            }
            else if (DrmaaSubmitter.OPT_JOB_ARRAY_PARAMS.equalsIgnoreCase(nameValuePair[0])) {
                setBulkParams(nameValuePair[1]);
            }
            else if (DrmaaSubmitter.OPT_RETURN_BY.equalsIgnoreCase(nameValuePair[0])) {
                notificationMethod = nameValuePair[1];
                if (!(DrmaaSubmitter.OPT_RETURN_VIA_SYSTEM_VAL.equalsIgnoreCase(notificationMethod) ||
                        DrmaaSubmitter.OPT_RETURN_VIA_QUEUE_VAL.equalsIgnoreCase(notificationMethod))) {
                    throw new IllegalArgumentException(DrmaaSubmitter.OPT_RETURN_BY + " must be set to either system or queue values");
                }
            }
            else if (DrmaaSubmitter.OPT_LOOP_SLEEP_PERIOD.equalsIgnoreCase(nameValuePair[0])) {

            }
            else if (DrmaaSubmitter.OPT_SUBMISSION_KEY.equalsIgnoreCase(nameValuePair[0])) {
                submissionKey = nameValuePair[1];
            }
            else if (DrmaaSubmitter.OPT_TASK_ID.equalsIgnoreCase(nameValuePair[0])) {
                try {
                    taskId = Long.parseLong(nameValuePair[1]);
                }
                catch (Throwable t) {
                    throw new IllegalArgumentException(DrmaaSubmitter.OPT_TASK_ID + " must be set to long value");
                }
            }
        }
    }

    private void setBulkParams(String bulkParamValue) {
        String[] vals = bulkParamValue.split(",");
        try {
            start = Integer.parseInt(vals[0]);
            end = Integer.parseInt(vals[1]);
            incr = Integer.parseInt(vals[2]);
        }
        catch (Throwable t) {
            throw new IllegalArgumentException("Value for parameter '" + DrmaaSubmitter.OPT_JOB_ARRAY_PARAMS + "' must be 3 integer values separated by commas");
        }
    }
}
