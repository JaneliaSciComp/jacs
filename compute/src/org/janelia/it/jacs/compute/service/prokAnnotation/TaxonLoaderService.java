
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:47:43 PM
 */
public class TaxonLoaderService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "Taxon_loader.pl " + _targetDatabase;
    }

}
