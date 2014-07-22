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
public class ActiveDataScannerStats {
    int successfulCount=0;
    int errorCount=0;
    int processingCount=0;

    public int getSuccessfulCount() {
        return successfulCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getProcessingCount() {
        return processingCount;
    }
    
    public void reset() {
        successfulCount=0;
        errorCount=0;
        processingCount=0;
    }
    
    public void success() {
        successfulCount++;
    }
    
    public void error() {
        errorCount++;
    }
    
    public void processingStarted() {
        processingCount++;
    }
    
    public void processingDone() {
        processingCount--;
    }
    
}
