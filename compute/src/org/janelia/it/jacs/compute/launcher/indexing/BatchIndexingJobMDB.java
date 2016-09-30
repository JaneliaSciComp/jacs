
package org.janelia.it.jacs.compute.launcher.indexing;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.DependsOn;
import javax.ejb.MessageDriven;
import javax.ejb.Schedule;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Singleton;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * A scheduled job that wakes up every once in a while and runs the next batch of indexing. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
//@MessageDriven(activationConfig = {
//        // Wake up every 20 seconds
//        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "*/15 * * * * ?")
//})
//@ResourceAdapter("quartz-ra.rar")
@Singleton
@Startup
@DependsOn({"IndexingManager"})
public class BatchIndexingJobMDB implements Job {

	private static final Boolean ENABLE_INDEXING = SystemConfigurationProperties.getBoolean("Solr.EnableIndexing");
	
//	@DependsOn({"jboss:custom=IndexingManager"})
	private IndexingManagerManagement indexingManager;
	
	// Eliminate (unneeded) transactions on this method, because they cause some strange JTA exceptions 
	@Schedule(second="*/15", minute="*", hour="*")
	@TransactionAttribute(value = TransactionAttributeType.NEVER)
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

		// It would be better to disable this bean entirely if this property is false, but unfortunately I don't 
		// have any idea of how to do that since it's scheduled with annotations. So for now this needlessly runs 
		// every 15 seconds and returns without doing anything. 
		if (!ENABLE_INDEXING) return;
		
    	try {
    		indexingManager.runNextBatch();
    	}
    	catch(Exception e) {
    		throw new JobExecutionException(e);
    	}
    }
}
