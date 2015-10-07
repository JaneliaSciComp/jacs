
package org.janelia.it.jacs.web.gwt.download.server;

import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;

import java.util.List;
import java.util.Map;

/**
 * Implement this to provide information about publications and their supporting data.
 * <p/>
 * User: Lfoster
 * Date: Aug 25, 2006
 * Time: 10:09:11 AM
 */
public interface PublicationHelper {
    public static final String DESCRIPTIVE_TEXT = "Description";
    public static final String COMING_SOON = "(Coming Soon)";

    Map<String, Project> getSymbolToProjectMapping();

    Map<String, Publication> getAccessionToPublicationMapping();

    void saveOrUpdateProject(ProjectImpl project);

    Project getProjectByName(String projectName);

    Project getProjectBySymbol(String projectSymbol);

    Publication getPublicationByAccession(String publicationAccession);

    List<String> getNewFiles();

    Boolean checkFileLocation(String fileLocation);

    List<Sample> getProjectSamples(String projectSymbol);

    Map<String, List<Sample>> getProjectSamplesByProject();

    List<DownloadableDataNode> getDownloadableFilesBySampleAcc(String sampleAcc);
}
