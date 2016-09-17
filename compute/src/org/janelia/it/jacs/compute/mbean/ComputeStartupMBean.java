
package org.janelia.it.jacs.compute.mbean;

import javax.management.MXBean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2006
 * Time: 11:21:58 AM
 *
 * @version $Id: ComputeStartupMBean.java 1 2011-02-16 21:07:19Z tprindle $
 */
@MXBean
public interface ComputeStartupMBean {

    public void reReadProperties();

    public void start();

    public void stop();

}
