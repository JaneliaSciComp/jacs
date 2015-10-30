
package org.janelia.it.jacs.server.access.hibernate.utils;

import org.janelia.it.jacs.server.access.TaskDAO;

/**
 * User: aresnick
 * Date: Jul 8, 2009
 * Time: 11:37:07 AM
 * <p/>
 * <p/>
 * Description:
 */
public class BlastJobInfoGeneratorFactory {
    private BlastJobInfoGeneratorFactory() {
    }

    public static BlastJobInfoGenerator getInstance(TaskDAO taskDAO, String classname) throws Exception {
        if ("BlastTask".equals(classname)) {
            return new BlastTaskBlastJobInfoGenerator(taskDAO);
        }
//        else if ("ReversePsiBlastTask".equals(classname)) {
//            return new ReversePsiBlastTaskBlastJobInfoGenerator(taskDAO);
//        }
        else {
            throw new Exception("BlastJobInfoGeneratorFactory cannot construct a BlastJobInfoGenerator " +
                    " from a " + classname + " object");
        }
    }
}
