
package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.search.client.SearchArtifactFactory;

/**
 * @author Michael Press
 */
public class SearchIconPanelFactory extends SearchArtifactFactory {
    public SearchIconPanelFactory() {
        super();
    }

    public SearchIconPanel createSearchIconPanel(String category) {
        return (SearchIconPanel) createCategorySearchArtifact(category, null);
    }

    public SearchIconPanel createSearchIconPanel(String category, boolean largeImage) {
        return (SearchIconPanel) createCategorySearchArtifact(category, null);
    }

    public Object createAllSearchArtifact(Object[] args) {
        return new SearchIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getSearchAllIconSmall(), Constants.SEARCH_ALL), Constants.SEARCH_ALL, null);
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        return new SearchIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getAccessionIconSmall(), Constants.SEARCH_ACCESSION), Constants.SEARCH_ACCESSION, null);
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new SearchIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProteinsIconSmall(), Constants.SEARCH_PROTEINS), Constants.SEARCH_PROTEINS, null);
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new SearchIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProteinClustersIconSmall(), Constants.SEARCH_CLUSTERS), Constants.SEARCH_CLUSTERS, null);
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        return new SearchIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getPublicationsIconSmall(), Constants.SEARCH_PUBLICATIONS), Constants.SEARCH_PUBLICATIONS, null);
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        return new SearchIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProjectsIconSmall(), Constants.SEARCH_PROJECTS), Constants.SEARCH_PROJECTS, null);
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        return new SearchIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getSamplesIconSmall(), Constants.SEARCH_SAMPLES), Constants.SEARCH_SAMPLES, null);
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        return new SearchIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getWebsiteIconSmall(), Constants.SEARCH_WEBSITE), Constants.SEARCH_WEBSITE, null);
    }

    public Image createImage(AbstractImagePrototype imagePrototype, String title) {
        Image image = imagePrototype.createImage();
        image.setTitle(title);
        return image;
    }
}