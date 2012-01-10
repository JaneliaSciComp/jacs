
package org.janelia.it.jacs.web.gwt.search.server.formatter;

import org.janelia.it.jacs.web.gwt.search.client.SearchArtifactFactory;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 7, 2007
 * Time: 5:04:54 PM
 */
public class SearchResultsCSVFormatterFactory extends SearchArtifactFactory {


    public SearchResultsCSVFormatterFactory() {
        super();
    }

    public SearchResultCSVFormatter createSearchResultCSVFormatter(String category) {
        return (SearchResultCSVFormatter) createCategorySearchArtifact(category,
                new Object[]{
                });
    }

    public Object createAllSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result formatter for the 'ALL' category");
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result formatter for the 'Accessions' category");
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new ProteinSearchResultCSVFormatter();
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new ClusterSearchResultCSVFormatter();
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        throw new UnsupportedOperationException("Publication result CSV Formatter is not implemented");
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        throw new UnsupportedOperationException("Project result CSV Formatter is not implemented");
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        throw new UnsupportedOperationException("Sample result CSV Formatter is not implemented");
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        throw new UnsupportedOperationException("Web result CSV Formatter is not implemented");
    }

}
