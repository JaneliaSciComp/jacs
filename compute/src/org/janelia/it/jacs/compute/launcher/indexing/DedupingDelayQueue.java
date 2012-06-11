package org.janelia.it.jacs.compute.launcher.indexing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import org.apache.log4j.Logger;

/**
 * A delay queue which does not accept duplicates. 
 * 
 * Adapted from http://aredko.blogspot.com/2012/04/using-delayed-queues-in-practice.html
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class DedupingDelayQueue<T> {

	private static Logger logger = Logger.getLogger(DedupingDelayQueue.class);
	
	private long workItemDelay = 5000; // Wait 5 seconds for each item by default

	private final BlockingQueue<PostponedWorkItem<T>> delayed = new DelayQueue<PostponedWorkItem<T>>();

	private int numAdded = 0;
	private int numProcessed = 0;
	
	public long getWorkItemDelay() {
		return workItemDelay;
	}

	public void setWorkItemDelay(long workItemDelay) {
		this.workItemDelay = workItemDelay;
	}

	public void addWorkItem(final T workItem) {
		PostponedWorkItem<T> postponed = new PostponedWorkItem<T>(workItem, workItemDelay);
		synchronized (this) {
			if (!delayed.contains(postponed)) {
				numAdded++;
				delayed.offer(postponed);
			}
		}
	}

	public synchronized int process(int maxBatchSize) {

		Collection<PostponedWorkItem<T>> expired = new ArrayList<PostponedWorkItem<T>>();
		synchronized (this) {
			if (delayed.isEmpty()) return 0;
			logger.info("Draining max "+maxBatchSize +" elements from "+delayed.size()+" total elements");
			delayed.drainTo(expired, maxBatchSize);
			logger.info("Got "+expired.size()+" elements");
		}
		
		List<T> workItems = new ArrayList<T>();
		for (PostponedWorkItem<T> postponed : expired) {
			workItems.add(postponed.getWorkItem());
		}
		
		if (!workItems.isEmpty()) {
			process(workItems);
			numProcessed += workItems.size();
			logger.info("Processing completed. Total offered: "+numAdded+", total processed: "+numProcessed+", current batch: "+expired.size());
		}

		return expired.size();
	}
	
	public abstract void process(List<T> workItems);
	
}