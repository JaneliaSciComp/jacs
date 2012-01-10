
package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 15, 2006
 * Time: 1:04:46 PM
 */
public interface AdministrativeManagerMBean {

    // older methods not used in a while.  would need verification before using
    public void cleanupCoreDumps(String pathToSystemRecruitmentFileDirectory);

    public void cleanupOOSFiles(String systemBlastRootDir);

    public void cleanSystemDirs(String nodeName, boolean debug);

    public void cleanUpUsersAgainstLDAP();

    public void resubmitJobs(String processDefinition, String taskId);

    public void showCurrentGridProcessMap();

}