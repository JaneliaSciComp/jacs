
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:39:13 PM
 */
public class AutoGeneCurationService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "auto_gene_curation.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -x -I";
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}
