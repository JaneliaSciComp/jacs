
package org.janelia.it.jacs.compute.launcher.scheduler;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

@MessageDriven(activationConfig = {
        // crontTrigger starts with seconds.  Below should run at the stroke of 2AM EST, every day
        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 0 4 * * ?")
})
//@ResourceAdapter("quartz-ra.rar")
/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 29, 2009
 * Time: 4:30:39 PM
 */
public class ExpiredDataCleanerMDB implements Job {
    private static final Logger log = Logger.getLogger(ExpiredDataCleanerMDB.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!SystemConfigurationProperties.getBoolean("ComputeServer.UseScheduler")) {
            return;
        }
        log.info("Waking to clean out the expired tasks and nodes.");
//        try {
////            String filestoreDir = SystemConfigurationProperties.getString("FileStore.CentralDir");
//            String systemEmail = SystemConfigurationProperties.getString("ComputeServer.SystemEmail");
////            int warningDayAge = SystemConfigurationProperties.getInt("ExpiredDataCleaner.DaysToTriggerWarning");
////            int deletionDayAge = SystemConfigurationProperties.getInt("ExpiredDataCleaner.DaysToTriggerDeletion");
//
//            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
//            List<User> users = computeBean.getAllUsers();
//            // Loop through all the users and get a list of items which are older than 7 days
//            for (User user : users) {
//                MailHelper helper = new MailHelper();
//                helper.sendEmail(systemEmail, user.getEmail(), "VICS Data Expiration Notice", getFormattedEmail());
//            }
//            // Email the user for things exactly warningDayAge days old Warning, deletionDayAge days old scheduled for deletion
//            // Send email
//            // Clean up the items
//        }
//        catch (Exception e) {
//            log.error("There was a problem cleaning the expired data.", e);
//        }
        log.info("Expired data cleaning complete.");
    }

    public String getFormattedEmail() {
        return "Boo!";
    }
}