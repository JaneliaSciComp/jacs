
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:41:16 PM
 */
public class CogSearchService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "condor_cog_search.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword +
                " -G " + getDefaultProjectCode() + " -I";
    }

}
