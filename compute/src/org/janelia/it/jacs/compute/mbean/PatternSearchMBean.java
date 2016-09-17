package org.janelia.it.jacs.compute.mbean;

import javax.management.MXBean;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/8/12
 * Time: 12:18 PM
 * To change this template use File | Settings | File Templates.
 */

@MXBean
public interface PatternSearchMBean {

    public void runCompartmentAnnotation3DService(String user, String configurationName, String sampleIdListFilePath,
                                                  String patternChannelString);


}