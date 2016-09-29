
package org.janelia.it.jacs.compute.mbean;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2006
 * Time: 11:22:34 AM
 *
 * @version $Id: ComputeStartup.java 1 2011-02-16 21:07:19Z tprindle $
 */
@Singleton
@Startup
@Remote(ComputeStartupMBean.class)
public class ComputeStartup extends AbstractComponentMBean implements ComputeStartupMBean {
    private static final Logger logger = Logger.getLogger(ComputeStartup.class);

    public ComputeStartup() {
        super("jacs");
        reReadProperties();
    }

    public void reReadProperties() {
        try {
            SystemConfigurationProperties.load();
        }
        catch (Exception ex) {
            logger.error("Unable to load property file from EAR!!", ex);
        }
    }

    public void start() {

    }

    public void stop() {

    }

}
