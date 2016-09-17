
package org.janelia.it.jacs.compute.mbean;

import javax.management.MXBean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 15, 2006
 * Time: 1:04:46 PM
 */
@MXBean
public interface AnnotationManagerMBean {

    public void testNeuronSep(String inputFilePath);
    public void testColorSep(String inputFilePath);
    public void generateUserAnnotationReport(String username);
}