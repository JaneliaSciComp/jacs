
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import org.janelia.it.jacs.web.gwt.common.client.popup.SimpleTooltipPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;

/**
 * @author Michael Press
 */
public class GeneOntologyExternalLink extends ExternalLink {
    private static final String BASE_URL = "http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?view=details&search_constraint=terms&query=";

    public GeneOntologyExternalLink(String goId) {
        this(goId, /*description*/ null);
    }

    public GeneOntologyExternalLink(String goId, String description) {
        super(goId, BASE_URL + goId);
        setHoverPopup(description);
    }

    public GeneOntologyExternalLink(String goId, String description, PopupLauncher launcher) {
        super(goId, BASE_URL + goId);
        setHoverPopup(new SimpleTooltipPopup(description), launcher);
    }
}
