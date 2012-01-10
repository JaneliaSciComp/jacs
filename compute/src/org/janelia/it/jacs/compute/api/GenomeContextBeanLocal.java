
package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.metadata.Sample;

import javax.ejb.Local;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 12, 2007
 * Time: 11:23:11 AM
 */
@Local
public interface GenomeContextBeanLocal {

    public List<Sample> getSamplesByProject(String projectId) throws Exception;

}
