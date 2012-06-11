package org.janelia.it.jacs.compute.launcher.indexing;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * An object with a delay. 
 * 
 * Adapted from
 * http://aredko.blogspot.com/2012/04/using-delayed-queues-in-practice.html
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PostponedWorkItem<T> implements Delayed {

	private final long origin;
	private final long delay;
	private final T workItem;

	public PostponedWorkItem(final T workItem, final long delay) {
		this.origin = System.currentTimeMillis();
		this.workItem = workItem;
		this.delay = delay;
	}

	public T getWorkItem() {
		return workItem;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(delay - (System.currentTimeMillis() - origin), TimeUnit.MILLISECONDS);
	}

	@Override
	public int compareTo(Delayed delayed) {
		if (delayed == this) {
			return 0;
		}

		if (delayed instanceof PostponedWorkItem) {
			long diff = delay - ((PostponedWorkItem) delayed).delay;
			return ((diff == 0) ? 0 : ((diff < 0) ? -1 : 1));
		}

		long d = (getDelay(TimeUnit.MILLISECONDS) - delayed.getDelay(TimeUnit.MILLISECONDS));
		return ((d == 0) ? 0 : ((d < 0) ? -1 : 1));
	}

	@Override
	public int hashCode() {
		final int prime = 31;

		int result = 1;
		result = prime * result + ((workItem == null) ? 0 : workItem.hashCode());

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof PostponedWorkItem)) {
			return false;
		}

		final PostponedWorkItem other = (PostponedWorkItem) obj;
		if (workItem == null) {
			if (other.workItem != null) {
				return false;
			}
		} else if (!workItem.equals(other.workItem)) {
			return false;
		}

		return true;
	}
}
