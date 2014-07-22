/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.activeData;

import java.util.List;

/**
 *
 * @author murphys
 */
public abstract class ActiveDataClient {
    
    protected EntityScanner entityScanner;
    private Long scannerId = 0L;
    
    public void setEntityScanner(EntityScanner entityScanner) {
        this.entityScanner=entityScanner;
    }
    
    /* Registers a scanner with the ActiveData system (ADS). If the ADS does not
    already have an instance of the scanner, it will create a management entry
    and populate its ID list using the scanner classes' generateIdList() method.
    If this is the case, this method might block for a considerable period of 
    time while the list is being generated. On the other hand, if the ADS already
    has an instance, it will return immediately with the status of that scanner group.   
    */
    public abstract ActiveDataRegistration registerScanner() throws Exception;
    
    // Blocks for a lock on the requested String
    public abstract void lock(String lockString) throws Exception;
    
    // Releases lock on the requested String
    public abstract void release(String lockString) throws Exception;
    
    // Gets the next entity ID to process
    public abstract Long getNext() throws Exception;
    
    // Gets the status of the ActiveData system for the given scanner context
    public abstract ActiveDataScanStatus getScanStatus() throws Exception;
    
    // Gets the process status of the entity - valid only for current Epoch
    public abstract int getEntityStatus(long entityId) throws Exception;
    
    // Sets the process status of the entity - valid only for current Epoch
    public abstract void setEntityStatus(long entityId, int statusCode) throws Exception;
    
    // Add process Event for entity - valid for all Epochs
    public abstract void addEntityEvent(long entityId, String eventDescriptor) throws Exception;
    
    // Gets the entire Event history for the given Entity, format <timestamp:descriptor>, valid for all Epochs
    public abstract List<ActiveDataEntityEvent> getEntityEvents(long entityId) throws Exception;
    
    // Clears all Events for the given Entity, across all Epochs
    public abstract void clearEntityEvents(long entityId) throws Exception;
    
    // Places the given entity in the Injection (high-priority) queue
    public abstract void injectEntity(long entityId) throws Exception;
    
    // Gets the timestamps at which each epoch started
    public abstract List<Long> getEpochHistory() throws Exception;
    
    // Advances to the next epoch, and re-generates id list, also resetting all state
    public abstract void advanceEpoch() throws Exception;
    
}
