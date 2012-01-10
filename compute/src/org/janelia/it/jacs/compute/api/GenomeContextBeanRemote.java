
package org.janelia.it.jacs.compute.api;

import org.janelia.it.jacs.model.metadata.Sample;

import javax.ejb.Remote;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 12, 2007
 * Time: 11:23:25 AM
 */
@Remote
public interface GenomeContextBeanRemote {

    public List<Sample> getSamplesByProject(String projectId) throws Exception;

}
