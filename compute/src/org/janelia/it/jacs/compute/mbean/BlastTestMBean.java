
package org.janelia.it.jacs.compute.mbean;

import javax.management.MXBean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2006
 * Time: 11:22:09 AM
 *
 * @version $Id: BlastTestMBean.java 1 2011-02-16 21:07:19Z tprindle $
 */
@MXBean
public interface BlastTestMBean {

    public void start();

    public void stop();

    public void submitFileNodeQueryBlastNTest();

    public void submitDataNodeQueryBlastNTest();

    public String submitMultipleFileNodeQueryBlastNTest(int numOfJobs, int numOfAlinments);

    public void submitFrvBlastNTest();

    public void saveTaskParameterTest();

    public void updateTasksToParameterStringMap();

    public void deleteBlastResultsOrOrphanDirsForUser(String username, boolean isDebug);

    public void deleteUserBlastNodesOrOrphanDirsBeforeDate(String username, int month, int day, int year, boolean isDebug);

    public void submitSimpleBlastGridTest(String queryPath, String dbPrefixPath, String outputPrefixPath, int numJobs, String queue);

    public void removeFastaAndResultFileNodesForSystem();

    public long getCumulativeCpuTime(long taskId);
//    public void convertBlastXmlToAnotherFormat(String pathToSourceXmlResultFile, String outputFormat, String newFileDestinationDir);
}

