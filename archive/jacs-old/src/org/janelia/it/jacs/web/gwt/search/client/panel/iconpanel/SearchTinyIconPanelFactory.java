
package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class SearchTinyIconPanelFactory extends SearchIconPanelFactory {
    public SearchTinyIconPanelFactory() {
        super();
    }

    public Object createAllSearchArtifact(Object[] args) {
        return null; /* TODO */
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        return new SearchTinyIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getAccessionIconTiny(), Constants.SEARCH_ACCESSION), Constants.SEARCH_ACCESSION, null);
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new SearchTinyIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProteinsIconTiny(), Constants.SEARCH_PROTEINS), Constants.SEARCH_PROTEINS, null);
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new SearchTinyIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProteinClustersIconTiny(), Constants.SEARCH_CLUSTERS), Constants.SEARCH_CLUSTERS, null);
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        return new SearchTinyIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getPublicationsIconTiny(), Constants.SEARCH_PUBLICATIONS), Constants.SEARCH_PUBLICATIONS, null);
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        return new SearchTinyIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProjectsIconTiny(), Constants.SEARCH_PROJECTS), Constants.SEARCH_PROJECTS, null);
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        return new SearchTinyIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getSamplesIconTiny(), Constants.SEARCH_SAMPLES), Constants.SEARCH_SAMPLES, null);
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        return new SearchTinyIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getWebsiteIconTiny(), Constants.SEARCH_WEBSITE), Constants.SEARCH_WEBSITE, null);
    }
}