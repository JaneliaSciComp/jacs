
package org.janelia.it.jacs.compute.mbean;

import javax.management.MXBean;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 14, 2009
 * Time: 11:38:22 AM
 */
@MXBean
public interface AnnotationNodeManagerMBean {
    public void resetNewNcbiGenomes(String ncbiGenomeLoadFile, boolean isTest);
    public void prokDirTest();

}
