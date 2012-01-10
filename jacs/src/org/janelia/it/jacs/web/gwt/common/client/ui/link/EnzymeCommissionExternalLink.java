
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import org.janelia.it.jacs.web.gwt.common.client.popup.SimpleTooltipPopup;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;

/**
 * @author Michael Press
 */
public class EnzymeCommissionExternalLink extends ExternalLink {
    private static final String BASE_URL = "http://au.expasy.org/enzyme/";

    public EnzymeCommissionExternalLink(String ecId) {
        this(ecId, /*description*/ null);
    }

    public EnzymeCommissionExternalLink(String ecId, String description) {
        super(ecId, BASE_URL + ecId.substring(3));
        setHoverPopup(description);
    }

    public EnzymeCommissionExternalLink(String ecId, String description, PopupLauncher launcher) {
        super(ecId, BASE_URL + ecId.substring(3));
        setHoverPopup(new SimpleTooltipPopup(description), launcher);
    }
}