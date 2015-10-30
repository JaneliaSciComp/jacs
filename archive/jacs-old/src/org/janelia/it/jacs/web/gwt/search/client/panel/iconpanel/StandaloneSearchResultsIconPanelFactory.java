
package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class StandaloneSearchResultsIconPanelFactory extends SearchResultsIconPanelFactory {
    public StandaloneSearchResultsIconPanelFactory() {
        super();
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        return new StandaloneSearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getPublicationsIconLarge(), Constants.SEARCH_PUBLICATIONS), Constants.SEARCH_PUBLICATIONS, null);
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        return new StandaloneSearchResultsIconPanel(createImage(ImageBundleFactory.getCategoryImageBundle().getProjectsIconLarge(), Constants.SEARCH_PROJECTS), Constants.SEARCH_PROJECTS, null);
    }
}
