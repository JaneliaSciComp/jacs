package org.janelia.it.jacs.compute.launcher.fly;

import org.janelia.it.jacs.compute.engine.launcher.ejb.SeriesLauncherMDB;



import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 11/21/11
 * Time: 10:47 PM
 * To change this template use File | Settings | File Templates.
 */

@MessageDriven(activationConfig = {
        //@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/maskSampleAnnotationPipelineLauncher"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "2")//,
        //@ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000")//,
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 2, timeout = 10000)

public class MaskSampleAnnotationPipelineLauncherMDB extends SeriesLauncherMDB {
}

