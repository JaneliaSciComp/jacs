/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.util.Date;

/**
 *
 * @author murphys
 */
public class ActiveDataScanStatus {
    private long startTimestamp;
    private long endTimestamp;
    private int epochNumber;
    private int currentEpochNumProcessing;
    private int currentEpochNumSuccessful;
    private int currentEpochNumError;
    private int currentEpochIdCount;
    private String statusDescriptor;

    public ActiveDataScanStatus(
            long startTimestamp,
            long endTimestamp,
            int epochNumber, 
            int currentEpochNumProcessing,
            int currentEpochNumSuccessful,
            int currentEpochNumError,
            int currentEpochIdCount,
            String statusDescriptor) {
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.epochNumber = epochNumber;
        this.currentEpochNumProcessing = currentEpochNumProcessing;
        this.currentEpochNumSuccessful = currentEpochNumSuccessful;
        this.currentEpochNumError = currentEpochNumError;
        this.currentEpochIdCount = currentEpochIdCount;
        this.statusDescriptor = statusDescriptor;
    }
    
    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() { return endTimestamp; }

    public int getCurrentEpochNumProcessing() {
        return currentEpochNumProcessing;
    }

    public int getEpochNumber() {
        return epochNumber;
    }

    public String getStatusDescriptor() {
        return statusDescriptor;
    }

    public int getCurrentEpochNumSuccessful() {
        return currentEpochNumSuccessful;
    }

    public int getCurrentEpochNumError() {
        return currentEpochNumError;
    }
    
    public int getCurrentEpochIdCount() {
        return currentEpochIdCount;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("epoch=").append(epochNumber).append(" start=").append(new Date(startTimestamp)).append(" end=")
                .append(new Date(endTimestamp)).append(" processing=")
                .append(currentEpochNumProcessing).append(" successful=").append(currentEpochNumSuccessful).append(" error=")
                .append(currentEpochNumError).append(" status=").append(statusDescriptor).append(" idcount=").append(currentEpochIdCount);
        return sb.toString();
    }
    
    
}
