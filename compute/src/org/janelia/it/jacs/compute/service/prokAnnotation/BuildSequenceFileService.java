
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:40:53 PM
 */
public class BuildSequenceFileService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "build_seq.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -P " + _targetDirectory;
    }

    @Override
    protected String getSGEQueue() {
        return "-l fast";
    }

}
