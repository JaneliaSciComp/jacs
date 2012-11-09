package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 11/8/12
 * Time: 12:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class PatternSearch implements PatternSearchMBean {

    private static final Logger logger = Logger.getLogger(PatternSearchMBean.class);

    public void changePatternAnnotationFolderName() {
        logger.info("changePatternAnnotationFolderName() start");
    }



}
