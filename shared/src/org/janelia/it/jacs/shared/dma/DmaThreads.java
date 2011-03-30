/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.shared.dma;

import org.janelia.it.jacs.shared.dma.entity.MutableLong;
import org.janelia.it.jacs.shared.dma.reporter.DmaLogger;
import org.janelia.it.jacs.shared.dma.reporter.Progress;
import org.janelia.it.jacs.shared.dma.reporter.ProgressCapturer;
import org.janelia.it.jacs.shared.dma.reporter.ProgressReporter;
import org.janelia.it.jacs.shared.utils.DateUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * This class is used to perform forking and joining of asynchronous operations in DMA.
 *
 * @author Tareq Nabeel
 */
public class DmaThreads implements Runnable, DmaAction {

    private List<DmaAction> liveThreads = new ArrayList<DmaAction>();
    private DmaFiles dmaFiles;
    private int maxThreads;
    private DmaManager dmaManager;
    private long startTimeMillis;
    private String runnableClassName;

    private DmaLogger dmaLogger = DmaLogger.getInstance();

    private MutableLong processedByteCount = new MutableLong();
    private MutableLong processedSeqCount = new MutableLong();
    private MutableLong seqErrorCount = new MutableLong();
    private MutableLong completedByteCount = new MutableLong();
    private MutableLong completedSeqCount = new MutableLong();
    private MutableLong completedSeqErrorCount = new MutableLong();
    private ProgressCapturer progressCapturer;

    private String finalFullReport;
    private String finalTotalReport;
    private String name;
    private DmaArgs dmaArgs;
    private boolean reportETA;
    private boolean reportThreadCounts;
    private DmaAction singleDmaAction;

    private List<Progress> completedDmaActionsProgressList = new ArrayList<Progress>();

    // waitForCompletion would return before the first child thread has kicked off
    // This variable would prevent that
    private boolean dmaThreadsStarted;

    public DmaThreads(String name, DmaFiles dmaFiles, String runnableClassName, DmaManager dmaManager, int maxThreads, boolean reportETA, boolean reportThreadCounts) {
        this.name = name;
        this.maxThreads = maxThreads;
        this.dmaManager = dmaManager;
        if (dmaFiles != null && dmaFiles.originalSize() == 0) {
            throw new IllegalArgumentException("Nothing to process dmaFiles size=0");
        }
        this.dmaFiles = dmaFiles;
        this.dmaArgs = dmaManager.getDmaArgs();
        this.runnableClassName = runnableClassName;
        this.reportETA = reportETA;
        this.reportThreadCounts = reportThreadCounts;
        progressCapturer = new ProgressCapturer(this);
    }

    public DmaThreads(String name, String runnableClassName, DmaManager dmaManager, boolean reportETA, boolean reportThreadCounts) {
        this(name, null, runnableClassName, dmaManager, 1, reportETA, reportThreadCounts);
    }

    /**
     * This method runs assynchronously and ends up calling spinOff
     */
    public void execute() {
        startTimeMillis = System.currentTimeMillis();
        Thread me = new Thread(this);
        me.start();
    }

    /**
     * java.lang.Runnable
     */
    public void run() {
        try {
            spinOff();
        }
        catch (ClassNotFoundException e) {
            handleException(e);
        }
        catch (SQLException e) {
            handleException(e);
        }
        catch (InstantiationException e) {
            handleException(e);
        }
        catch (IllegalAccessException e) {
            handleException(e);
        }
        catch (InterruptedException e) {
            handleException(e);
        }
    }

    /**
     * This method either spins off a thread of DmaAction execution for each of the items in <code>dmaFiles</code>
     * if supplied or spins off a single thread of DmaAction execution.
     *
     * @throws InterruptedException
     * @throws IllegalAccessException
     * @throws SQLException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    private void spinOff() throws InterruptedException, IllegalAccessException, SQLException, InstantiationException, ClassNotFoundException {
        startTimeMillis = System.currentTimeMillis();
        dmaLogger.logInfo("DmaThreads " + this.getName() + " execute()... ", getClass());
        synchronized (this) {
            if (dmaFiles != null) {
                while (dmaFiles.currentSize() > 0) {
                    while (liveThreads.size() == maxThreads) {
                        wait();
                    }
                    DmaFile dmaFile = dmaFiles.removeLast();
                    spinOffDmaThread(dmaFile, false);
                    dmaManager.startStatusLogging();
                }
            }
            else {
                //spins off a single thread of DmaAction execution
                spinOffDmaThread(null, true);
            }
        }
        dmaLogger.logInfo("DmaThreads " + this.getName() + " execute() returning ", getClass());
    }

    /**
     * This method will block until all threads of execution have completed
     */
    public void waitForCompletion() {
        dmaLogger.logInfo("DmaThreads " + this.getName() + " waitForCompletion() .... ", getClass());
        try {
            // We need to prevent this method from returning before a single thread in liveThreads
            // has had a chance to start
            while (!dmaThreadsStarted) {
                Thread.sleep(1000);
            }
            synchronized (this) {
                while (liveThreads.size() > 0) {
                    // block until live thread count reaches 0
                    wait();
                    dmaLogger.logInfo("waitForCompletion() after wait liveThreads.size()=" + liveThreads.size() + " liveThreads=" + liveThreads, getClass());
                }
            }
        }
        catch (InterruptedException e) {
            handleException(e);
        }
        dmaLogger.logInfo("DmaThreads " + this.getName() + " waitForCompletion() returning ", getClass());
    }

    /**
     * This method is responsible for spinning off a single DmaAction thread
     *
     * @param dmaFile
     * @param isSingleDmaAction
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void spinOffDmaThread(DmaFile dmaFile, boolean isSingleDmaAction) throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
//        dmaLogger.logInfo("DmaThreads.spinOffDmaThread() starting " + (dmaFile == null ? "" : "for " + dmaFile.getName()), getClass());
        Object obj = Class.forName(runnableClassName).newInstance();
        if (!(obj instanceof DmaAction)) {
            throw new IllegalArgumentException(runnableClassName + " must be an instance of " + DmaAction.class.getName());
        }
        String threadName;
        DmaAction dmaAction = (DmaAction) obj;
        if (dmaFile != null) {
            dmaAction.setDmaFile(dmaFile);
            threadName = dmaFile.getName();
        }
        else {
            threadName = String.valueOf("Thread " + liveThreads.size() + 1);
        }
        ProgressCapturer progressCapturer = new ProgressCapturer(dmaAction);
        dmaAction.setProgressCapturer(progressCapturer);
        dmaAction.setDmaArgs(dmaArgs);
        Thread worker = new Thread(new DmaActionRunnable(this, dmaAction), threadName);
        liveThreads.add(dmaAction);
        if (isSingleDmaAction) {
            this.singleDmaAction = dmaAction;
        }
        worker.start();
        dmaThreadsStarted = true;
    }

    private void handleException(Exception e) {
        liveThreads.clear();
        throw new RuntimeException("Exception during execution of DmaThreads:" + getName(), e);
    }

    /**
     * This class is responsible for running DmaAction instances asynchronously.  When the execution
     * of an instance of DmaAction completes, it notifies the DmaThreads instance so that it can
     * remove it from its list of liveThreads and add it to completed list
     */
    private class DmaActionRunnable implements Runnable {
        private DmaAction dmaAction;
        private DmaThreads dmaThreads;

        public DmaActionRunnable(DmaThreads dmaThreads, DmaAction dmaAction) {
            this.dmaAction = dmaAction;
            this.dmaThreads = dmaThreads;
        }

        public void run() {
            try {
                dmaAction.execute();
                synchronized (dmaThreads) {
                    boolean removed = liveThreads.remove(dmaAction);
                    dmaLogger.logInfo("Removed  " + dmaAction.getName() + " from liveThreads; removed=" + removed + " liveThreads.size()=" + liveThreads.size(), getClass());
                    addCompletedProgress(dmaAction);
                    if (liveThreads.size() == 0) {
                        dmaManager.logStatus();
                    }
                    dmaThreads.notify();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method returns a full progress report
     *
     * @return
     */
    public synchronized String getFullReport() {
        if (finalFullReport != null) {
            return finalFullReport;
        }
        else {
            StringBuilder fullReportBuff = new StringBuilder();
            fullReportBuff.append("\n");
            fullReportBuff.append(getWaitingDmaActionsReport());
            fullReportBuff.append("\n");
            fullReportBuff.append(getCompletedDmaActionsReport());
            fullReportBuff.append("\n");
            fullReportBuff.append(getLiveDmaActionsReport());
            fullReportBuff.append("\n");
            fullReportBuff.append(getTotalReport());
            String fullReport = fullReportBuff.toString();
            if (liveThreads.size() == 0) {
                finalFullReport = fullReport;
            }
            return fullReport;
        }
    }

    /**
     * This method returns just the summary progress item and serves as an alternative to getFullReport()
     *
     * @return
     */
    public String getTotalReport() {
        synchronized (this) {
            if (finalTotalReport != null) {
                return finalTotalReport;
            }
            else {
                StringBuilder statusBuff = new StringBuilder();
                // Get the progress of DmaThreads instance as a whole and append the line item
                Progress dmaThreadsProgress = getProgress();
                statusBuff.append(ProgressReporter.getReport(dmaThreadsProgress, false, null));

                addThreadCountAndETAToReport(statusBuff, dmaThreadsProgress);

                String totalReport = statusBuff.toString();

                if (liveThreads.size() == 0) {
                    finalTotalReport = totalReport;
                }
                return totalReport;
            }
        }
    }

    private void addThreadCountAndETAToReport(StringBuilder statusBuff, Progress dmaThreadsProgress) {
        if (reportThreadCounts) {
            statusBuff.append("Total threads:").append(getLiveThreadCount() + getWaitingThreadCount() + getCompletedThreadCount());
            statusBuff.append(" Live:").append(getLiveThreadCount());
            statusBuff.append(" Waiting:").append(getWaitingThreadCount());
            statusBuff.append(" Completed:").append(getCompletedThreadCount()).append("\n");
        }

        // ETAs are based on target bytes and avg/actual bytes being processed.  It should only be computed for
        // DmaActions that are capable of computing it's target bytes in advance
        if (reportETA && !isComplete()) {
            if (dmaThreadsProgress.getActualBytesPerSec() > 0)
                statusBuff.append("ETA [actual rate]: ").append(getEstimatedTimeForCompletion(dmaThreadsProgress, dmaThreadsProgress.getActualBytesPerSec())).append("\n");
            if (dmaThreadsProgress.getAvgBytesPerSec() > 0)
                statusBuff.append("ETA [average rate]: ").append(getEstimatedTimeForCompletion(dmaThreadsProgress, dmaThreadsProgress.getAvgBytesPerSec())).append("\n\n");
        }
    }

    private void addProgressItems(List<DmaAction> dmaActions, List<Progress> progressList) {
        synchronized (this) {
            for (DmaAction dmaAction : dmaActions) {
                progressList.add(dmaAction.getProgress());
            }
        }
    }

    private void addTotals(List<DmaAction> dmaActions) {
        synchronized (this) {
            for (DmaAction dmaAction : dmaActions) {
                setTotals(dmaAction);
            }
        }
    }

    private void addCompletedProgress(DmaAction dmaAction) {
        completedDmaActionsProgressList.add(dmaAction.getProgress());
        completedByteCount.add(dmaAction.getProcessedByteCount());
        completedSeqCount.add(dmaAction.getProcessedSeqCount());
        completedSeqErrorCount.add(dmaAction.getSeqErrorCount());
    }

    private void addCompletedTotals() {
        this.processedByteCount.add(completedByteCount);
        this.processedSeqCount.add(completedSeqCount);
        this.seqErrorCount.add(completedSeqErrorCount);
    }

    private void setTotals(DmaAction dmaAction) {
        this.processedByteCount.add(dmaAction.getProcessedByteCount());
        this.processedSeqCount.add(dmaAction.getProcessedSeqCount());
        this.seqErrorCount.add(dmaAction.getSeqErrorCount());
    }

    private String getWaitingDmaActionsReport() {
        List<Progress> waitingDmaActionsProgressList = new ArrayList<Progress>();
        if (dmaFiles != null) {
            for (DmaFile notStartedDmaFile : dmaFiles.getDmaFileListToBeProcessed()) {
                waitingDmaActionsProgressList.add(new Progress(notStartedDmaFile.getName(), 0, notStartedDmaFile.getJavaFileSize(), 0, notStartedDmaFile.getTargetSeqCount(), 0, 0, 0, 0, 0.0f, 0, "", 0, 0.0));
            }
        }
        return ProgressReporter.getReport(waitingDmaActionsProgressList, true, "Waiting " + this.name);
    }

    private String getLiveDmaActionsReport() {
        List<Progress> runningDmaActionsProgressList = new ArrayList<Progress>();
        addProgressItems(getLiveThreads(), runningDmaActionsProgressList);
        return ProgressReporter.getReport(runningDmaActionsProgressList, true, "Live " + this.name);
    }

    private String getCompletedDmaActionsReport() {
        return ProgressReporter.getReport(completedDmaActionsProgressList, true, "Completed " + this.name);
    }

    public long getStartTime() {
        return startTimeMillis;
    }

    public String getName() {
        return name;
    }

    public Progress getProgress() {
        Progress progress = null;
        try {
//            synchronized(this) {
            addCompletedTotals();
            addTotals(getLiveThreads());
            progress = progressCapturer.capture(this.processedByteCount, this.processedSeqCount, this.seqErrorCount);
            this.processedByteCount.reset();
            this.processedSeqCount.reset();
            this.seqErrorCount.reset();
//            }
        }
        catch (Exception e) {
            handleException(e);
        }
        return progress;
    }

    public long getProcessedSeqCount() {
        return processedSeqCount.getValue();
    }

    public long getProcessedByteCount() {
        return processedByteCount.getValue();
    }

    public long getTargetSeqCount() {
        if (dmaFiles != null) {
            return dmaFiles.getTargetSeqCount();
        }
        else {
            return this.singleDmaAction.getTargetSeqCount();
        }
    }

    public long getTargetByteCount() {
        if (dmaFiles != null) {
            return dmaFiles.getTotalFilesSize();
        }
        else {
            return this.singleDmaAction.getTargetByteCount();
        }
    }

    public long getSeqErrorCount() {
        return seqErrorCount.getValue();
    }

    public void setProgressCapturer(ProgressCapturer progressCapturer) {
        this.progressCapturer = progressCapturer;
    }


    public String getLabel() {
        return getName() + " totals";
    }

    public void setDmaFile(DmaFile dmaFile) {
    }

    public DmaFile getDmaFile() {
        return null;
    }

    public void setDmaArgs(DmaArgs dmaArgs) {
        this.dmaArgs = dmaArgs;
    }

    public DmaArgs getDmaArgs() {
        return dmaArgs;
    }


    public List<DmaAction> getLiveThreads() {
        return liveThreads;
    }

    public int getLiveThreadCount() {
        return liveThreads.size();
    }

    public int getWaitingThreadCount() {
        if (dmaFiles != null && dmaFiles.getDmaFileListToBeProcessed() != null)
            return dmaFiles.getDmaFileListToBeProcessed().size();
        else
            return 0;
    }

    public int getCompletedThreadCount() {
        return completedDmaActionsProgressList.size();
    }

    private String getEstimatedTimeForCompletion(Progress progress, double rate) {
        double bytesLeftToProcess = progress.getTotalTargetBytes() - progress.getTotalBytesProcessed();
        long secondsToCompletion = (long) (((float) bytesLeftToProcess) / rate);
        return DateUtil.getElapsedTime(secondsToCompletion * 1000, false);
    }

    private boolean isComplete() {
        return this.dmaThreadsStarted && liveThreads.size() == 0;
    }
}
