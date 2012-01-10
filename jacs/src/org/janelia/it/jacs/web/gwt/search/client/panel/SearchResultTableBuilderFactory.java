
package org.janelia.it.jacs.web.gwt.search.client.panel;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.search.client.SearchArtifactFactory;
import org.janelia.it.jacs.web.gwt.search.client.panel.cluster.ClusterSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.project.ProjectSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.protein.ProteinSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.publication.PublicationSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.sample.SampleSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.website.WebsiteSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.website.WebsiteSearchDataBuilderBasedOnGoogle;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 6, 2007
 * Time: 1:44:54 PM
 */
public class SearchResultTableBuilderFactory extends SearchArtifactFactory {

    public SearchResultTableBuilderFactory() {
        super();
    }

    public CategorySearchDataBuilder createResultTableBuilder(String category,
                                                              String searchId,
                                                              String searchQuery) {
        return (CategorySearchDataBuilder) createCategorySearchArtifact(category,
                new Object[]{
                        searchId,
                        searchQuery
                });
    }

    public Object createAllSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result builder for the 'ALL' category");
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result builder for the 'Accessions' category");
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new ProteinSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new ClusterSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        return new PublicationSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        return new ProjectSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        return new SampleSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        if (Constants.USE_GOOGLE_API_FOR_WEBSITE_SEARCH) {
            return new WebsiteSearchDataBuilderBasedOnGoogle((String) args[0], (String) args[1]);
        }
        return new WebsiteSearchDataBuilder((String) args[0], (String) args[1]);
    }

}
