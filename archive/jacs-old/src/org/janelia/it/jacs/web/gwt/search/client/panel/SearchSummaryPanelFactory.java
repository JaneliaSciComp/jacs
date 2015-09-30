
package org.janelia.it.jacs.web.gwt.search.client.panel;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.search.client.SearchArtifactFactory;
import org.janelia.it.jacs.web.gwt.search.client.panel.accession.AccessionSummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.cluster.ClusterSummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.project.ProjectSummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.protein.ProteinSummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.publication.PublicationSummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.sample.SampleSummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.website.WebsiteSummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.website.WebsiteSummarySearchPanelBasedOnGoogle;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Sep 6, 2007
 * Time: 1:44:54 PM
 */
public class SearchSummaryPanelFactory extends SearchArtifactFactory {

    public SearchSummaryPanelFactory() {
        super();
    }

    public CategorySummarySearchPanel createSearchSummaryPanel(String category,
                                                               String searchId,
                                                               String searchQuery) {
        return (CategorySummarySearchPanel) createCategorySearchArtifact(category,
                new Object[]{
                        searchId,
                        searchQuery
                });
    }

    public Object createAllSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result builder for the 'ALL' category");
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        return new AccessionSummarySearchPanel((String) args[0], (String) args[1]);
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new ProteinSummarySearchPanel((String) args[0], (String) args[1]);
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new ClusterSummarySearchPanel((String) args[0], (String) args[1]);
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        return new PublicationSummarySearchPanel((String) args[0], (String) args[1]);
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        return new ProjectSummarySearchPanel((String) args[0], (String) args[1]);
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        return new SampleSummarySearchPanel((String) args[0], (String) args[1]);
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        if (Constants.USE_GOOGLE_API_FOR_WEBSITE_SEARCH) {
            return new WebsiteSummarySearchPanelBasedOnGoogle((String) args[0], (String) args[1]);
        }
        return new WebsiteSummarySearchPanel((String) args[0], (String) args[1]);
    }

}
