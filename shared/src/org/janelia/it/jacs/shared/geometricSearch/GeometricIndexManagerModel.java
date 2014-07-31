/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.geometricSearch;

/**
 *
 * @author murphys
 */
public class GeometricIndexManagerModel implements Comparable {
    
  String scannerSignature;
  Long  startTime;
  Long  endTime;
  int totalIdCount;
  int successfulCount;
  int errorCount;
  int activeScannerCount;

    public String getScannerSignature() {
        return scannerSignature;
    }

    public void setScannerSignature(String scannerSignature) {
        this.scannerSignature = scannerSignature;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
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

    public int getActiveScannerCount() {
        return activeScannerCount;
    }

    public void setActiveScannerCount(int activeScannerCount) {
        this.activeScannerCount = activeScannerCount;
    }
  
    @Override
    public int compareTo(Object o) {
        GeometricIndexManagerModel m2=(GeometricIndexManagerModel)o;
        if (m2.getStartTime()<startTime) {
            return 1;
        } else if (m2.getStartTime()>startTime) {
            return -1;
        } else {
            return m2.getScannerSignature().compareTo(scannerSignature);
        }
    }
    
}
