
package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class SearchResultsIconPanelFactory extends SearchIconPanelFactory {
    public SearchResultsIconPanelFactory() {
        super();
    }

    public Object createAllSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getSearchAllIconSmall(), Constants.SEARCH_ALL), Constants.SEARCH_ALL, null);
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getAccessionIconLarge(), Constants.SEARCH_ACCESSION), Constants.SEARCH_ACCESSION, null);
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProteinsIconLarge(), Constants.SEARCH_PROTEINS), Constants.SEARCH_PROTEINS, null);
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProteinClustersIconLarge(), Constants.SEARCH_CLUSTERS), Constants.SEARCH_CLUSTERS, null);
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getPublicationsIconLarge(), Constants.SEARCH_PUBLICATIONS), Constants.SEARCH_PUBLICATIONS, null);
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProjectsIconLarge(), Constants.SEARCH_PROJECTS), Constants.SEARCH_PROJECTS, null);
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getSamplesIconLarge(), Constants.SEARCH_SAMPLES), Constants.SEARCH_SAMPLES, null);
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        return new SearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getWebsiteIconLarge(), Constants.SEARCH_WEBSITE), Constants.SEARCH_WEBSITE, null);
    }
}