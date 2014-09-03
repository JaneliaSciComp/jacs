/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.geometric_search;

import java.io.Serializable;

/**
 *
 * @author murphys
 */
public class GeometricIndexManagerModel implements Comparable<GeometricIndexManagerModel>, Serializable {

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
    public int compareTo(GeometricIndexManagerModel m2) {
        //GeometricIndexManagerModel m2=(GeometricIndexManagerModel)o;
        if (m2.getStartTime()>startTime) {
            return 1;
        } else if (m2.getStartTime()<startTime) {
            return -1;
        } else {
            return m2.getScannerSignature().compareTo(scannerSignature);
        }
    }

    public String getAbbreviatedSignature() {
        StringBuilder sb=new StringBuilder();
        if (scannerSignature==null) {
            return null;
        }
        else {
            String[] cList=scannerSignature.split(":");
            if (cList.length==0) {
                return "<no classes>";
            }
            for (int i=0;i<cList.length;i++) {
                String className=cList[i];
                String[] pList=className.split("\\.");
                if (pList.length==0) {
                    sb.append(":");
                } else {
                    sb.append(pList[pList.length - 1]);
                }
                if (i<cList.length-1) {
                    sb.append(":");
                }
            }
            return sb.toString();
        }
    }

}
