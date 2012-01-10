
package org.janelia.it.jacs.web.gwt.download.client;

import java.util.List;

/**
 * @author Michael Press
 */
public class SecondaryDownloadBox extends DownloadBox {
    public SecondaryDownloadBox() {
        this(null);
    }

    public SecondaryDownloadBox(String title) {
        this(title, false);
    }

    public SecondaryDownloadBox(String title, boolean showActionLinks) {
        this(title, null, showActionLinks, true);
    }


    public SecondaryDownloadBox(String title, List files, boolean showActionLinks, boolean showContent) {
        super(title, files, showActionLinks, showContent);
    }

    protected void init() {
        super.init();
        setContentsPanelStyleName("secondaryDownloadBoxContentsPanel");
        setCornerStyleName("secondaryDownloadBoxRounding");
        setLabelStyleName("secondaryDownloadBoxLabel");
        setLabelCornerStyleName("secondaryDownloadBoxLabelRounding");
        setActionLinkBackgroundStyleName("secondaryDownloadBoxActionLinkBackground ");
    }
}
