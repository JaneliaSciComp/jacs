
package org.janelia.it.jacs.shared.tasks;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 7, 2010
 * Time: 10:46:18 AM
 */
public class RnaSeqJobInfo extends JobInfo {

    private String jobResultsDirectoryPath;

    /* This class is a place-holder for more information relating to RnaSeq jobs */

    public RnaSeqJobInfo() {
        super();
    }

    public String getJobResultsDirectoryPath() {
        return jobResultsDirectoryPath;
    }

    public void setJobResultsDirectoryPath(String jobResultsDirectoryPath) {
        this.jobResultsDirectoryPath = jobResultsDirectoryPath;
    }

}
