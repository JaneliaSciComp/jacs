
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:42:20 PM
 */
public class LinkToNtFeaturesService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "link_to_nt_features.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -I";
    }

}
