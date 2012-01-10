
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:34 PM
 */
public class GcContentLoaderService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        return "sgc_GC_content_load.spl -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -I";
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}