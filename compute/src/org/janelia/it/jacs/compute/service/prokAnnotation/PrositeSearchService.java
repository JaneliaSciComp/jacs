
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:45:13 PM
 */
public class PrositeSearchService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "prosite_search.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword +
                " -G " + getDefaultProjectCode() + " -I -V";
    }

}
