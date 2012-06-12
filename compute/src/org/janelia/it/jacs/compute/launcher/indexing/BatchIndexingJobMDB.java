
package org.janelia.it.jacs.compute.launcher.indexing;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.annotation.ejb.Depends;
import org.jboss.annotation.ejb.ResourceAdapter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * A scheduled job that wakes up every once in a while and runs the next batch of indexing. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MessageDriven(activationConfig = {
        // Wake up every 20 seconds
        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "*/20 * * * * ?")
})
@ResourceAdapter("quartz-ra.rar")
@Depends ({"jboss:custom=IndexingManager"})
public class BatchIndexingJobMDB implements Job {
	
	@Depends({"jboss:custom=IndexingManager"})
	private IndexingManagerManagement indexingManager;
	
	// Eliminate (unneeded) transactions on this method, because they cause some strange JTA exceptions 
	@TransactionAttribute(value = TransactionAttributeType.NEVER)
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    	try {
    		indexingManager.runNextBatch();
    	}
    	catch(Exception e) {
    		throw new JobExecutionException(e);
    	}
    }
}
