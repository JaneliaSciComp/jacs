package org.janelia.jacs2.asyncservice.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceEvent;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.janelia.jacs2.model.page.PageRequest;
import org.janelia.jacs2.model.page.PageResult;
import org.janelia.jacs2.model.page.SortCriteria;
import org.janelia.jacs2.model.page.SortDirection;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

@ApplicationScoped
public class InMemoryJacsServiceQueue implements JacsServiceQueue {
    private static final int DEFAULT_MAX_READY_CAPACITY = 20;

    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private Queue<JacsServiceData> waitingServices;
    private Set<Number> waitingServicesSet = new LinkedHashSet<>();
    private Set<Number> submittedServicesSet = new LinkedHashSet<>();
    private Logger logger;
    private String queueId;
    private int maxReadyCapacity;
    private boolean noWaitingSpaceAvailable;

    InMemoryJacsServiceQueue() {
        // CDI required ctor
    }

    @Inject
    public InMemoryJacsServiceQueue(JacsServiceDataPersistence jacsServiceDataPersistence,
                                    @PropertyValue(name = "service.queue.id") String queueId,
                                    @PropertyValue(name = "service.queue.MaxCapacity") int maxReadyCapacity,
                                    Logger logger) {
        this.queueId = queueId;
        this.maxReadyCapacity = maxReadyCapacity == 0 ? DEFAULT_MAX_READY_CAPACITY : maxReadyCapacity;
        this.jacsServiceDataPersistence = jacsServiceDataPersistence;
        this.waitingServices = new PriorityBlockingQueue<>(this.maxReadyCapacity, new DefaultServiceInfoComparator());
        this.logger = logger;
        noWaitingSpaceAvailable = false;
    }

    @Override
    public JacsServiceData enqueueService(JacsServiceData jacsServiceData) {
        logger.debug("Enqueued service {}", jacsServiceData);
        if (noWaitingSpaceAvailable) {
            // don't even check if anything has become available since last time
            // just drop it for now - the queue will be refilled after it drains.
            logger.info("In memory queue reached the capacity so service {} will not be put in memory", jacsServiceData);
            return jacsServiceData;
        }
        boolean added = addWaitingService(jacsServiceData);
        noWaitingSpaceAvailable  = !added || (waitingCapacity() <= 0);
        if (noWaitingSpaceAvailable) {
            logger.info("Not enough space in memory queue for {}", jacsServiceData);
        }
        return jacsServiceData;
    }

    @Override
    public JacsServiceData dequeService() {
        JacsServiceData queuedService = getWaitingService();
        if (queuedService == null && enqueueAvailableServices(EnumSet.of(JacsServiceState.CREATED, JacsServiceState.QUEUED))) {
            queuedService = getWaitingService();
        }
        return queuedService;
    }

    @Override
    public void refreshServiceQueue() {
        logger.trace("Sync the waiting queue");
        // check for newly created services and queue them based on their priorities
        enqueueAvailableServices(EnumSet.of(JacsServiceState.CREATED));
    }

    @Override
    public synchronized void abortService(JacsServiceData jacsServiceData) {
        submittedServicesSet.remove(jacsServiceData.getId());
    }

    @Override
    public synchronized void completeService(JacsServiceData jacsServiceData) {
        submittedServicesSet.remove(jacsServiceData.getId());
    }

    @Override
    public synchronized int getReadyServicesSize() {
        return waitingServices.size();
    }

    @Override
    public synchronized int getPendingServicesSize() {
        return submittedServicesSet.size();
    }

    @Override
    public synchronized List<Number> getPendingServices() {
        return ImmutableList.copyOf(submittedServicesSet);
    }

    @Override
    public int getMaxReadyCapacity() {
        return maxReadyCapacity;
    }

    @Override
    public void setMaxReadyCapacity(int maxReadyCapacity) {
        this.maxReadyCapacity = maxReadyCapacity <= 0 ? DEFAULT_MAX_READY_CAPACITY : maxReadyCapacity;
    }

    private synchronized boolean addWaitingService(JacsServiceData jacsServiceData) {
        boolean added = false;
        if (submittedServicesSet.contains(jacsServiceData.getId())
                || waitingServicesSet.contains(jacsServiceData.getId())) {
            // service is already waiting or running
            return true;
        }
        added = waitingServices.offer(jacsServiceData);
        if (added) {
            logger.debug("Enqueued service {} into {}", jacsServiceData, this);
            waitingServicesSet.add(jacsServiceData.getId());
            if (jacsServiceData.getState() == JacsServiceState.CREATED) {
                jacsServiceDataPersistence.updateServiceState(jacsServiceData, JacsServiceState.QUEUED, Optional.<JacsServiceEvent>empty());
            }
        }
        return added;
    }

    private synchronized boolean enqueueAvailableServices(Set<JacsServiceState> jacsServiceStates) {
        int availableSpaces = maxReadyCapacity;
        PageRequest servicePageRequest = new PageRequest();
        servicePageRequest.setPageSize(availableSpaces);
        servicePageRequest.setSortCriteria(new ArrayList<>(ImmutableList.of(
                new SortCriteria("priority", SortDirection.DESC),
                new SortCriteria("creationDate"))));
        PageResult<JacsServiceData> services = jacsServiceDataPersistence.claimServiceByQueueAndState(queueId, jacsServiceStates, servicePageRequest);
        if (CollectionUtils.isNotEmpty(services.getResultList())) {
            services.getResultList().stream().forEach(serviceData -> {
                try {
                    Preconditions.checkArgument(serviceData.getId() != null, "Invalid service ID");
                    if (!submittedServicesSet.contains(serviceData.getId()) &&
                            !waitingServicesSet.contains(serviceData.getId())) {
                        addWaitingService(serviceData);
                    }
                } catch (Exception e) {
                    logger.error("Internal error - no computation can be created for {}", serviceData);
                }
            });
            noWaitingSpaceAvailable = waitingCapacity() <= 0;
            return true;
        }
        return false;
    }

    private synchronized JacsServiceData getWaitingService() {
        JacsServiceData jacsServiceData = waitingServices.poll();
        if (jacsServiceData != null) {
            logger.debug("Retrieved waiting service {}", jacsServiceData);
            Number serviceId = jacsServiceData.getId();
            submittedServicesSet.add(serviceId);
            waitingServicesSet.remove(serviceId);
        }
        return jacsServiceData;
    }

    private int waitingCapacity() {
        int remainingCapacity = maxReadyCapacity - waitingServices.size();
        return remainingCapacity < 0 ? 0 : remainingCapacity;
    }

}
