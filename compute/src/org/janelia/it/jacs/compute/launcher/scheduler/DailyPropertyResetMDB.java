
package org.janelia.it.jacs.compute.launcher.scheduler;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

//@MessageDriven(activationConfig ={
//   // crontTrigger starts with seconds.  Below should run at the stroke of 2AM EST, every day
//   @ActivationConfigProperty(propertyName="cronTrigger", propertyValue="0 0 0 * * ?")
//})

//@ResourceAdapter("quartz-ra.rar")
/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 29, 2009
 * Time: 4:30:39 PM
 */
public class DailyPropertyResetMDB /*implements Job*/ {
    private static final Logger log = Logger.getLogger(DailyPropertyResetMDB.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Waking to reset property values which can become stale.");
        try {
            // Add your properties to scrub here.
            // Project codes should be updated daily.  We generate a list for the front-end oracle.  We also may validate
            // values passed to tasks against this list before executing web service calls.  This takes care of that.
            // Project codes can also be reset from the Admin MBean.
        }
        catch (Exception e) {
            log.error("There was a problem doing daily reset of property values.", e);
        }
        log.info("Daily property values have been reset.");
    }

}