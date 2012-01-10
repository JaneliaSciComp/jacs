
package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Aug 4, 2008
 * Time: 12:35:57 PM
 */
public interface GridJobsManagerMBean {

    public void cancelTask(long taskId);

    public String printTaskReport(long taskId);

    public String printCurrentReport();

    public String printStatus(long taskId);

    public String printTaskOrder(long taskID);

    public String printPercentComplete(long taskID);
}
