
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:34 PM
 */
public class LocusLoaderService extends ProkAnnotationBaseService {

    public String getCommandLine() throws ServiceException {
        // -b is a toggle pickup mode. Only assign locus_tags to those gene features in ident that don't already have one.
        return "locus_loader.pl -b -db " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword;
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}