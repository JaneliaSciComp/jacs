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
public class ActiveDataRegistration {
    Long scannerId;
    ActiveDataScanStatus scanStatus;

    public ActiveDataRegistration(Long scannerId, ActiveDataScanStatus scanStatus) {
        this.scannerId = scannerId;
        this.scanStatus = scanStatus;
    }

    public Long getScannerId() {
        return scannerId;
    }

    public ActiveDataScanStatus getScanStatus() {
        return scanStatus;
    }
    
}
