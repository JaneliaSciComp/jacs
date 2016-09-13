
package org.janelia.it.jacs.compute.launcher.scheduler;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.io.File;
import java.util.Date;

@MessageDriven(activationConfig = {
        // crontTrigger starts with seconds.  Below should run at the stroke of 2AM EST, every day
        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 0 2 * * ?")
})
//@ResourceAdapter("quartz-ra.rar")
/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 29, 2009
 * Time: 4:30:39 PM
 */
public class DirectoryCleanerMDB implements Job {
    private static final Logger log = Logger.getLogger(DirectoryCleanerMDB.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!SystemConfigurationProperties.getBoolean("ComputeServer.UseScheduler")) {
            return;
        }
        log.info("Waking to clean out the unaccessed uploaded temporary files.");
        try {
            String filestoreUploadDir = SystemConfigurationProperties.getString("Upload.ScratchDir");
            File uploadScratchDir = new File(filestoreUploadDir);
            for (File tmpUploadFile : uploadScratchDir.listFiles()) {
                Date now = new Date();
                long timeSinceModified = now.getTime() - tmpUploadFile.lastModified();
                // If the tmp file was last modified at least 5 hours ago, then nuke it.
                if (timeSinceModified > 18000000) {
                    boolean deletionSuccessful = tmpUploadFile.delete();
                    log.debug("Deletion of file " + tmpUploadFile.getName() + ((deletionSuccessful) ? " was " : " was not ") + "successful.");
                }
            }
        }
        catch (Exception e) {
            log.error("There was a problem profiling the disk usage.", e);
        }
        log.info("Upload directory cleaning complete.");
    }
}