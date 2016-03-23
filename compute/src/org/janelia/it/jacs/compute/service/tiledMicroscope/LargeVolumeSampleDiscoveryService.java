/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.service.tiledMicroscope;

import java.util.Date;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.mbean.LargeVolumeSampleDiscovery;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.model.tasks.Event;

/**
 * This is a service-implementation to hand off to large-volume discovery.
 *
 * @author fosterl
 */
public class LargeVolumeSampleDiscoveryService extends AbstractDomainService  {
    @Override
    public void execute() throws Exception {
        EJBFactory.getLocalComputeBean().updateTaskStatus(processData.getProcessId(), "Executing", "Large Sample Disccovery Begun");
        LargeVolumeSampleDiscovery discoveryObj = new LargeVolumeSampleDiscovery(new LargeVolumeSampleDiscovery.Notifier() {
            @Override
            public void status(boolean success, String reason) {
                try {
                    if (success) {
                        computeBean.saveEvent(task.getObjectId(), Event.COMPLETED_EVENT, reason, new Date());
                    }
                    else {
                        computeBean.saveEvent(task.getObjectId(), Event.ERROR_EVENT, reason, new Date());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        discoveryObj.discoverSamples();
    }
}
