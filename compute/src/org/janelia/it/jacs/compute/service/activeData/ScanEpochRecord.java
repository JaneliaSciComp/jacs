/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

/**
 *
 * @author murphys
 */
public class ScanEpochRecord {
    private long startTimestamp=0L;
    private long endTimestamp=0L;
    private int totalIdCount=0;
    private int successfulCount=0;
    private int errorCount=0;

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public int getTotalIdCount() {
        return totalIdCount;
    }

    public void setTotalIdCount(int totalIdCount) {
        this.totalIdCount = totalIdCount;
    }

    public int getSuccessfulCount() {
        return successfulCount;
    }

    public void setSuccessfulCount(int successfulCount) {
        this.successfulCount = successfulCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }
    
}
