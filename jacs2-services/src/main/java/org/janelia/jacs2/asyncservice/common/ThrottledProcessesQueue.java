package org.janelia.jacs2.asyncservice.common;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThrottledProcessesQueue {

    private final Logger logger;
    private final ScheduledExecutorService scheduler;
    @ApplicationScoped
    private Map<String, BlockingQueue<ThrottledJobInfo>> waitingProcesses;
    @ApplicationScoped
    private Map<String, BlockingQueue<ThrottledJobInfo>> runningProcesses;
    private final int initialDelayInMillis;
    private final int periodInMillis;

    @Inject
    ThrottledProcessesQueue(Logger logger) {
        this.logger = logger;
        this.initialDelayInMillis = 30000;
        this.periodInMillis = 500;
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("JACS-THROTTLE-%d")
                .setDaemon(true)
                .build();
        this.scheduler = Executors.newScheduledThreadPool(1, threadFactory);
    }

    @PostConstruct
    public void initialize() {
        waitingProcesses = new ConcurrentHashMap<>();;
        runningProcesses= new ConcurrentHashMap<>();;
        scheduler.scheduleAtFixedRate(() -> checkWaitingQueue(), initialDelayInMillis, periodInMillis, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
    }

    /**
     * This method adds another job to the queue either as a running job or as a waiting job if there are not enough resources.
     * Since this can be called by multiple threads the access to it needs to be synchronized
     * @param jobInfo being added
     * @return
     */
    synchronized ThrottledJobInfo add(ThrottledJobInfo jobInfo) {
        if (jobInfo.getMaxRunningProcesses() <= 0 ||
                CollectionUtils.size(runningProcesses.get(jobInfo.getProcessName())) < jobInfo.getMaxRunningProcesses()) {
            addJobDoneCallback(jobInfo);
            if (jobInfo.runProcess() && jobInfo.getMaxRunningProcesses() > 0) {
                BlockingQueue<ThrottledJobInfo> queue = getQueue(jobInfo.getProcessName(), runningProcesses);
                queue.add(jobInfo);
            }
        } else {
            BlockingQueue<ThrottledJobInfo> queue = getQueue(jobInfo.getProcessName(), waitingProcesses);
            queue.add(jobInfo);
        }
        return jobInfo;
    }

    private BlockingQueue<ThrottledJobInfo> getQueue(String name, Map<String, BlockingQueue<ThrottledJobInfo>> whichProcesses) {
        synchronized (whichProcesses) {
            BlockingQueue<ThrottledJobInfo> queue = whichProcesses.get(name);
            if (queue == null) {
                queue = new LinkedBlockingQueue<>();
                whichProcesses.put(name, queue);
            }
            return queue;
        }
    }

    private void moveProcessToRunningQueue(ThrottledJobInfo jobInfo) {
        logger.debug("Prepare for actually running queue {} - {}", jobInfo.getProcessName(), jobInfo.getServiceContext());
        BlockingQueue<ThrottledJobInfo> waitingQueue = getQueue(jobInfo.getProcessName(), waitingProcesses);
        BlockingQueue<ThrottledJobInfo> runningQueue = getQueue(jobInfo.getProcessName(), runningProcesses);
        waitingQueue.remove(jobInfo);
        if (jobInfo.runProcess()) {
            runningQueue.add(jobInfo);
        }
    }

    private void removeProcessFromRunningQueue(ThrottledJobInfo jobInfo) {
        BlockingQueue<ThrottledJobInfo> runningQueue = getQueue(jobInfo.getProcessName(), runningProcesses);
        boolean removed = runningQueue.remove(jobInfo);
        logger.debug("Completed {}:{} and removed it ({}) from the runningProcesses: ", jobInfo.getProcessName(), jobInfo.getScriptName(), removed, runningQueue.size());
    }

    private void checkWaitingQueue() {
        for (Map.Entry<String, BlockingQueue<ThrottledJobInfo>> queueEntry : waitingProcesses.entrySet()) {
            BlockingQueue<ThrottledJobInfo> queue = queueEntry.getValue();
            for (ThrottledJobInfo jobInfo = queue.peek(); jobInfo != null; jobInfo = queue.peek()) {
                if (CollectionUtils.size(runningProcesses.get(jobInfo.getProcessName())) < jobInfo.getMaxRunningProcesses()) {
                    addJobDoneCallback(jobInfo);
                    moveProcessToRunningQueue(jobInfo);
                    jobInfo.runProcess();
                } else {
                    break;
                }
            }
        }
    }

    private void addJobDoneCallback(ThrottledJobInfo jobInfo) {
        jobInfo.setJobDoneCallback(ji -> removeProcessFromRunningQueue(ji));
    }
}
