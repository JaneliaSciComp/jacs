package org.janelia.it.jacs.compute.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;


/**
 * A delayed processing queue which does not accept duplicates, thereby eliminating any duplicates that occur within
 * a certain predefined delay time. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DedupingDelayQueue<T> {

	private long workItemDelay = 5000; // Wait 5 seconds for each item by default

	private final BlockingQueue<PostponedWorkItem<T>> delayed = new DelayQueue<PostponedWorkItem<T>>();
	
	/**
	 * Get the delay that is applied to each work item before it can be processed.
	 * @return delay in milliseconds
	 */
	public long getWorkItemDelay() {
		return workItemDelay;
	}

	/**
	 * Set the delay that is applied to each work item before it can be processed.
	 * @param workItemDelay delay in milliseconds
	 */
	public void setWorkItemDelay(long workItemDelay) {
		this.workItemDelay = workItemDelay;
	}

	/**
	 * Add a work item to the queue for processing at some later time.
	 * @param workItem
	 */
	public void addWorkItem(final T workItem) {
		PostponedWorkItem<T> postponed = new PostponedWorkItem<T>(workItem, workItemDelay);
		synchronized (this) {
			if (!delayed.contains(postponed)) {
				delayed.offer(postponed);
			}
		}
	}
	
    /**
     * Returns the current size of the queue.
     * @return
     */
	public int getQueueSize() {
		return delayed.size();
	}
	
	/**
	 * Process as many expired work items in the queue as possible, limited only by the given batch size. 
	 * @param maxBatchSize max number of items to process
	 * @return the number of items that were processed
	 */
	public synchronized int process(int maxBatchSize) {

		Collection<PostponedWorkItem<T>> expired = new ArrayList<PostponedWorkItem<T>>();
		synchronized (this) {
			if (delayed.isEmpty()) return 0;
			delayed.drainTo(expired, maxBatchSize);
		}
		
		List<T> workItems = new ArrayList<T>();
		for (PostponedWorkItem<T> postponed : expired) {
			workItems.add(postponed.getWorkItem());
		}
		
		if (!workItems.isEmpty()) {
			process(workItems);
		}

		return expired.size();
	}
	
	/**
	 * Override this method to provide logic for processing a batch of work items. These items are about to be 
	 * removed from the queue. 
	 * @param workItems list of work items to process
	 */
	public abstract void process(List<T> workItems);
	
}